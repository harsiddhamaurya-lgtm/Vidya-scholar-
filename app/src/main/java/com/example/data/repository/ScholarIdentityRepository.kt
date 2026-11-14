package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.AvatarSpec
import com.example.data.OperationType
import com.example.data.PracticePeersCatalog
import com.example.data.UserProfile
import com.example.data.VidyaIdentityCard
import com.example.data.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VidyaIdFormatter {
    private val handleRegex = Regex("^[a-z0-9_]{3,16}$")
    private val codeRegex = Regex("^[0-9]{4}$")

    fun normalizeHandle(raw: String): String {
        val cleaned = raw.trim()
            .removePrefix("@")
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
        return cleaned.take(16)
    }

    fun suggestHandle(displayName: String?, email: String?): String {
        val fromName = displayName
            ?.trim()
            ?.substringBefore(" ")
            ?.let(::normalizeHandle)
            .orEmpty()
        if (fromName.length >= 3) return fromName

        val fromEmail = email
            ?.substringBefore("@")
            ?.let(::normalizeHandle)
            .orEmpty()
        if (fromEmail.length >= 3) return fromEmail

        return "scholar"
    }

    fun randomDiscriminator(): String = (1000..9999).random().toString()

    fun isValidHandle(handle: String): Boolean = handleRegex.matches(handle)

    fun isValidDiscriminator(code: String): Boolean = codeRegex.matches(code)

    fun formatVidyaId(handle: String, discriminator: String): String {
        return "${normalizeHandle(handle)}#$discriminator"
    }

    fun toDocumentKey(handle: String, discriminator: String): String {
        return "${normalizeHandle(handle)}_$discriminator"
    }

    fun parseVidyaId(raw: String): Pair<String, String>? {
        val trimmed = raw.trim().removePrefix("@")
        val parts = trimmed.split("#")
        if (parts.size != 2) return null
        val h = normalizeHandle(parts[0])
        val d = parts[1].trim()
        if (!isValidHandle(h) || !isValidDiscriminator(d)) return null
        return h to d
    }
}

class ScholarIdentityRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return docRef.snapshots(MetadataChanges.EXCLUDE)
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun getOrInitializeUserProfile(
        uid: String,
        defaultName: String,
        email: String,
        photoUrl: String
    ): Result<UserProfile> {
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    return Result.success(existing)
                }
            }

            val safeName = defaultName.trim().ifBlank { "Scholar" }.take(60)
            val payload = mapOf(
                "uid" to uid,
                "name" to safeName,
                "email" to email.take(120),
                "photoUrl" to photoUrl.take(500),
                "vidyaId" to "",
                "vidyaHandle" to "",
                "vidyaDiscriminator" to "",
                "vidyaKey" to "",
                "contactCode" to "",
                "avatarMode" to "PRESET",
                "avatarPresetId" to "vedic_strategist",
                "avatarCrestId" to "yantra",
                "avatarFrameId" to "astrolabe",
                "avatarToneId" to "saffron",
                "localAvatarPhotoPath" to "",
                "focusRealm" to "MATHEMATICS",
                "onboardingCompleted" to false,
                "streak" to 1,
                "xp" to 100,
                "level" to "Bronze Scholar",
                "hearts" to 5,
                "completedLessons" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = UserProfile(
                uid = uid,
                name = safeName,
                email = email.take(120),
                photoUrl = photoUrl.take(500),
                onboardingCompleted = false,
                streak = 1,
                xp = 100,
                level = "Bronze Scholar",
                hearts = 5
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun isVidyaIdAvailable(
        handle: String,
        discriminator: String,
        currentUid: String
    ): Boolean {
        val normalized = VidyaIdFormatter.normalizeHandle(handle)
        if (!VidyaIdFormatter.isValidHandle(normalized) || !VidyaIdFormatter.isValidDiscriminator(discriminator)) {
            return false
        }
        val key = VidyaIdFormatter.toDocumentKey(normalized, discriminator)
        if (PracticePeersCatalog.findExactByVidyaId("$normalized#$discriminator") != null) {
            return false
        }
        val docRef = db.collection(COLLECTION_VIDYA_IDS).document(key)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                true
            } else {
                snap.getString("uid") == currentUid
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            false
        }
    }

    suspend fun completeOnboardingAndClaimVidyaId(
        uid: String,
        displayName: String,
        email: String,
        photoUrl: String,
        handle: String,
        discriminator: String,
        avatarSpec: AvatarSpec,
        focusRealm: String
    ): Result<UserProfile> {
        val normalizedHandle = VidyaIdFormatter.normalizeHandle(handle)
        if (!VidyaIdFormatter.isValidHandle(normalizedHandle)) {
            return Result.failure(IllegalArgumentException("Handle must be 3-16 lowercase letters, numbers, or underscores."))
        }
        if (!VidyaIdFormatter.isValidDiscriminator(discriminator)) {
            return Result.failure(IllegalArgumentException("Code must be a 4-digit number."))
        }
        if (PracticePeersCatalog.findExactByVidyaId("$normalizedHandle#$discriminator") != null) {
            return Result.failure(IllegalStateException("That Vidya ID is already reserved."))
        }

        val safeName = displayName.trim().ifBlank { "Scholar" }.take(60)
        val vidyaId = VidyaIdFormatter.formatVidyaId(normalizedHandle, discriminator)
        val newKey = VidyaIdFormatter.toDocumentKey(normalizedHandle, discriminator)

        val userRef = db.collection(COLLECTION_USERS).document(uid)
        val newCardRef = db.collection(COLLECTION_VIDYA_IDS).document(newKey)

        return try {
            val savedProfile = db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val newCardSnap = tx.get(newCardRef)

                if (newCardSnap.exists() && newCardSnap.getString("uid") != uid) {
                    throw IllegalStateException("Vidya ID $vidyaId is already claimed by another scholar.")
                }

                val previousKey = userSnap.getString("vidyaKey").orEmpty()
                if (previousKey.isNotBlank() && previousKey != newKey) {
                    val oldCardRef = db.collection(COLLECTION_VIDYA_IDS).document(previousKey)
                    val oldCardSnap = tx.get(oldCardRef)
                    if (oldCardSnap.exists() && oldCardSnap.getString("uid") == uid) {
                        tx.delete(oldCardRef)
                    }
                }

                val currentLevel = userSnap.getString("level").orEmpty().ifBlank { "Bronze Scholar" }
                val currentXp = (userSnap.getLong("xp") ?: 100L).toInt()
                val currentStreak = (userSnap.getLong("streak") ?: 1L).toInt()
                val currentHearts = (userSnap.getLong("hearts") ?: 5L).toInt()
                val currentLessons = (userSnap.get("completedLessons") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                val cardPayload = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "vidyaId" to vidyaId,
                    "vidyaHandle" to normalizedHandle,
                    "vidyaDiscriminator" to discriminator,
                    "displayName" to safeName,
                    "level" to currentLevel,
                    "avatarMode" to avatarSpec.mode.name,
                    "avatarPresetId" to avatarSpec.presetId,
                    "avatarCrestId" to avatarSpec.crestId,
                    "avatarFrameId" to avatarSpec.frameId,
                    "avatarToneId" to avatarSpec.toneId,
                    "focusRealm" to focusRealm,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!newCardSnap.exists()) {
                    cardPayload["createdAt"] = FieldValue.serverTimestamp()
                }
                if (newCardSnap.exists()) {
                    tx.set(newCardRef, cardPayload, SetOptions.merge())
                } else {
                    tx.set(newCardRef, cardPayload)
                }

                val userPayload = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "name" to safeName,
                    "photoUrl" to photoUrl.take(500),
                    "vidyaId" to vidyaId,
                    "vidyaHandle" to normalizedHandle,
                    "vidyaDiscriminator" to discriminator,
                    "vidyaKey" to newKey,
                    "contactCode" to vidyaId,
                    "avatarMode" to avatarSpec.mode.name,
                    "avatarPresetId" to avatarSpec.presetId,
                    "avatarCrestId" to avatarSpec.crestId,
                    "avatarFrameId" to avatarSpec.frameId,
                    "avatarToneId" to avatarSpec.toneId,
                    "localAvatarPhotoPath" to avatarSpec.localPhotoPath.take(400),
                    "focusRealm" to focusRealm,
                    "onboardingCompleted" to true,
                    "streak" to currentStreak,
                    "xp" to currentXp,
                    "level" to currentLevel,
                    "hearts" to currentHearts,
                    "completedLessons" to currentLessons,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!userSnap.exists()) {
                    userPayload["email"] = email.take(120)
                    userPayload["createdAt"] = FieldValue.serverTimestamp()
                    tx.set(userRef, userPayload)
                } else {
                    tx.set(userRef, userPayload, SetOptions.merge())
                }

                UserProfile(
                    uid = uid,
                    name = safeName,
                    email = email,
                    photoUrl = photoUrl,
                    vidyaId = vidyaId,
                    vidyaHandle = normalizedHandle,
                    vidyaDiscriminator = discriminator,
                    vidyaKey = newKey,
                    contactCode = vidyaId,
                    avatarMode = avatarSpec.mode.name,
                    avatarPresetId = avatarSpec.presetId,
                    avatarCrestId = avatarSpec.crestId,
                    avatarFrameId = avatarSpec.frameId,
                    avatarToneId = avatarSpec.toneId,
                    localAvatarPhotoPath = avatarSpec.localPhotoPath,
                    focusRealm = focusRealm,
                    onboardingCompleted = true,
                    streak = currentStreak,
                    xp = currentXp,
                    level = currentLevel,
                    hearts = currentHearts,
                    completedLessons = currentLessons
                )
            }.await()

            Result.success(savedProfile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAvatarAndDisplayName(
        profile: UserProfile,
        newName: String,
        newAvatarSpec: AvatarSpec,
        newFocusRealm: String
    ): Result<UserProfile> {
        val safeName = newName.trim().ifBlank { profile.name }.take(60)
        val userRef = db.collection(COLLECTION_USERS).document(profile.uid)

        return try {
            db.runTransaction { tx ->
                val userUpdate = mapOf(
                    "name" to safeName,
                    "avatarMode" to newAvatarSpec.mode.name,
                    "avatarPresetId" to newAvatarSpec.presetId,
                    "avatarCrestId" to newAvatarSpec.crestId,
                    "avatarFrameId" to newAvatarSpec.frameId,
                    "avatarToneId" to newAvatarSpec.toneId,
                    "localAvatarPhotoPath" to newAvatarSpec.localPhotoPath.take(400),
                    "focusRealm" to newFocusRealm,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(userRef, userUpdate, SetOptions.merge())

                if (profile.vidyaKey.isNotBlank()) {
                    val cardRef = db.collection(COLLECTION_VIDYA_IDS).document(profile.vidyaKey)
                    val cardSnap = tx.get(cardRef)
                    if (cardSnap.exists() && cardSnap.getString("uid") == profile.uid) {
                        tx.update(
                            cardRef,
                            mapOf(
                                "displayName" to safeName,
                                "avatarMode" to newAvatarSpec.mode.name,
                                "avatarPresetId" to newAvatarSpec.presetId,
                                "avatarCrestId" to newAvatarSpec.crestId,
                                "avatarFrameId" to newAvatarSpec.frameId,
                                "avatarToneId" to newAvatarSpec.toneId,
                                "focusRealm" to newFocusRealm,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }.await()

            Result.success(
                profile.copy(
                    name = safeName,
                    avatarMode = newAvatarSpec.mode.name,
                    avatarPresetId = newAvatarSpec.presetId,
                    avatarCrestId = newAvatarSpec.crestId,
                    avatarFrameId = newAvatarSpec.frameId,
                    avatarToneId = newAvatarSpec.toneId,
                    localAvatarPhotoPath = newAvatarSpec.localPhotoPath,
                    focusRealm = newFocusRealm
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateLearningProgress(profile: UserProfile): Result<Unit> {
        val userRef = db.collection(COLLECTION_USERS).document(profile.uid)
        return try {
            userRef.update(
                mapOf(
                    "xp" to profile.xp,
                    "level" to profile.level,
                    "completedLessons" to profile.completedLessons,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun lookupScholarByVidyaId(rawQuery: String): Result<VidyaIdentityCard?> {
        PracticePeersCatalog.findExactByVidyaId(rawQuery)?.let {
            return Result.success(it)
        }
        val parsed = VidyaIdFormatter.parseVidyaId(rawQuery)
            ?: return Result.success(PracticePeersCatalog.findByQuery(rawQuery).firstOrNull())

        val key = VidyaIdFormatter.toDocumentKey(parsed.first, parsed.second)
        val docRef = db.collection(COLLECTION_VIDYA_IDS).document(key)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val card = snap.toObject(
                    VidyaIdentityCard::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(card)
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ScholarIdentityRepo"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_VIDYA_IDS = "vidyaIds"

        suspend fun savePickedAvatarPhoto(context: Context, uri: Uri, uid: String): String? =
            withContext(Dispatchers.IO) {
                try {
                    val dir = File(context.filesDir, "avatars").apply { mkdirs() }
                    val safeUid = uid.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "scholar" }
                    val outFile = File(dir, "avatar_${safeUid}_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@withContext null
                    outFile.absolutePath
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist picked avatar photo", e)
                    null
                }
            }
    }
}
