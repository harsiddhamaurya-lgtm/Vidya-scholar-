package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AvatarMode
import com.example.data.AvatarSpec
import com.example.data.UserProfile
import com.example.data.VidyaIdentityCard
import com.example.data.repository.ScholarIdentityRepository
import com.example.data.repository.VidyaIdFormatter
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface AuthGateState {
    data object Loading : AuthGateState
    data object Unauthenticated : AuthGateState
    data class NeedsOnboarding(
        val profile: UserProfile,
        val isOfflinePreview: Boolean = false
    ) : AuthGateState

    data class Authenticated(
        val profile: UserProfile,
        val isOfflinePreview: Boolean = false
    ) : AuthGateState
}

sealed interface VidyaAvailabilityState {
    data object Idle : VidyaAvailabilityState
    data object Checking : VidyaAvailabilityState
    data class Available(val formattedId: String) : VidyaAvailabilityState
    data class Unavailable(val reason: String) : VidyaAvailabilityState
}

class AuthViewModel(
    private val repository: ScholarIdentityRepository
) : ViewModel() {

    private val auth = Firebase.auth

    private val _authGateState = MutableStateFlow<AuthGateState>(AuthGateState.Loading)
    val authGateState: StateFlow<AuthGateState> = _authGateState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _vidyaAvailability = MutableStateFlow<VidyaAvailabilityState>(VidyaAvailabilityState.Idle)
    val vidyaAvailability: StateFlow<VidyaAvailabilityState> = _vidyaAvailability.asStateFlow()

    private var isOfflinePreviewMode = false
    private var profileObserverJob: Job? = null
    private var availabilityJob: Job? = null
    private var autoSignInAttempted = false

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            isOfflinePreviewMode = false
            syncAuthenticatedUser(
                uid = user.uid,
                defaultName = user.displayName ?: "Scholar",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
        } else if (!isOfflinePreviewMode) {
            stopObservingProfile()
            _userProfile.value = null
            _authGateState.value = AuthGateState.Unauthenticated
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun attemptAutoSignIn(context: Context) {
        if (autoSignInAttempted || auth.currentUser != null || isOfflinePreviewMode) return
        autoSignInAttempted = true

        val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
            ?: return

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)
        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context as? Activity ?: context, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val firebaseCred = GoogleAuthProvider.getCredential(token, null)
                    auth.signInWithCredential(firebaseCred).await()
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Silent Google Sign-In dismissed: ${e.message}")
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto-sign-in unavailable: ${e.message}")
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _errorMessage.value = "Google Sign-In configuration missing."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
        val credentialManager = CredentialManager.create(context)

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context as? Activity ?: context, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    _isLoading.value = false
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Unexpected credential type returned."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _isLoading.value = false
                _errorMessage.value = e.localizedMessage
                    ?: "Google Sign-In could not complete. Try again or explore offline."
            }
        }
    }

    fun enterOfflinePreview(startOnboarding: Boolean = true) {
        isOfflinePreviewMode = true
        _errorMessage.value = null
        val previewProfile = UserProfile(
            uid = "demo_student_123",
            name = "Aarav Scholar",
            email = "local-preview@vidhyascholar.app",
            vidyaId = if (startOnboarding) "" else "aarav#4821",
            vidyaHandle = "aarav",
            vidyaDiscriminator = "4821",
            vidyaKey = "aarav_4821",
            contactCode = if (startOnboarding) "" else "aarav#4821",
            avatarMode = AvatarMode.PRESET.name,
            avatarPresetId = "vedic_strategist",
            avatarCrestId = "yantra",
            avatarFrameId = "astrolabe",
            avatarToneId = "saffron",
            focusRealm = "MATHEMATICS",
            onboardingCompleted = !startOnboarding,
            streak = 7,
            xp = 1450,
            level = "Gold Master",
            hearts = 5,
            completedLessons = listOf("m_1", "p_1")
        )
        _userProfile.value = previewProfile
        _authGateState.value = if (startOnboarding) {
            AuthGateState.NeedsOnboarding(previewProfile, isOfflinePreview = true)
        } else {
            AuthGateState.Authenticated(previewProfile, isOfflinePreview = true)
        }
    }

    private fun syncAuthenticatedUser(
        uid: String,
        defaultName: String,
        email: String,
        photoUrl: String
    ) {
        stopObservingProfile()
        _authGateState.value = AuthGateState.Loading

        viewModelScope.launch {
            val initResult = repository.getOrInitializeUserProfile(uid, defaultName, email, photoUrl)
            initResult.onSuccess { initialProfile ->
                updateGateFromProfile(initialProfile, isOffline = false)
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to load your scholar profile."
            }

            profileObserverJob = viewModelScope.launch {
                repository.observeUserProfile(uid)
                    .catch { error ->
                        Log.w(TAG, "Profile stream error: ${error.message}", error)
                    }
                    .collect { profile ->
                        if (profile != null && !isOfflinePreviewMode) {
                            updateGateFromProfile(profile, isOffline = false)
                        }
                    }
            }
        }
    }

    private fun updateGateFromProfile(profile: UserProfile, isOffline: Boolean) {
        _userProfile.value = profile
        _authGateState.value = if (!profile.onboardingCompleted || profile.vidyaId.isBlank()) {
            AuthGateState.NeedsOnboarding(profile, isOfflinePreview = isOffline)
        } else {
            AuthGateState.Authenticated(profile, isOfflinePreview = isOffline)
        }
    }

    private fun stopObservingProfile() {
        profileObserverJob?.cancel()
        profileObserverJob = null
    }

    fun checkVidyaIdAvailability(rawHandle: String, discriminator: String) {
        availabilityJob?.cancel()
        val normalized = VidyaIdFormatter.normalizeHandle(rawHandle)
        if (!VidyaIdFormatter.isValidHandle(normalized)) {
            _vidyaAvailability.value = VidyaAvailabilityState.Unavailable(
                "Use 3–16 lowercase letters, digits, or underscores."
            )
            return
        }
        if (!VidyaIdFormatter.isValidDiscriminator(discriminator)) {
            _vidyaAvailability.value = VidyaAvailabilityState.Unavailable(
                "4-digit code must be 1000–9999."
            )
            return
        }

        val formatted = VidyaIdFormatter.formatVidyaId(normalized, discriminator)
        if (isOfflinePreviewMode || auth.currentUser == null) {
            val takenLocal = com.example.data.PracticePeersCatalog.findExactByVidyaId(formatted) != null
            _vidyaAvailability.value = if (takenLocal) {
                VidyaAvailabilityState.Unavailable("$formatted is reserved — tap Shuffle.")
            } else {
                VidyaAvailabilityState.Available(formatted)
            }
            return
        }

        _vidyaAvailability.value = VidyaAvailabilityState.Checking
        val currentUid = auth.currentUser?.uid.orEmpty()
        availabilityJob = viewModelScope.launch {
            delay(260)
            val available = repository.isVidyaIdAvailable(normalized, discriminator, currentUid)
            _vidyaAvailability.value = if (available) {
                VidyaAvailabilityState.Available(formatted)
            } else {
                VidyaAvailabilityState.Unavailable("$formatted is already claimed — tap Shuffle.")
            }
        }
    }

    suspend fun savePickedPhotoLocally(context: Context, uri: Uri): String? {
        val uid = auth.currentUser?.uid ?: _userProfile.value?.uid ?: "scholar"
        return ScholarIdentityRepository.savePickedAvatarPhoto(context, uri, uid)
    }

    fun completeOnboarding(
        displayName: String,
        handle: String,
        discriminator: String,
        avatarSpec: AvatarSpec,
        focusRealm: String,
        onError: (String) -> Unit = {}
    ) {
        val current = _userProfile.value ?: return
        val normalizedHandle = VidyaIdFormatter.normalizeHandle(handle)
        if (!VidyaIdFormatter.isValidHandle(normalizedHandle)) {
            onError("Handle must be 3–16 lowercase letters, digits, or underscores.")
            return
        }

        if (isOfflinePreviewMode || auth.currentUser == null) {
            val vidyaId = VidyaIdFormatter.formatVidyaId(normalizedHandle, discriminator)
            val updated = current.copy(
                name = displayName.trim().ifBlank { current.name },
                vidyaId = vidyaId,
                vidyaHandle = normalizedHandle,
                vidyaDiscriminator = discriminator,
                vidyaKey = VidyaIdFormatter.toDocumentKey(normalizedHandle, discriminator),
                contactCode = vidyaId,
                avatarMode = avatarSpec.mode.name,
                avatarPresetId = avatarSpec.presetId,
                avatarCrestId = avatarSpec.crestId,
                avatarFrameId = avatarSpec.frameId,
                avatarToneId = avatarSpec.toneId,
                localAvatarPhotoPath = avatarSpec.localPhotoPath,
                focusRealm = focusRealm,
                onboardingCompleted = true
            )
            updateGateFromProfile(updated, isOffline = true)
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.completeOnboardingAndClaimVidyaId(
                uid = current.uid,
                displayName = displayName,
                email = current.email,
                photoUrl = current.photoUrl,
                handle = normalizedHandle,
                discriminator = discriminator,
                avatarSpec = avatarSpec,
                focusRealm = focusRealm
            )
            _isLoading.value = false
            result.onSuccess { saved ->
                updateGateFromProfile(saved, isOffline = false)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Could not save your Vidya ID. Try shuffling the code."
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    fun updateIdentityCustomization(
        displayName: String,
        avatarSpec: AvatarSpec,
        focusRealm: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val current = _userProfile.value ?: return
        val safeName = displayName.trim().ifBlank { current.name }.take(60)
        if (isOfflinePreviewMode || auth.currentUser == null) {
            val updated = current.copy(
                name = safeName,
                avatarMode = avatarSpec.mode.name,
                avatarPresetId = avatarSpec.presetId,
                avatarCrestId = avatarSpec.crestId,
                avatarFrameId = avatarSpec.frameId,
                avatarToneId = avatarSpec.toneId,
                localAvatarPhotoPath = avatarSpec.localPhotoPath,
                focusRealm = focusRealm
            )
            updateGateFromProfile(updated, isOffline = true)
            onComplete(true, null)
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.updateAvatarAndDisplayName(current, safeName, avatarSpec, focusRealm)
            _isLoading.value = false
            result.onSuccess { updated ->
                updateGateFromProfile(updated, isOffline = false)
                onComplete(true, null)
            }.onFailure { err ->
                onComplete(false, err.localizedMessage ?: "Failed to update identity.")
            }
        }
    }

    suspend fun lookupPeerByVidyaId(query: String): VidyaIdentityCard? {
        if (isOfflinePreviewMode || auth.currentUser == null) {
            return com.example.data.PracticePeersCatalog.findByQuery(query).firstOrNull()
        }
        return repository.lookupScholarByVidyaId(query).getOrNull()
    }

    fun updateProgress(newXp: Int, completedLessonId: String) {
        val current = _userProfile.value ?: return
        val updatedLessons = if (current.completedLessons.contains(completedLessonId)) {
            current.completedLessons
        } else {
            current.completedLessons + completedLessonId
        }
        val newTotalXp = current.xp + newXp
        val newLevel = when {
            newTotalXp > 3000 -> "Platinum Sage"
            newTotalXp > 2000 -> "Gold Master"
            newTotalXp > 1000 -> "Silver Prodigy"
            else -> "Bronze Scholar"
        }
        val updated = current.copy(
            xp = newTotalXp,
            level = newLevel,
            completedLessons = updatedLessons
        )
        _userProfile.value = updated
        if (!isOfflinePreviewMode && auth.currentUser != null && !current.uid.startsWith("demo_")) {
            viewModelScope.launch {
                repository.updateLearningProgress(updated)
            }
        }
    }

    fun signOut(context: Context, onSignOutComplete: () -> Unit = {}) {
        val credentialManager = CredentialManager.create(context)
        isOfflinePreviewMode = false
        stopObservingProfile()
        _errorMessage.value = null

        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    withContext(Dispatchers.Main) {
                        _userProfile.value = null
                        _authGateState.value = AuthGateState.Unauthenticated
                        onSignOutComplete()
                    }
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        stopObservingProfile()
    }

    private companion object {
        const val TAG = "AuthViewModel"
    }
}
