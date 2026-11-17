package com.example.rag

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Coordinates Cosmium's Offline Retrieval-Augmented Generation (RAG) pipeline:
 *
 * 1. **all-MiniLM-L6-v2 ONNX Embedding Encoder**: Generates normalized 384-dim semantic vectors
 *    for downloaded textbook chapters and incoming student queries.
 * 2. **Encrypted Room Database + FTS4 Full-Text Search**: Stores chapter payloads and `float32`
 *    vector blobs inside a SQLCipher-encrypted Room DB (`RagRoomDatabase`) paired with a
 *    synchronized SQLite FTS4 virtual table (`rag_chapters_fts`) for fast keyword indexing.
 * 3. **Memory-Mapped Inverted Keyword Index (`*.idx.bin`)**: Augments Room FTS4 with BM25 scoring
 *    via `MappedByteBuffer`.
 * 4. **Medium Language Model (MLM) 4-Bit Brain (`Qwen 2.5 3B INT4`)**: Synthesizes step-by-step
 *    grounded answers over top retrieved textbook chapters (with low-RAM / pre-download fallback).
 */
class OfflineRAGManager(context: Context) {

    companion object {
        private const val VECTOR_DIM = EncryptedRagDatabase.VECTOR_DIM // 384
        private const val DENSE_WEIGHT = 0.55f
        private const val FTS_WEIGHT = 0.25f
        private const val BM25_MMAP_WEIGHT = 0.20f
        private const val MIN_RELEVANCE_THRESHOLD = 0.08f

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float =
            com.example.utils.NativeCoreBridge.cosineSimilarity(a, b)

        /**
         * Converts arbitrary student queries (including formulas or punctuation) into a safe
         * SQLite FTS4 `MATCH` prefix-OR query string (e.g. `kirchhoff* OR wheatstone*`).
         */
        fun buildSafeFtsMatchQuery(rawQuery: String): String {
            val tokens = MemoryMappedKeywordIndex.tokenize(rawQuery)
                .map { it.replace(Regex("[^a-z0-9_]"), "") }
                .filter { it.length >= 2 }
                .distinct()
                .take(8)
            return tokens.joinToString(" OR ") { "${it}*" }
        }
    }

    private val appContext = context.applicationContext
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
            subtitle = "Physics, Chemistry, Advanced Mathematics & AI/Computing"
        )
    )
    val packStatus: StateFlow<StandardPackStatus> = _packStatus.asStateFlow()

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
     * Generates 384-dim `all-MiniLM-L6-v2` embeddings for the supplied textbook chapters,
     * writes them into the encrypted Room database (automatically updating the Room FTS4 index),
     * and builds the companion Memory-Mapped BM25 index file.
     */
    suspend fun indexDownloadedChapters(
        standardId: String,
        streamId: String,
        chapters: List<TextbookChunk>
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

        val entities = ArrayList<RagChapterEntity>(chapters.size)
        chapters.forEachIndexed { idx, chunk ->
            val embedInput = "${chunk.topicTitle}. ${chunk.subjectName}. ${chunk.formulaTags}. ${chunk.contentText}"
            val vector = modelManager.encodeText(embedInput)
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
        refreshPackStatus(standardId, effectiveStream)
    }

    suspend fun installOrReindexPack(standardId: String, streamId: String): StandardPackStatus {
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        val chunks = SyllabusPackCatalog.getChunksForScope(standardId, effectiveStream)
        return indexDownloadedChapters(standardId, effectiveStream, chunks)
    }

    suspend fun clearPackForScope(standardId: String, streamId: String): StandardPackStatus =
        withContext(Dispatchers.IO) {
            val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
            chapterDao.deleteScope(standardId, effectiveStream)
            keywordIndex.deleteScope(standardId, effectiveStream)
            refreshPackStatus(standardId, effectiveStream)
        }

    /**
     * Hybrid retrieval fusing:
     * 1. `all-MiniLM-L6-v2` 384-dim dense vector cosine similarity stored in Encrypted Room (`0.55`)
     * 2. Room FTS4 (`rag_chapters_fts MATCH`) keyword hit ranking (`0.25`)
     * 3. Memory-Mapped Inverted Keyword Index BM25 score (`0.20`)
     * Strictly isolated to `(standardId, streamId)`.
     */
    suspend fun retrieveRelevantChapters(
        query: String,
        standardId: String,
        streamId: String,
        topK: Int = 3
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
            val rankScore = (1f - (idx.toFloat() / ftsHits.size.coerceAtLeast(1).toFloat()) * 0.5f).coerceIn(0.4f, 1f)
            entity.chunkId to rankScore
        }.toMap()

        val bm25Hits = keywordIndex.search(standardId, effectiveStream, query)

        scopedChapters.map { entity ->
            val storedVec = EncryptedRagDatabase.decodeFloatArray(entity.embeddingBlob)
            val dense = cosineSimilarity(queryVec, storedVec)
            val ftsScore = ftsRankByChunkId[entity.chunkId] ?: 0f
            val bm25Pair = bm25Hits[entity.chunkId]
            val bm25 = bm25Pair?.first ?: 0f
            val keywords = bm25Pair?.second ?: emptyList()

            val fused = (DENSE_WEIGHT * dense) + (FTS_WEIGHT * ftsScore) + (BM25_MMAP_WEIGHT * bm25)
            RetrievedChunk(
                chunk = entity.toTextbookChunk(),
                denseScore = dense,
                bm25Score = maxOf(bm25, ftsScore),
                fusedScore = fused,
                matchedKeywords = keywords
            )
        }
            .filter { it.fusedScore >= MIN_RELEVANCE_THRESHOLD || it.bm25Score > 0f }
            .sortedByDescending { it.fusedScore }
            .take(topK)
    }

    suspend fun answerOffline(
        query: String,
        standardId: String,
        streamId: String
    ): RagAnswerResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        if (chapterDao.getCountForScope(standardId, effectiveStream) == 0) {
            installOrReindexPack(standardId, effectiveStream)
        }

        val retrieved = retrieveRelevantChapters(query, standardId, effectiveStream, topK = 3)
        val (standardTitle, _) = SyllabusPackCatalog.getPackMetadata(standardId, effectiveStream)
        val (answer, mode) = modelManager.synthesizeGroundedAnswer(query, retrieved, standardTitle)
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
