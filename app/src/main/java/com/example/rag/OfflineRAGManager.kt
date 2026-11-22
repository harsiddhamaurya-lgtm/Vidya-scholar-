package com.example.rag

import android.content.Context
import com.example.utils.NativeCoreBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Coordinates Cosmium's Scoped Offline Retrieval-Augmented Generation (RAG) pipeline:
 *
 * 1. **Strict Standard/Stream Isolation & Lazy Eviction**: Only the student's active
 *    `(standardId, streamId)` syllabus pack is persisted in Encrypted Room (`RagRoomDatabase`)
 *    and Memory-Mapped BM25 (`MemoryMappedKeywordIndex`). Other standards/streams are pruned
 *    automatically on onboarding or scope switch to save storage and prevent cross-stream noise.
 * 2. **Unified Vector Space**: Encodes both chapters and queries via the active encoder
 *    (`minilm_onnx_v1` or `native_cpp_v4`) and auto-reindexes whenever catalog version or active
 *    encoder changes.
 * 3. **Cluster + Logistic Specificity Gating**: Fuses Dense cosine similarity, Room FTS4, MMap
 *    BM25, and C++ logistic specificity calibration with strict minimum-confidence and single-subject
 *    purity filtering so unrelated formulas never leak into responses.
 */
class OfflineRAGManager(context: Context) {

    companion object {
        private const val VECTOR_DIM = EncryptedRagDatabase.VECTOR_DIM // 384
        private const val DENSE_WEIGHT = 0.35f
        private const val FTS_WEIGHT = 0.15f
        private const val BM25_MMAP_WEIGHT = 0.15f
        private const val SPECIFICITY_WEIGHT = 0.35f
        private const val MIN_PRIMARY_SCORE = 0.28f
        private const val META_PREFS = "cosmium_rag_catalog_meta"

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float =
            NativeCoreBridge.cosineSimilarity(a, b)

        /**
         * Converts arbitrary student queries (including formulas or punctuation) into a safe
         * SQLite FTS4 `MATCH` prefix-OR query string (e.g. `kirchhoff* OR wheatstone*`).
         */
        fun buildSafeFtsMatchQuery(rawQuery: String): String =
            NativeCoreBridge.buildSafeFtsMatchQuery(rawQuery)
    }

    private val appContext = context.applicationContext
    private val catalogMetaPrefs = appContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
    val roomDb: RagRoomDatabase = RagRoomDatabase.getInstance(appContext)
    private val chapterDao: RagChapterDao = roomDb.ragChapterDao()
    val keywordIndex = MemoryMappedKeywordIndex(File(appContext.filesDir, "cosmium_rag"))
    val modelManager = OnnxDualModelManager(appContext)
    val downloader = OfflineModelVaultDownloader(appContext, modelManager)

    private val _packStatus = MutableStateFlow(
        StandardPackStatus(
            standardId = "class_11",
            streamId = "science_pcm",
            title = "Class 11 • Science (PCM)",
            subtitle = "Physics, Chemistry, Mathematics & Computer Science"
        )
    )
    val packStatus: StateFlow<StandardPackStatus> = _packStatus.asStateFlow()

    private fun scopeMetaKey(standardId: String, streamId: String): String =
        "meta_${standardId}_${streamId}"

    private fun currentExpectedSignature(): String =
        "${SyllabusPackCatalog.CATALOG_VERSION}:${modelManager.activeEncoderSignature()}"

    fun observeChaptersForScope(standardId: String, streamId: String): Flow<List<RagChapterEntity>> {
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        return chapterDao.observeChaptersForScope(standardId, effectiveStream)
    }

    suspend fun refreshPackStatus(standardId: String, streamId: String): StandardPackStatus =
        withContext(Dispatchers.IO) {
            val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
            val (title, subtitle) = SyllabusPackCatalog.getPackMetadata(standardId, effectiveStream)
            val count = chapterDao.getCountForScope(standardId, effectiveStream)
            val idxBytes = keywordIndex.getIndexSizeBytes(standardId, effectiveStream)
            val status = StandardPackStatus(
                standardId = standardId,
                streamId = effectiveStream,
                title = title,
                subtitle = subtitle,
                chunkCount = count,
                mmapIndexSizeBytes = idxBytes,
                isInstalled = count > 0 && idxBytes > 0L,
                isInstalling = false,
                installProgress = if (count > 0) 1f else 0f,
                sqlCipherActive = RagRoomDatabase.isSqlCipherActive
            )
            _packStatus.value = status
            status
        }

    /**
     * Generates 384-dim weighted document embeddings in the active encoder vector space,
     * writes them into Encrypted Room (+ FTS4), builds the companion MMap BM25 index, and
     * optionally evicts all other standards/streams from device storage.
     */
    suspend fun indexDownloadedChapters(
        standardId: String,
        streamId: String,
        chapters: List<TextbookChunk>,
        pruneOtherScopes: Boolean = true
    ): StandardPackStatus = withContext(Dispatchers.IO) {
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        val (title, subtitle) = SyllabusPackCatalog.getPackMetadata(standardId, effectiveStream)

        _packStatus.value = StandardPackStatus(
            standardId = standardId,
            streamId = effectiveStream,
            title = title,
            subtitle = subtitle,
            chunkCount = 0,
            mmapIndexSizeBytes = 0L,
            isInstalled = false,
            isInstalling = true,
            installProgress = 0.1f,
            sqlCipherActive = RagRoomDatabase.isSqlCipherActive
        )

        if (pruneOtherScopes) {
            pruneOtherScopesInternal(standardId, effectiveStream)
        }

        val entities = ArrayList<RagChapterEntity>(chapters.size)
        chapters.forEachIndexed { idx, chunk ->
            val vector = modelManager.encodeWeightedDocument(
                title = chunk.topicTitle,
                subject = chunk.subjectName,
                formulaTags = chunk.formulaTags,
                body = chunk.contentText,
                workedExample = chunk.workedExample
            )
            val normalizedVec = if (vector.size == VECTOR_DIM) vector else FloatArray(VECTOR_DIM)
            entities += RagChapterEntity(
                chunkId = chunk.chunkId,
                standardId = chunk.standardId,
                streamId = effectiveStream,
                subjectId = chunk.subjectId,
                subjectName = chunk.subjectName,
                chapterNumber = chunk.chapterNumber,
                levelId = chunk.levelId,
                topicTitle = chunk.topicTitle,
                contentText = chunk.contentText,
                formulaTags = chunk.formulaTags,
                workedExample = chunk.workedExample,
                embeddingBlob = EncryptedRagDatabase.encodeFloatArray(normalizedVec)
            )
            val prog = 0.15f + 0.65f * ((idx + 1).toFloat() / chapters.size.coerceAtLeast(1).toFloat())
            _packStatus.value = _packStatus.value.copy(installProgress = prog)
        }

        chapterDao.deleteScope(standardId, effectiveStream)
        chapterDao.upsertChapters(entities)
        _packStatus.value = _packStatus.value.copy(installProgress = 0.88f)

        keywordIndex.buildAndPersist(standardId, effectiveStream, chapters)
        catalogMetaPrefs.edit()
            .putString(scopeMetaKey(standardId, effectiveStream), currentExpectedSignature())
            .apply()

        refreshPackStatus(standardId, effectiveStream)
    }

    suspend fun installOrReindexPack(standardId: String, streamId: String): StandardPackStatus {
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        val chunks = SyllabusPackCatalog.getChunksForScope(standardId, effectiveStream)
        return indexDownloadedChapters(standardId, effectiveStream, chunks, pruneOtherScopes = true)
    }

    /**
     * Switches device storage exclusively to `(standardId, streamId)`: evicts all other standards
     * and streams immediately and lazily installs the new scope if missing or stale.
     */
    suspend fun switchActiveScope(standardId: String, streamId: String): StandardPackStatus =
        withContext(Dispatchers.IO) {
            val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
            pruneOtherScopesInternal(standardId, effectiveStream)
            ensureActiveScopeReady(standardId, effectiveStream)
            refreshPackStatus(standardId, effectiveStream)
        }

    suspend fun pruneOtherScopesExcept(standardId: String, streamId: String) =
        withContext(Dispatchers.IO) {
            val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
            pruneOtherScopesInternal(standardId, effectiveStream)
        }

    private suspend fun pruneOtherScopesInternal(standardId: String, effectiveStream: String) {
        chapterDao.deleteOtherScopes(standardId, effectiveStream)
        keywordIndex.pruneOtherScopes(standardId, effectiveStream)
        val keepKey = scopeMetaKey(standardId, effectiveStream)
        val editor = catalogMetaPrefs.edit()
        catalogMetaPrefs.all.keys
            .filter { it.startsWith("meta_") && it != keepKey }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    private suspend fun ensureActiveScopeReady(standardId: String, effectiveStream: String) {
        val count = chapterDao.getCountForScope(standardId, effectiveStream)
        val idxBytes = keywordIndex.getIndexSizeBytes(standardId, effectiveStream)
        val savedSig = catalogMetaPrefs.getString(scopeMetaKey(standardId, effectiveStream), null)
        if (count == 0 || idxBytes == 0L || savedSig != currentExpectedSignature()) {
            installOrReindexPack(standardId, effectiveStream)
        }
    }

    suspend fun clearPackForScope(standardId: String, streamId: String): StandardPackStatus =
        withContext(Dispatchers.IO) {
            val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
            chapterDao.deleteScope(standardId, effectiveStream)
            keywordIndex.deleteScope(standardId, effectiveStream)
            catalogMetaPrefs.edit().remove(scopeMetaKey(standardId, effectiveStream)).apply()
            refreshPackStatus(standardId, effectiveStream)
        }

    /**
     * Hybrid retrieval strictly isolated to `(standardId, streamId)` fusing:
     * 1. Active encoder dense cosine similarity (`0.35`)
     * 2. Room FTS4 prefix match rank (`0.15`)
     * 3. Memory-Mapped BM25 score (`0.15`)
     * 4. C++ logistic-calibrated cluster & conflict specificity (`0.35`)
     *
     * Enforces minimum relevance and same-subject secondary gating so off-topic or cross-chapter
     * equations never pollute answers.
     */
    suspend fun retrieveRelevantChapters(
        query: String,
        standardId: String,
        streamId: String,
        topK: Int = 2
    ): List<RetrievedChunk> = withContext(Dispatchers.IO) {
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        val scopedChapters = chapterDao.getChaptersForScope(standardId, effectiveStream)
        if (scopedChapters.isEmpty()) return@withContext emptyList()

        val queryVec = modelManager.encodeText(query)
        val ftsQuery = buildSafeFtsMatchQuery(query)
        val ftsHits = if (ftsQuery.isNotBlank()) {
            runCatching {
                chapterDao.searchFtsInScope(
                    ftsQuery = ftsQuery,
                    standardId = standardId,
                    streamId = effectiveStream,
                    limit = 12
                )
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val ftsRankByChunkId = ftsHits.mapIndexed { idx, entity ->
            val rankScore = (1f - (idx.toFloat() / ftsHits.size.coerceAtLeast(1).toFloat()) * 0.5f)
                .coerceIn(0.4f, 1f)
            entity.chunkId to rankScore
        }.toMap()

        val bm25Hits = keywordIndex.search(standardId, effectiveStream, query)

        val scored = scopedChapters.map { entity ->
            val storedVec = EncryptedRagDatabase.decodeFloatArray(entity.embeddingBlob)
            val dense = cosineSimilarity(queryVec, storedVec)
            val ftsScore = ftsRankByChunkId[entity.chunkId] ?: 0f
            val bm25Pair = bm25Hits[entity.chunkId]
            val bm25 = bm25Pair?.first ?: 0f
            val keywords = bm25Pair?.second ?: emptyList()

            val specificity = NativeCoreBridge.computeTopicSpecificityScore(
                query = query,
                topicTitle = entity.topicTitle,
                subjectName = entity.subjectName,
                formulaTags = entity.formulaTags,
                contentText = entity.contentText,
                denseSim = dense,
                ftsScore = ftsScore,
                bm25Score = bm25
            )
            val fused = (
                (DENSE_WEIGHT * dense) +
                    (FTS_WEIGHT * ftsScore) +
                    (BM25_MMAP_WEIGHT * bm25) +
                    (SPECIFICITY_WEIGHT * specificity)
                ).coerceIn(0f, 1f)

            RetrievedChunk(
                chunk = entity.toTextbookChunk(),
                denseScore = dense,
                bm25Score = maxOf(bm25, ftsScore),
                fusedScore = fused,
                matchedKeywords = keywords
            )
        }.sortedByDescending { it.fusedScore }

        val top = scored.firstOrNull() ?: return@withContext emptyList()
        val passesPrimaryGate = top.fusedScore >= MIN_PRIMARY_SCORE ||
            (top.bm25Score >= 0.70f && top.denseScore >= 0.22f)
        if (!passesPrimaryGate) return@withContext emptyList()

        if (topK <= 1) return@withContext listOf(top)

        val secondaryThreshold = maxOf(0.50f, top.fusedScore * 0.82f)
        val filtered = mutableListOf(top)
        for (candidate in scored.drop(1)) {
            if (filtered.size >= topK) break
            if (candidate.chunk.subjectId == top.chunk.subjectId &&
                candidate.fusedScore >= secondaryThreshold
            ) {
                filtered += candidate
            }
        }
        filtered
    }

    suspend fun answerOffline(
        query: String,
        standardId: String,
        streamId: String
    ): RagAnswerResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        ensureActiveScopeReady(standardId, effectiveStream)

        val retrieved = retrieveRelevantChapters(query, standardId, effectiveStream, topK = 1)
        val (standardTitle, _) = SyllabusPackCatalog.getPackMetadata(standardId, effectiveStream)
        val (answer, mode) = modelManager.synthesizeGroundedAnswer(
            query = query,
            retrieved = retrieved,
            standardId = standardId,
            streamId = effectiveStream,
            standardTitle = standardTitle
        )
        val elapsed = System.currentTimeMillis() - startMs

        RagAnswerResult(
            answerText = answer,
            brainMode = mode,
            retrievedChunks = retrieved,
            latencyMs = elapsed
        )
    }

    suspend fun unloadModelsFromRam() {
        modelManager.unloadAllModelsFromRam()
    }

    fun close() {
        runCatching { roomDb.close() }
    }
}
