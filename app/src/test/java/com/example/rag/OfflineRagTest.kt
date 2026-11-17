package com.example.rag

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineRagTest {

    @Test
    fun offlineRagManager_indexesInEncryptedRoomWithFts4AndRetrievesGroundedChapters() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        RagRoomDatabase.resetInstanceForTesting()
        app.deleteDatabase(RagRoomDatabase.ENCRYPTED_DB_NAME)

        val manager = OfflineRAGManager(app)
        assertEquals(ModelLifecycleState.UNLOADED, manager.modelManager.lifecycleState.value)

        // Index Class 11 Science (PCM) chapters into Room + FTS4 + MMap
        val status = manager.installOrReindexPack("class_11", "science_pcm")
        assertTrue(status.isInstalled)
        assertTrue(status.chunkCount >= 5)
        assertTrue(status.mmapIndexSizeBytes > 0L)

        // Verify reactive Room Flow observes persisted chapters with 384-dim embeddings
        val persisted = manager.observeChaptersForScope("class_11", "science_pcm").first()
        assertEquals(status.chunkCount, persisted.size)
        assertEquals(
            EncryptedRagDatabase.VECTOR_DIM,
            EncryptedRagDatabase.decodeFloatArray(persisted.first().embeddingBlob).size
        )

        // Verify Room FTS4 MATCH query works directly on rag_chapters_fts
        val ftsQuery = OfflineRAGManager.buildSafeFtsMatchQuery("Explain Bohr quantum orbital & redox")
        val ftsMatches = manager.roomDb.ragChapterDao().searchFtsInScope(
            ftsQuery = ftsQuery,
            standardId = "class_11",
            streamId = "science_pcm"
        )
        assertTrue(ftsMatches.any { it.chunkId == "c11_pcm_chem_ch2_atomic" })

        // Verify full RAG retrieval + grounded offline answer
        val result = manager.answerOffline(
            query = "How do I find derivative using power rule d/dx(x^n)?",
            standardId = "class_11",
            streamId = "science_pcm"
        )
        assertEquals(BrainMode.OFFLINE_HYBRID_RAG, result.brainMode)
        assertTrue(result.retrievedChunks.isNotEmpty())
        assertEquals("c11_pcm_math_ch13_calc", result.retrievedChunks.first().chunk.chunkId)
        assertTrue(result.answerText.contains("12x^3 - 10x") || result.answerText.contains("Power Rule"))

        // Verify strict scope isolation (Commerce pack is isolated from PCM pack)
        assertEquals(0, manager.roomDb.ragChapterDao().getCountForScope("class_11", "commerce"))

        manager.unloadModelsFromRam()
        assertEquals(ModelLifecycleState.UNLOADED, manager.modelManager.lifecycleState.value)
        RagRoomDatabase.resetInstanceForTesting()
    }

    @Test
    fun memoryMappedKeywordIndex_writesAndSearchesViaMappedByteBuffer() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val idxDir = File(app.filesDir, "test_mmap_idx_${System.nanoTime()}")
        val index = MemoryMappedKeywordIndex(idxDir)

        val chunks = SyllabusPackCatalog.getChunksForScope("class_12", "science_pcm")
        assertTrue(chunks.isNotEmpty())

        val outFile = index.buildAndPersist("class_12", "science_pcm", chunks)
        assertTrue(outFile.exists())
        assertTrue(outFile.length() > 16L)

        val hits = index.search("class_12", "science_pcm", "Explain Kirchhoff loop rule and Wheatstone bridge")
        assertTrue(hits.isNotEmpty())
        val topChunkId = hits.maxByOrNull { it.value.first }?.key
        assertEquals("c12_pcm_phy_ch3_kirchhoff", topChunkId)
        assertTrue(hits[topChunkId]?.second?.contains("kirchhoff") == true)

        idxDir.deleteRecursively()
    }

    @Test
    fun encryptedRagDatabase_roundTripsFloatVectorsAndIsolatesScopes() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.deleteDatabase(EncryptedRagDatabase.DB_NAME)
        val db = EncryptedRagDatabase(app)

        val pcmChunk = SyllabusPackCatalog.getChunksForScope("class_11", "science_pcm").first()
        val commerceChunk = SyllabusPackCatalog.getChunksForScope("class_11", "commerce").first()

        val pcmVec = FloatArray(EncryptedRagDatabase.VECTOR_DIM) { idx -> if (idx % 2 == 0) 0.5f else -0.25f }
        val comVec = FloatArray(EncryptedRagDatabase.VECTOR_DIM) { idx -> if (idx % 3 == 0) 0.7f else 0.1f }

        db.upsertChunks(
            listOf(
                VectorizedChunk(pcmChunk, pcmVec),
                VectorizedChunk(commerceChunk, comVec)
            )
        )

        val fetchedPcm = db.getChunksForScope("class_11", "science_pcm")
        assertEquals(1, fetchedPcm.size)
        assertEquals(pcmChunk.chunkId, fetchedPcm.first().chunk.chunkId)
        assertArrayEquals(pcmVec, fetchedPcm.first().embedding, 1e-5f)

        assertEquals(1, db.getChunkCountForScope("class_11", "commerce"))
        assertEquals(0, db.getChunkCountForScope("class_11", "arts"))
        assertFalse(fetchedPcm.any { it.chunk.streamId == "commerce" })

        db.deleteScope("class_11", "science_pcm")
        assertEquals(0, db.getChunkCountForScope("class_11", "science_pcm"))
        assertEquals(1, db.getChunkCountForScope("class_11", "commerce"))
        db.close()
    }

    @Test
    fun syllabusCatalog_normalizesClass9And10StreamsToGeneralFoundation() {
        val c10Math = SyllabusPackCatalog.getChunksForScope("class_10", "science_pcm")
        assertTrue(c10Math.isNotEmpty())
        assertTrue(c10Math.all { it.streamId == "general" })
    }
}
