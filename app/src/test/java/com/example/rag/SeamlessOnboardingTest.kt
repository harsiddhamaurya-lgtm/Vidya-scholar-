package com.example.rag

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.UserProfile
import com.example.security.LocalProfileStore
import com.example.utils.OfflineCosmiumKnowledgeEngine
import com.example.viewmodel.AiTutorViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SeamlessOnboardingTest {

    private val forbiddenUiTerms = listOf(
        "qwen",
        "gemma",
        "minilm",
        "hugging face",
        "huggingface",
        ".onnx",
        "sqlcipher",
        "room fts",
        "mmap",
        "ram guardrail",
        "offline vault",
        "4-bit brain"
    )

    @Test
    fun offlineCosmiumKnowledgeEngine_emitsCleanStudentCopyWithoutTechnicalJargon() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        RagRoomDatabase.resetInstanceForTesting()
        app.deleteDatabase(RagRoomDatabase.ENCRYPTED_DB_NAME)

        val manager = OfflineRAGManager(app)
        manager.installOrReindexPack("class_11", "science_pcm")

        val ragReply = OfflineCosmiumKnowledgeEngine.generateRagOfflineAnswer(
            ragManager = manager,
            query = "Explain derivative power rule",
            standardId = "class_11",
            streamId = "science_pcm"
        )
        assertNull(ragReply.brainBadge)
        val lowerRag = ragReply.text.lowercase()
        forbiddenUiTerms.forEach { term ->
            assertFalse("Reply must not expose '$term'", lowerRag.contains(term))
        }

        val unknownReply = OfflineCosmiumKnowledgeEngine.generateRagOfflineAnswer(
            ragManager = manager,
            query = "xyzzy_unmatched_topic_query",
            standardId = "class_11",
            streamId = "science_pcm"
        )
        assertNull(unknownReply.brainBadge)
        val lowerUnknown = unknownReply.text.lowercase()
        forbiddenUiTerms.forEach { term ->
            assertFalse("Fallback reply must not expose '$term'", lowerUnknown.contains(term))
        }

        val quickFallback = OfflineCosmiumKnowledgeEngine.generateOfflineAnswer("quantum mechanics")
        assertNull(quickFallback.brainBadge)
        val lowerQuick = quickFallback.text.lowercase()
        forbiddenUiTerms.forEach { term ->
            assertFalse("Quick answer must not expose '$term'", lowerQuick.contains(term))
        }

        RagRoomDatabase.resetInstanceForTesting()
    }

    @Test
    fun localProfileStore_roundTripsPreferredLanguage() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = LocalProfileStore(app)
        store.clearAllProfiles()

        val profile = UserProfile(
            uid = "lang_test_uid",
            name = "Meera Scholar",
            standardId = "class_12",
            streamId = "science_pcb",
            preferredLanguage = "hi",
            onboardingCompleted = true
        )
        store.saveProfile(profile)

        val restored = store.getProfile("lang_test_uid")
        assertEquals("hi", restored?.preferredLanguage)
        assertEquals("class_12", restored?.standardId)
        assertEquals("science_pcb", restored?.streamId)
        store.clearAllProfiles()
    }

    @Test
    fun aiTutorViewModel_welcomeMessageHasNoTechnicalJargonAndProvisioningCompletes() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        RagRoomDatabase.resetInstanceForTesting()
        app.deleteDatabase(RagRoomDatabase.ENCRYPTED_DB_NAME)

        val vm = AiTutorViewModel(app)
        val welcome = vm.messages.value.first().text.lowercase()
        forbiddenUiTerms.forEach { term ->
            assertFalse("Welcome message must not expose '$term'", welcome.contains(term))
        }

        val readySignal = java.util.concurrent.atomic.AtomicBoolean(false)
        vm.startOnboardingProvisioning(
            standardId = "class_12",
            streamId = "science_pcm",
            preferredLanguage = "ta",
            fetchRemoteWeights = false,
            onReady = { readySignal.set(true) }
        )

        val deadline = System.currentTimeMillis() + 15_000L
        while (!readySignal.get() && System.currentTimeMillis() < deadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(25L)
        }
        assertTrue("Onboarding provisioning did not complete in time", readySignal.get())

        assertTrue(vm.onboardingProvisioning.value.isComplete)
        assertEquals(1f, vm.onboardingProvisioning.value.progress, 0.001f)
        assertTrue(vm.vaultUiState.value.packStatus.isInstalled)
        assertEquals("class_12", vm.vaultUiState.value.standardId)
        assertEquals("science_pcm", vm.vaultUiState.value.streamId)
        assertEquals("ta", vm.vaultUiState.value.selectedVoiceLanguageCode)

        RagRoomDatabase.resetInstanceForTesting()
    }
}
