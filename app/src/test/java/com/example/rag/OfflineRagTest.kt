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

    @Test
    fun offlineRag_staysStrictlyOnTopicForWorkEnergyWithoutLeakingFrictionOrThermo() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        RagRoomDatabase.resetInstanceForTesting()
        app.deleteDatabase(RagRoomDatabase.ENCRYPTED_DB_NAME)

        val manager = OfflineRAGManager(app)
        val result = manager.answerOffline(
            query = "explain work energy theorem",
            standardId = "class_11",
            streamId = "science_pcm"
        )

        assertTrue(result.retrievedChunks.isNotEmpty())
        assertEquals("c11_pcm_phy_ch6_work_energy", result.retrievedChunks.first().chunk.chunkId)
        assertTrue(result.answerText.contains("Work-Energy Theorem"))
        assertTrue(result.answerText.contains("W_{\\text{net}} = \\Delta K"))
        assertFalse("Must not mix static friction f_max into Work-Energy", result.answerText.contains("f_max"))
        assertFalse("Must not mix Impulse into Work-Energy", result.answerText.contains("Impulse"))
        assertFalse("Must not dump Thermodynamics into Work-Energy", result.answerText.contains("First Law of Thermodynamics"))

        RagRoomDatabase.resetInstanceForTesting()
    }

    @Test
    fun offlineRag_handlesAllStandardsStreamsAccuratelyAndEvictsOldScopesOnSwitch() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        RagRoomDatabase.resetInstanceForTesting()
        app.deleteDatabase(RagRoomDatabase.ENCRYPTED_DB_NAME)

        val manager = OfflineRAGManager(app)

        // 1. Switch to Class 9 -> retrieves French Revolution & Heron's Formula accurately
        manager.switchActiveScope("class_9", "general")
        val c9French = manager.answerOffline("When was the Bastille stormed in the French Revolution?", "class_9", "general")
        assertEquals("c9_gen_sst_ch1_french", c9French.retrievedChunks.first().chunk.chunkId)
        assertTrue(c9French.answerText.contains("14 July 1789"))

        val c9Heron = manager.answerOffline("Calculate triangle area using Heron formula semiperimeter", "class_9", "general")
        assertEquals("c9_gen_math_ch10_heron", c9Heron.retrievedChunks.first().chunk.chunkId)
        assertTrue(c9Heron.answerText.contains("\\sqrt{s(s - a)(s - b)(s - c)}"))

        // 2. Switch to Class 10 -> evicts Class 9 completely from Room & MMap disk index
        manager.switchActiveScope("class_10", "general")
        assertEquals(0, manager.roomDb.ragChapterDao().getCountForScope("class_9", "general"))
        assertEquals(0L, manager.keywordIndex.getIndexSizeBytes("class_9", "general"))

        val c10Real = manager.answerOffline("Fundamental Theorem of Arithmetic HCF LCM", "class_10", "general")
        assertEquals("c10_gen_math_ch1_real", c10Real.retrievedChunks.first().chunk.chunkId)
        assertTrue(c10Real.answerText.contains("\\text{HCF}(a, b)"))

        // 3. Switch to Class 12 Commerce -> evicts Class 10, answers ratios accurately, rejects off-topic bio
        manager.switchActiveScope("class_12", "commerce")
        assertEquals(0, manager.roomDb.ragChapterDao().getCountForScope("class_10", "general"))
        assertEquals(0L, manager.keywordIndex.getIndexSizeBytes("class_10", "general"))

        val c12Goodwill = manager.answerOffline("Calculate partnership goodwill using Super Profit method", "class_12", "commerce")
        assertEquals("c12_com_acc_ch3_goodwill", c12Goodwill.retrievedChunks.first().chunk.chunkId)
        assertTrue(c12Goodwill.answerText.contains("Super Profit"))

        val offTopic = manager.answerOffline("Explain chocolate cake baking recipe ingredients", "class_12", "commerce")
        assertTrue("Off-topic query should return empty retrieval rather than hallucinating", offTopic.retrievedChunks.isEmpty())

        // 4. Switch to Class 12 Arts -> evicts Commerce, verifies historical constitution dates
        manager.switchActiveScope("class_12", "arts")
        assertEquals(0, manager.roomDb.ragChapterDao().getCountForScope("class_12", "commerce"))

        val c12Hist = manager.answerOffline("When was the Indian Constitution adopted and Champaran satyagraha?", "class_12", "arts")
        assertEquals("c12_art_hist_ch1_mod", c12Hist.retrievedChunks.first().chunk.chunkId)
        assertTrue(c12Hist.answerText.contains("26 November 1949"))

        RagRoomDatabase.resetInstanceForTesting()
    }
}
