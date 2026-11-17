package com.example.rag

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthRepository
import com.example.data.ChatMessage
import com.example.utils.CosmiumVoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceQuizAndChatHistoryTest {

    @Test
    fun cosmosChatRepository_persistsLazyLoadsPinsAndDeletesOffline() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        RagRoomDatabase.resetInstanceForTesting()
        app.deleteDatabase(RagRoomDatabase.ENCRYPTED_DB_NAME)

        val db = RagRoomDatabase.getInstance(app)
        val repo = CosmosChatRepository(
            context = app,
            roomDb = db,
            authRepo = AuthRepository(),
            firestoreProvider = { null }
        )

        val userId = "test_scholar_uid"
        val session1 = repo.createNewSession("class_11", "science_pcm", "Session One", userId = userId)
        delay(10)
        val session2 = repo.createNewSession("class_11", "science_pcm", "Session Two", userId = userId)

        // Populate 25 messages in session1 to test lazy-loaded pagination
        for (i in 1..25) {
            repo.saveMessage(
                sessionId = session1.sessionId,
                message = ChatMessage(
                    id = "msg_$i",
                    text = "Message number $i",
                    isUser = (i % 2 == 1)
                ),
                standardId = "class_11",
                streamId = "science_pcm",
                userId = userId
            )
            delay(2)
        }

        // Verify total count & lazy-load pages (newest-first from DAO)
        assertEquals(25, repo.getMessageCount(session1.sessionId))
        val page1 = repo.getMessagesPaged(session1.sessionId, limit = 20, offset = 0)
        assertEquals(20, page1.size)
        assertEquals("msg_25", page1.first().messageId)

        val page2 = repo.getMessagesPaged(session1.sessionId, limit = 20, offset = 20)
        assertEquals(5, page2.size)
        assertEquals("msg_5", page2.first().messageId)
        assertEquals("msg_1", page2.last().messageId)

        // Touch session2 after session1's messages so session2 is strictly newer
        delay(5)
        repo.saveMessage(
            sessionId = session2.sessionId,
            message = ChatMessage(id = "msg_s2_1", text = "Newer session message", isUser = true),
            standardId = "class_11",
            streamId = "science_pcm",
            userId = userId
        )

        // Verify pinning moves session1 ahead of newer session2
        var sessions = repo.getUserSessions(userId)
        assertEquals(session2.sessionId, sessions.first().sessionId)

        repo.setChatPinned(session1.sessionId, true)
        sessions = repo.getUserSessions(userId)
        assertEquals(session1.sessionId, sessions.first().sessionId)
        assertTrue(sessions.first().isPinned)

        // Verify deletion hides session and removes local messages while keeping tombstone for cloud sync
        repo.deleteSession(session1.sessionId)
        sessions = repo.getUserSessions(userId)
        assertFalse(sessions.any { it.sessionId == session1.sessionId })
        assertEquals(0, repo.getMessageCount(session1.sessionId))

        val unsynced = db.cosmosChatDao().getUnsyncedSessions(userId)
        val deletedTombstone = unsynced.firstOrNull { it.sessionId == session1.sessionId }
        assertNotNull(deletedTombstone)
        assertTrue(deletedTombstone?.isDeleted == true)

        RagRoomDatabase.resetInstanceForTesting()
    }

    @Test
    fun cosmosVoiceQuizManager_generatesQuestionsFromTextbookPacksAndEvaluatesAnswers() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val voiceManager = CosmiumVoiceManager(app)
        val quizManager = CosmosVoiceQuizManager(app, voiceManager, scope)

        quizManager.startQuizSession("class_11", "commerce", numQuestions = 3)
        val initialState = quizManager.uiState.value
        assertEquals(VoiceQuizStep.ASKING_QUESTION, initialState.step)
        assertEquals(3, initialState.totalQuestions)
        val question = initialState.currentQuestion
        assertNotNull(question)

        // Without RECORD_AUDIO granted in Robolectric, calling startListeningForAnswer should degrade gracefully
        quizManager.startListeningForAnswer()
        val listeningState = quizManager.uiState.value
        assertEquals(VoiceQuizStep.LISTENING, listeningState.step)
        assertFalse(listeningState.isMicAvailable)

        // Submit canonical answer keyword -> verify score increments
        val answerToSubmit = question!!.expectedKeywords.firstOrNull() ?: question.canonicalAnswer
        quizManager.submitAnswer(answerToSubmit)
        val evaluated = quizManager.uiState.value
        assertEquals(VoiceQuizStep.FEEDBACK_SPEAKING, evaluated.step)
        assertTrue(evaluated.isAnswerCorrect == true)
        assertEquals(1, evaluated.score)

        quizManager.release()
        voiceManager.shutdown()
        scope.cancel()
    }
}
