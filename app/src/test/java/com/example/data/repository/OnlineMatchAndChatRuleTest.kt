package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.games.chess.ChessGameState
import com.example.games.matchmaking.MatchmakingQueueResult
import com.example.games.matchmaking.OnlineMatchRepository
import com.example.security.KeystoreCryptoManager
import com.example.viewmodel.GameType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OnlineMatchAndChatRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun matchmaking_atomicClaimAndEncryptedTurnEnforcement_succeedsBetweenTwoScholars() = runBlocking {
        val aliceEmail = "alice_match_${UUID.randomUUID()}@test.com"
        val aliceUid = signInTestUser(aliceEmail)
        val bobEmail = "bob_match_${UUID.randomUUID()}@test.com"
        val charlieEmail = "charlie_match_${UUID.randomUUID()}@test.com"

        val repo = OnlineMatchRepository(firestore)
        val initialBoard = ChessGameState.initialBoard().serialize()

        // 1. Alice enters queue -> gets Searching ticket
        val aliceQueue = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.findOrQueueMatch(
                uid = aliceUid,
                vidyaId = "alice#1111",
                displayName = "Alice",
                gameType = GameType.SHATRANJ,
                eloRating = 1200,
                requiredPlayers = 2,
                initialSerializedState = initialBoard
            )
        }
        assertTrue(aliceQueue is MatchmakingQueueResult.WaitingOnTicket)
        val aliceTicketId = (aliceQueue as MatchmakingQueueResult.WaitingOnTicket).ticketId

        // 2. Bob signs in and enters queue -> atomically claims Alice's ticket + creates encrypted onlineMatch
        val bobUid = signInTestUser(bobEmail)
        val bobQueue = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.findOrQueueMatch(
                uid = bobUid,
                vidyaId = "bob#2222",
                displayName = "Bob",
                gameType = GameType.SHATRANJ,
                eloRating = 1220,
                requiredPlayers = 2,
                initialSerializedState = initialBoard
            )
        }
        assertTrue(bobQueue is MatchmakingQueueResult.Matched)
        val matched = bobQueue as MatchmakingQueueResult.Matched
        assertEquals(aliceUid, matched.opponentUid)
        assertEquals(1, matched.localPlayerIndex)

        // 3. Verify Alice's ticket transitioned to matched
        val ticketAfter = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTicket(aliceTicketId).first { it?.status == "matched" }
        }
        assertEquals(matched.matchId, ticketAfter?.matchId)

        // 4. Bob tries to submit moveSeq=1 while it's Alice's turn -> must fail PERMISSION_DENIED
        val matchForBob = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getMatchOnce(matched.matchId)!! }
        val bobKey = repo.resolveMatchKey(matchForBob, bobUid)
        assertNotNull(bobKey)
        assertEquals(initialBoard, repo.verifyAndDecryptMatchState(matchForBob, bobKey!!))

        val outOfTurnResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitEncryptedTurn(
                matchId = matched.matchId,
                nextTurnUid = aliceUid,
                nextMoveSeq = 1,
                serializedState = "SPOOFED|BLACK",
                sessionKey = bobKey
            )
        }
        assertTrue(outOfTurnResult.isFailure)
        val outOfTurnEx = outOfTurnResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, outOfTurnEx?.code)

        // 5. Sign in as Alice -> she unwraps her RSA-wrapped key and submits moveSeq=1
        signInTestUser(aliceEmail)
        val matchForAlice = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getMatchOnce(matched.matchId)!! }
        val aliceKey = repo.resolveMatchKey(matchForAlice, aliceUid)
        assertNotNull(aliceKey)
        assertEquals(initialBoard, repo.verifyAndDecryptMatchState(matchForAlice, aliceKey!!))

        val move1Result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitEncryptedTurn(
                matchId = matched.matchId,
                nextTurnUid = bobUid,
                nextMoveSeq = 1,
                serializedState = "NEXT_BOARD|BLACK",
                sessionKey = aliceKey
            )
        }
        assertTrue(move1Result.isSuccess)

        // Bob decrypts moveSeq=1 and verifies digest integrity
        signInTestUser(bobEmail)
        val updatedMatch = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getMatchOnce(matched.matchId)!! }
        assertEquals(1, updatedMatch.moveSeq)
        assertEquals("NEXT_BOARD|BLACK", repo.verifyAndDecryptMatchState(updatedMatch, bobKey))

        // Outsider Charlie cannot read the match
        val charlieUid = signInTestUser(charlieEmail)

        // Outsider Charlie cannot read the match
        var charlieDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(OnlineMatchRepository.COLLECTION_ONLINE_MATCHES)
                    .document(matched.matchId)
                    .get()
                    .await()
            }
        } catch (e: FirebaseFirestoreException) {
            charlieDenied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Outsider $charlieUid must receive PERMISSION_DENIED on onlineMatch", charlieDenied)
    }

    @Test
    fun encryptedChatAndMessages_enforceMembershipAndCiphertextFields() = runBlocking {
        val aliceEmail = "alice_chat_${UUID.randomUUID()}@test.com"
        val bobEmail = "bob_chat_${UUID.randomUUID()}@test.com"
        val charlieEmail = "charlie_chat_${UUID.randomUUID()}@test.com"

        val aliceUid = signInTestUser(aliceEmail)
        val alicePub = KeystoreCryptoManager.ensureIdentityKeyPair(aliceUid)

        val bobUid = signInTestUser(bobEmail)
        val bobPub = KeystoreCryptoManager.ensureIdentityKeyPair(bobUid)

        // Alice creates an E2EE chat with Bob
        signInTestUser(aliceEmail)
        val chatId = "chat_rule_${UUID.randomUUID().toString().take(8)}"
        val sessionDek = KeystoreCryptoManager.generateSessionAesKey()
        val (previewCipher, previewIv) = KeystoreCryptoManager.encryptWithSessionKey("Hello Bob!", sessionDek)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("chats").document(chatId).set(
                mapOf(
                    "createdBy" to aliceUid,
                    "participants" to listOf(aliceUid, bobUid),
                    "name" to "Bob Scholar",
                    "peerVidyaId" to "bob#2222",
                    "lastMessage" to previewCipher,
                    "lastMessageIv" to previewIv,
                    "lastTimestamp" to System.currentTimeMillis(),
                    "type" to "individual",
                    "subjectTag" to "Physics",
                    "unreadCount" to 0,
                    "isOnline" to true,
                    "timestampLabel" to "Just now",
                    "wrappedKeys" to mapOf(
                        aliceUid to KeystoreCryptoManager.wrapSessionKey(sessionDek, alicePub),
                        bobUid to KeystoreCryptoManager.wrapSessionKey(sessionDek, bobPub)
                    ),
                    "keyVersion" to 1,
                    "isEncrypted" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val (msgCipher, msgIv) = KeystoreCryptoManager.encryptWithSessionKey("Secret quantum formula", sessionDek)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("chats").document(chatId)
                .collection("messages").document("m1")
                .set(
                    mapOf(
                        "senderId" to aliceUid,
                        "senderName" to "Alice",
                        "senderVidyaId" to "alice#1111",
                        "memberUids" to listOf(aliceUid, bobUid),
                        "text" to "[Encrypted]",
                        "ciphertext" to msgCipher,
                        "iv" to msgIv,
                        "keyVersion" to 1,
                        "isEncrypted" to true,
                        "timestamp" to System.currentTimeMillis(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        // Bob reads and decrypts the message end-to-end
        signInTestUser(bobEmail)
        val chatSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("chats").document(chatId).get().await()
        }
        val wrappedKeys = chatSnap.get("wrappedKeys") as Map<*, *>
        val bobUnwrapped = KeystoreCryptoManager.unwrapSessionKey(bobUid, wrappedKeys[bobUid].toString())
        assertNotNull(bobUnwrapped)
        assertEquals(
            "Hello Bob!",
            KeystoreCryptoManager.decryptWithSessionKey(
                chatSnap.getString("lastMessage").orEmpty(),
                chatSnap.getString("lastMessageIv").orEmpty(),
                bobUnwrapped!!
            )
        )

        val msgSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("chats").document(chatId).collection("messages").document("m1").get().await()
        }
        assertEquals(
            "Secret quantum formula",
            KeystoreCryptoManager.decryptWithSessionKey(
                msgSnap.getString("ciphertext").orEmpty(),
                msgSnap.getString("iv").orEmpty(),
                bobUnwrapped
            )
        )

        // Outsider Charlie cannot query messages
        signInTestUser(charlieEmail)
        var charlieDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("chats").document(chatId).collection("messages").document("m1").get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            charlieDenied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Outsider must fail PERMISSION_DENIED reading E2EE messages", charlieDenied)
    }

    @Test
    fun awaitLiveMatch_pairsConcurrentSearchingTickets_andCancelsOwnTicket() = runBlocking {
        val aliceEmail = "alice_concurrent_${UUID.randomUUID()}@test.com"
        val bobEmail = "bob_concurrent_${UUID.randomUUID()}@test.com"

        val aliceUid = signInTestUser(aliceEmail)
        val alicePub = KeystoreCryptoManager.ensureIdentityKeyPair(aliceUid)
        val bobUid = signInTestUser(bobEmail)
        val bobPub = KeystoreCryptoManager.ensureIdentityKeyPair(bobUid)

        // Simulate both players having created searching tickets at the exact same moment
        signInTestUser(aliceEmail)
        val aliceTicketId = "ticket_a_${UUID.randomUUID().toString().take(8)}"
        firestore.collection(OnlineMatchRepository.COLLECTION_TICKETS).document(aliceTicketId).set(
            mapOf(
                "uid" to aliceUid,
                "vidyaId" to "alice#1111",
                "displayName" to "Alice",
                "gameType" to GameType.LUDO.name,
                "eloRating" to 1200,
                "requiredPlayers" to 2,
                "publicKey" to alicePub,
                "status" to "searching",
                "matchId" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        signInTestUser(bobEmail)
        val bobTicketId = "ticket_b_${UUID.randomUUID().toString().take(8)}"
        firestore.collection(OnlineMatchRepository.COLLECTION_TICKETS).document(bobTicketId).set(
            mapOf(
                "uid" to bobUid,
                "vidyaId" to "bob#2222",
                "displayName" to "Bob",
                "gameType" to GameType.LUDO.name,
                "eloRating" to 1900, // Wide ELO difference to also verify real-player matching works
                "requiredPlayers" to 2,
                "publicKey" to bobPub,
                "status" to "searching",
                "matchId" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Bob runs awaitLiveMatch (with short timeout > 3s tie-breaker so even if bobUid > aliceUid he claims)
        val repo = OnlineMatchRepository(firestore)
        val matched = withTimeout(10_000L) {
            repo.awaitLiveMatch(
                myTicketId = bobTicketId,
                uid = bobUid,
                gameType = GameType.LUDO,
                eloRating = 1900,
                initialSerializedState = "RED=0:-1|RED|1|false",
                timeoutMs = 8_000L,
                pollIntervalMs = 1_000L
            )
        }
        assertNotNull("Concurrent live players must match instead of falling back to bot", matched)
        assertEquals(aliceUid, matched!!.opponentUid)

        val bobTicketAfter = firestore.collection(OnlineMatchRepository.COLLECTION_TICKETS).document(bobTicketId).get().await()
        assertEquals("cancelled", bobTicketAfter.getString("status"))
        val aliceTicketAfter = firestore.collection(OnlineMatchRepository.COLLECTION_TICKETS).document(aliceTicketId).get().await()
        assertEquals("matched", aliceTicketAfter.getString("status"))
        assertEquals(matched.matchId, aliceTicketAfter.getString("matchId"))
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
