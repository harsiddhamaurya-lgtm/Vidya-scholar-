package com.example.games.matchmaking

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.security.KeystoreCryptoManager
import com.example.viewmodel.GameType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.math.abs

sealed interface MatchmakingQueueResult {
    data class Matched(
        val matchId: String,
        val opponentUid: String,
        val opponentName: String,
        val opponentVidyaId: String,
        val opponentElo: Int,
        val localPlayerIndex: Int
    ) : MatchmakingQueueResult

    data class WaitingOnTicket(
        val ticketId: String
    ) : MatchmakingQueueResult

    data object FallbackToBot : MatchmakingQueueResult
}

class OnlineMatchRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    suspend fun findOrQueueMatch(
        uid: String,
        vidyaId: String,
        displayName: String,
        gameType: GameType,
        eloRating: Int,
        requiredPlayers: Int,
        initialSerializedState: String
    ): MatchmakingQueueResult {
        if (uid.isBlank() || uid.startsWith("demo_")) {
            return MatchmakingQueueResult.FallbackToBot
        }
        val myPublicKey = KeystoreCryptoManager.ensureIdentityKeyPair(uid)
        val ticketsCol = db.collection(COLLECTION_TICKETS)

        return try {
            // Clean up any abandoned searching tickets from prior crashes for this user
            cleanupOwnOpenTickets(uid)

            // 1. Scan open searching tickets for this game mode
            val candidate = findBestOpenCandidate(
                uid = uid,
                gameType = gameType,
                eloRating = eloRating,
                maxEloDelta = 500,
                serverNowMs = null
            )

            if (candidate != null) {
                val claimed = attemptAtomicTicketClaim(
                    opponentTicket = candidate,
                    claimerUid = uid,
                    claimerPubKey = myPublicKey,
                    gameType = gameType,
                    initialSerializedState = initialSerializedState,
                    myActiveTicketId = null
                )
                if (claimed != null) {
                    return claimed
                }
            }

            // 2. No compatible open ticket yet -> publish our own searching ticket
            val ticketId = "ticket_${uid.take(20)}_${System.currentTimeMillis()}"
            val ticketPayload = mapOf(
                "uid" to uid,
                "vidyaId" to vidyaId.ifBlank { "scholar#1000" }.take(30),
                "displayName" to displayName.ifBlank { "Scholar" }.take(60),
                "gameType" to gameType.name,
                "eloRating" to eloRating.coerceIn(0, 10000),
                "requiredPlayers" to requiredPlayers.coerceIn(2, 4),
                "publicKey" to myPublicKey.take(1024),
                "status" to "searching",
                "matchId" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ticketsCol.document(ticketId).set(ticketPayload).await()
            MatchmakingQueueResult.WaitingOnTicket(ticketId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Online matchmaking unavailable, falling back to Vidyut AI: ${e.message}")
            handleFirestoreError(e, OperationType.WRITE, COLLECTION_TICKETS)
            MatchmakingQueueResult.FallbackToBot
        }
    }

    /**
     * Actively awaits a live opponent when queued in [WaitingOnTicket]:
     * - Listens to [myTicketId] in real time in case another player claims us.
     * - Periodically re-scans open tickets every 2s (widening ELO tolerance after 10s) so
     *   two players entering the queue concurrently still pair immediately into a single
     *   atomic OnlineMatch instead of both waiting passively and falling back to bots.
     */
    suspend fun awaitLiveMatch(
        myTicketId: String,
        uid: String,
        gameType: GameType,
        eloRating: Int,
        initialSerializedState: String,
        timeoutMs: Long = DEFAULT_QUEUE_TIMEOUT_MS,
        pollIntervalMs: Long = 2_000L
    ): MatchmakingQueueResult.Matched? {
        val myPublicKey = KeystoreCryptoManager.ensureIdentityKeyPair(uid)
        val deadline = System.currentTimeMillis() + timeoutMs
        val startedAt = System.currentTimeMillis()

        return coroutineScope {
            val ticketClaimDeferred = async {
                observeTicket(myTicketId)
                    .firstOrNull { it != null && it.status == "matched" && it.matchId.isNotBlank() }
                    ?.let { claimed ->
                        val remoteMatch = getMatchOnce(claimed.matchId)
                        val opponentUid = remoteMatch?.participantUids?.firstOrNull { it != uid }.orEmpty()
                        MatchmakingQueueResult.Matched(
                            matchId = claimed.matchId,
                            opponentUid = opponentUid,
                            opponentName = "Ranked Scholar",
                            opponentVidyaId = "Live Arena",
                            opponentElo = eloRating,
                            localPlayerIndex = 0
                        )
                    }
            }

            val activeScanDeferred = async {
                var ownServerTimeMs: Long? = null
                var attempt = 0
                while (System.currentTimeMillis() < deadline) {
                    delay(if (attempt == 0) 900L else pollIntervalMs)
                    attempt++
                    val elapsedMs = System.currentTimeMillis() - startedAt
                    if (ownServerTimeMs == null) {
                        ownServerTimeMs = runCatching {
                            db.collection(COLLECTION_TICKETS).document(myTicketId).get().await()
                                .getTimestamp("updatedAt")?.toDate()?.time
                        }.getOrNull()
                    }
                    // Widen ELO range after 4s so real live humans pair up rather than waiting for bot fallback
                    val eloWindow = if (elapsedMs >= 4_000L) Int.MAX_VALUE else 500
                    val candidate = findBestOpenCandidate(
                        uid = uid,
                        gameType = gameType,
                        eloRating = eloRating,
                        maxEloDelta = eloWindow,
                        serverNowMs = ownServerTimeMs?.plus(elapsedMs)
                    ) ?: continue

                    // Deterministic leader tie-breaker for first 3 seconds so concurrent searchers don't collide
                    if (elapsedMs < 3_000L && uid > candidate.uid) {
                        continue
                    }

                    val matched = attemptAtomicTicketClaim(
                        opponentTicket = candidate,
                        claimerUid = uid,
                        claimerPubKey = myPublicKey,
                        gameType = gameType,
                        initialSerializedState = initialSerializedState,
                        myActiveTicketId = myTicketId
                    )
                    if (matched != null) {
                        return@async matched
                    }
                }
                null
            }

            try {
                val winner: MatchmakingQueueResult.Matched? = select {
                    ticketClaimDeferred.onAwait { it }
                    activeScanDeferred.onAwait { res ->
                        if (res != null) res else ticketClaimDeferred.await()
                    }
                }
                winner
            } finally {
                coroutineContext.cancelChildren()
            }
        }
    }

    private suspend fun cleanupOwnOpenTickets(uid: String) {
        try {
            val ownOpen = db.collection(COLLECTION_TICKETS)
                .whereEqualTo("uid", uid)
                .whereEqualTo("status", "searching")
                .limit(10)
                .get()
                .await()
            for (doc in ownOpen.documents) {
                runCatching {
                    doc.reference.update(
                        mapOf(
                            "status" to "cancelled",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun findBestOpenCandidate(
        uid: String,
        gameType: GameType,
        eloRating: Int,
        maxEloDelta: Int,
        serverNowMs: Long?
    ): MatchmakingTicket? {
        return try {
            val snap = db.collection(COLLECTION_TICKETS)
                .whereEqualTo("status", "searching")
                .whereEqualTo("gameType", gameType.name)
                .limit(40)
                .get()
                .await()

            snap.documents
                .mapNotNull { doc ->
                    doc.toObject(MatchmakingTicket::class.java)?.copy(ticketId = doc.id)
                }
                .filter { ticket ->
                    if (ticket.uid.isBlank() || ticket.uid == uid) return@filter false
                    if (maxEloDelta != Int.MAX_VALUE && abs(ticket.eloRating - eloRating) > maxEloDelta) {
                        return@filter false
                    }
                    // Exclude abandoned zombie tickets older than 120s relative to server clock
                    val ticketServerMs = ticket.updatedAt?.toDate()?.time
                    if (serverNowMs != null && ticketServerMs != null && (serverNowMs - ticketServerMs) > STALE_TICKET_AGE_MS) {
                        return@filter false
                    }
                    true
                }
                .sortedWith(
                    compareByDescending<MatchmakingTicket> { it.updatedAt?.toDate()?.time ?: 0L }
                        .thenBy { abs(it.eloRating - eloRating) }
                )
                .firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun attemptAtomicTicketClaim(
        opponentTicket: MatchmakingTicket,
        claimerUid: String,
        claimerPubKey: String,
        gameType: GameType,
        initialSerializedState: String,
        myActiveTicketId: String?
    ): MatchmakingQueueResult.Matched? {
        val matchId = "match_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val matchRef = db.collection(COLLECTION_ONLINE_MATCHES).document(matchId)
        val opponentTicketRef = db.collection(COLLECTION_TICKETS).document(opponentTicket.ticketId)

        return try {
            val sessionKey = KeystoreCryptoManager.generateSessionAesKey()
            val wrappedKeys = mutableMapOf<String, String>()
            if (claimerPubKey.isNotBlank()) {
                val w = KeystoreCryptoManager.wrapSessionKey(sessionKey, claimerPubKey)
                if (w.isNotBlank()) wrappedKeys[claimerUid] = w
            }
            if (opponentTicket.publicKey.isNotBlank()) {
                val w = KeystoreCryptoManager.wrapSessionKey(sessionKey, opponentTicket.publicKey)
                if (w.isNotBlank()) wrappedKeys[opponentTicket.uid] = w
            }

            val (encState, stateIv) = KeystoreCryptoManager.encryptWithSessionKey(initialSerializedState, sessionKey)
            val stateDigest = KeystoreCryptoManager.sha256Digest("$matchId|0|$encState")

            val participants = listOf(opponentTicket.uid, claimerUid)
            val batch = db.batch()
            batch.set(
                matchRef,
                mapOf(
                    "createdBy" to claimerUid,
                    "participantUids" to participants,
                    "gameType" to gameType.name,
                    "status" to "active",
                    "currentTurnUid" to opponentTicket.uid,
                    "moveSeq" to 0,
                    "wrappedKeys" to wrappedKeys,
                    "keyVersion" to 1,
                    "encryptedState" to encState,
                    "stateIv" to stateIv,
                    "stateDigest" to stateDigest,
                    "winnerUid" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                opponentTicketRef,
                mapOf(
                    "status" to "matched",
                    "matchId" to matchId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            // Atomically cancel our own searching ticket in the same WriteBatch to prevent dual matches
            if (!myActiveTicketId.isNullOrBlank()) {
                batch.update(
                    db.collection(COLLECTION_TICKETS).document(myActiveTicketId),
                    mapOf(
                        "status" to "cancelled",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            MatchmakingQueueResult.Matched(
                matchId = matchId,
                opponentUid = opponentTicket.uid,
                opponentName = opponentTicket.displayName.ifBlank { opponentTicket.vidyaId },
                opponentVidyaId = opponentTicket.vidyaId,
                opponentElo = opponentTicket.eloRating,
                localPlayerIndex = 1
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Ticket claim race or rule rejection: ${e.message}")
            null
        }
    }

    fun observeTicket(ticketId: String): Flow<MatchmakingTicket?> {
        val docRef = db.collection(COLLECTION_TICKETS).document(ticketId)
        return docRef.snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    MatchmakingTicket::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.copy(ticketId = snap.id)
            }
            .catch { error ->
                val fsEx = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsEx == null && error is CancellationException) throw error
                val exToLog = fsEx ?: (error as? Exception)
                if (exToLog != null) handleFirestoreError(exToLog, OperationType.GET, docRef.path)
            }
    }

    suspend fun cancelTicket(ticketId: String) {
        if (ticketId.isBlank()) return
        val docRef = db.collection(COLLECTION_TICKETS).document(ticketId)
        try {
            docRef.update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            runCatching { docRef.delete().await() }
        }
    }

    suspend fun getMatchOnce(matchId: String): OnlineMatch? {
        if (matchId.isBlank()) return null
        val docRef = db.collection(COLLECTION_ONLINE_MATCHES).document(matchId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) null
            else snap.toObject(
                OnlineMatch::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.copy(matchId = snap.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            null
        }
    }

    fun observeMatch(matchId: String): Flow<OnlineMatch?> {
        val docRef = db.collection(COLLECTION_ONLINE_MATCHES).document(matchId)
        return docRef.snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    OnlineMatch::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.copy(matchId = snap.id)
            }
            .catch { error ->
                val fsEx = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsEx == null && error is CancellationException) throw error
                val exToLog = fsEx ?: (error as? Exception)
                if (exToLog != null) handleFirestoreError(exToLog, OperationType.GET, docRef.path)
            }
    }

    suspend fun submitEncryptedTurn(
        matchId: String,
        nextTurnUid: String,
        nextMoveSeq: Int,
        serializedState: String,
        sessionKey: SecretKey,
        winnerUid: String = ""
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ONLINE_MATCHES).document(matchId)
        return try {
            val (encState, stateIv) = KeystoreCryptoManager.encryptWithSessionKey(serializedState, sessionKey)
            val digest = KeystoreCryptoManager.sha256Digest("$matchId|$nextMoveSeq|$encState")
            val newStatus = if (winnerUid.isNotBlank()) "completed" else "active"

            docRef.update(
                mapOf(
                    "currentTurnUid" to nextTurnUid,
                    "moveSeq" to nextMoveSeq,
                    "encryptedState" to encState,
                    "stateIv" to stateIv,
                    "stateDigest" to digest,
                    "status" to newStatus,
                    "winnerUid" to winnerUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun resolveMatchKey(match: OnlineMatch, uid: String): SecretKey? {
        val wrapped = match.wrappedKeys[uid] ?: return null
        return KeystoreCryptoManager.unwrapSessionKey(uid, wrapped)
    }

    fun verifyAndDecryptMatchState(match: OnlineMatch, sessionKey: SecretKey): String? {
        if (match.encryptedState.isBlank() || match.stateIv.isBlank()) return null
        if (match.stateDigest.isNotBlank()) {
            val expected = KeystoreCryptoManager.sha256Digest("${match.matchId}|${match.moveSeq}|${match.encryptedState}")
            if (expected != match.stateDigest) {
                Log.w(TAG, "Match ${match.matchId} state digest mismatch at moveSeq=${match.moveSeq}")
                return null
            }
        }
        return KeystoreCryptoManager.decryptWithSessionKey(match.encryptedState, match.stateIv, sessionKey)
    }

    companion object {
        private const val TAG = "OnlineMatchRepo"
        const val COLLECTION_TICKETS = "matchmakingTickets"
        const val COLLECTION_ONLINE_MATCHES = "onlineMatches"
        const val DEFAULT_QUEUE_TIMEOUT_MS = 45_000L
        private const val STALE_TICKET_AGE_MS = 120_000L
    }
}
