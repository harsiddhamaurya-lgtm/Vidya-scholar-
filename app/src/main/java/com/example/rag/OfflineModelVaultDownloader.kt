package com.example.rag

import android.content.Context
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * On-demand downloader for pre-trained ONNX models into app-internal `filesDir/cosmium_models`.
 * Complies with Google Play storage policy (zero broad storage permissions required).
 */
class OfflineModelVaultDownloader(
    private val context: Context,
    private val modelManager: OnnxDualModelManager
) {
    companion object {
        private const val TAG = "OfflineVaultDownloader"
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val activeJobs = ConcurrentHashMap<OfflineModelSpec, Job>()

    private val _miniLmStatus = MutableStateFlow(initialStatus(OfflineModelSpec.MINILM_ENCODER))
    val miniLmStatus: StateFlow<ModelDownloadStatus> = _miniLmStatus.asStateFlow()

    private val _qwenStatus = MutableStateFlow(initialStatus(OfflineModelSpec.QWEN_2_5_3B_INT4))
    val qwenStatus: StateFlow<ModelDownloadStatus> = _qwenStatus.asStateFlow()

    private fun initialStatus(spec: OfflineModelSpec): ModelDownloadStatus {
        val ready = modelManager.isModelDownloaded(spec)
        return ModelDownloadStatus(
            spec = spec,
            state = if (ready) DownloadTaskState.READY else DownloadTaskState.NOT_DOWNLOADED,
            progress = if (ready) 1f else 0f
        )
    }

    fun getAvailableStorageMb(): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            (stat.availableBytes / (1024L * 1024L))
        } catch (_: Exception) {
            4096L
        }
    }

    fun refreshStatuses() {
        updateFlow(OfflineModelSpec.MINILM_ENCODER, initialStatus(OfflineModelSpec.MINILM_ENCODER))
        updateFlow(OfflineModelSpec.QWEN_2_5_3B_INT4, initialStatus(OfflineModelSpec.QWEN_2_5_3B_INT4))
    }

    suspend fun downloadModel(spec: OfflineModelSpec): Boolean = withContext(Dispatchers.IO) {
        val job = coroutineContext[Job]
        if (job != null) activeJobs[spec] = job

        val availMb = getAvailableStorageMb()
        val requiredMb = (spec.approxBytes / (1024L * 1024L)) + 150L
        if (availMb < requiredMb) {
            updateFlow(
                spec,
                ModelDownloadStatus(
                    spec = spec,
                    state = DownloadTaskState.FAILED,
                    errorMessage = "Insufficient free storage (${availMb} MB available, need ~${requiredMb} MB)."
                )
            )
            return@withContext false
        }

        val dir = modelManager.modelDirectory(spec)
        val modelTmp = File(dir, "${spec.primaryFileName}.part")
        val modelTarget = File(dir, spec.primaryFileName)
        val tokenizerTarget = File(dir, spec.tokenizerFileName)

        try {
            updateFlow(
                spec,
                ModelDownloadStatus(spec = spec, state = DownloadTaskState.DOWNLOADING, progress = 0.02f)
            )

            // 1. Download tokenizer.json first (tiny metadata file)
            if (!tokenizerTarget.exists() || tokenizerTarget.length() < 128L) {
                downloadSingleFile(
                    url = spec.tokenizerDownloadUrl,
                    dest = tokenizerTarget,
                    supportResume = false
                ) { _, _ -> }
            }

            // 2. Stream primary ONNX weights with resume support
            downloadSingleFile(
                url = spec.primaryDownloadUrl,
                dest = modelTmp,
                supportResume = true
            ) { downloaded, total ->
                val denom = if (total > 0L) total else spec.approxBytes
                val frac = (downloaded.toFloat() / denom.toFloat()).coerceIn(0.03f, 0.96f)
                updateFlow(
                    spec,
                    ModelDownloadStatus(
                        spec = spec,
                        state = DownloadTaskState.DOWNLOADING,
                        progress = frac,
                        downloadedBytes = downloaded,
                        totalBytes = denom
                    )
                )
            }

            updateFlow(
                spec,
                ModelDownloadStatus(
                    spec = spec,
                    state = DownloadTaskState.VERIFYING,
                    progress = 0.98f,
                    downloadedBytes = modelTmp.length(),
                    totalBytes = modelTmp.length()
                )
            )

            if (!modelTmp.exists() || modelTmp.length() < 1024L) {
                throw IllegalStateException("Downloaded ONNX artifact failed integrity size check.")
            }

            if (modelTarget.exists()) modelTarget.delete()
            if (!modelTmp.renameTo(modelTarget)) {
                modelTmp.copyTo(modelTarget, overwrite = true)
                modelTmp.delete()
            }

            updateFlow(
                spec,
                ModelDownloadStatus(
                    spec = spec,
                    state = DownloadTaskState.READY,
                    progress = 1f,
                    downloadedBytes = modelTarget.length(),
                    totalBytes = modelTarget.length()
                )
            )
            true
        } catch (ce: CancellationException) {
            updateFlow(
                spec,
                ModelDownloadStatus(
                    spec = spec,
                    state = DownloadTaskState.NOT_DOWNLOADED,
                    progress = 0f,
                    errorMessage = "Download paused/cancelled"
                )
            )
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading ${spec.displayName}: ${e.message}", e)
            updateFlow(
                spec,
                ModelDownloadStatus(
                    spec = spec,
                    state = DownloadTaskState.FAILED,
                    progress = 0f,
                    errorMessage = e.message ?: "Network download failed"
                )
            )
            false
        } finally {
            activeJobs.remove(spec)
        }
    }

    fun cancelDownload(spec: OfflineModelSpec) {
        activeJobs.remove(spec)?.cancel()
    }

    suspend fun deleteModel(spec: OfflineModelSpec) = withContext(Dispatchers.IO) {
        cancelDownload(spec)
        modelManager.unloadAllModelsFromRam()
        val dir = modelManager.modelDirectory(spec)
        runCatching { dir.deleteRecursively() }
        updateFlow(spec, ModelDownloadStatus(spec = spec, state = DownloadTaskState.NOT_DOWNLOADED, progress = 0f))
    }

    private suspend fun downloadSingleFile(
        url: String,
        dest: File,
        supportResume: Boolean,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val existingBytes = if (supportResume && dest.exists()) dest.length() else 0L
        val reqBuilder = Request.Builder().url(url)
        if (existingBytes > 0L) {
            reqBuilder.header("Range", "bytes=$existingBytes-")
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IllegalStateException("HTTP ${response.code} while downloading model artifact")
            }
            val appendMode = response.code == 206 && existingBytes > 0L
            val body = response.body ?: throw IllegalStateException("Empty HTTP response body")
            val contentLen = body.contentLength()
            val totalExpected = if (appendMode && contentLen > 0L) existingBytes + contentLen else contentLen

            var written = if (appendMode) existingBytes else 0L
            val buffer = ByteArray(64 * 1024)
            body.byteStream().use { input ->
                FileOutputStream(dest, appendMode).use { output ->
                    while (isActive) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        onProgress(written, totalExpected)
                    }
                    output.flush()
                }
            }
        }
    }

    private fun updateFlow(spec: OfflineModelSpec, status: ModelDownloadStatus) {
        when (spec) {
            OfflineModelSpec.MINILM_ENCODER -> _miniLmStatus.value = status
            OfflineModelSpec.QWEN_2_5_3B_INT4 -> _qwenStatus.value = status
        }
    }
}
