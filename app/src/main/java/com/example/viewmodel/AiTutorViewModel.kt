package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.rag.BrainMode
import com.example.rag.CosmosChatRepository
import com.example.rag.CosmosChatSessionEntity
import com.example.rag.CosmosVoiceQuizManager
import com.example.rag.DownloadTaskState
import com.example.rag.ModelLifecycleState
import com.example.rag.OfflineModelSpec
import com.example.rag.OfflineRAGManager
import com.example.rag.OfflineVaultUiState
import com.example.rag.SyllabusPackCatalog
import com.example.rag.VoiceQuizUiState
import com.example.security.SecurePrefsFactory
import com.example.utils.CosmiumVoiceManager
import com.example.utils.OfflineCosmiumKnowledgeEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val responseText: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

data class OnboardingProvisioningState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val isComplete: Boolean = false
)

class AiTutorViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AiTutorViewModel"
    private val appContext = application.applicationContext
    private val prefs = SecurePrefsFactory.getEncryptedPrefs(appContext, "cosmium_vault_prefs_v1")

    val voiceManager = CosmiumVoiceManager(appContext)
    val ragManager = OfflineRAGManager(appContext)
    val ragEngine: OfflineRAGManager get() = ragManager

    val chatRepo = CosmosChatRepository(appContext)
    val voiceQuizManager = CosmosVoiceQuizManager(appContext, voiceManager, viewModelScope)
    val voiceQuizState: StateFlow<VoiceQuizUiState> = voiceQuizManager.uiState

    val activeSessionId: StateFlow<String> = chatRepo.activeSessionId
    val isSyncingChats: StateFlow<Boolean> = chatRepo.isSyncing

    val chatSessions: StateFlow<List<CosmosChatSessionEntity>> = chatRepo
        .observeUserSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "cosmos_welcome",
                text = "Welcome to the Cosmos! Whether you're untangling Physics equations, debugging Python, or studying offline with your personal study guide—drop your challenge below!",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _hasOlderMessages = MutableStateFlow(false)
    val hasOlderMessages: StateFlow<Boolean> = _hasOlderMessages.asStateFlow()

    private val _isLoadingOlder = MutableStateFlow(false)
    val isLoadingOlder: StateFlow<Boolean> = _isLoadingOlder.asStateFlow()

    private var loadedMessageOffset = 0
    private val MESSAGE_PAGE_SIZE = 20

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _studentScope = MutableStateFlow("class_11" to "science_pcm")
    private val _bannerVisible = MutableStateFlow(false)
    private val _voiceLangCode = MutableStateFlow(prefs.getString("voice_lang_code", "en") ?: "en")

    private val _onboardingProvisioning = MutableStateFlow(OnboardingProvisioningState())
    val onboardingProvisioning: StateFlow<OnboardingProvisioningState> = _onboardingProvisioning.asStateFlow()

    private var provisioningJob: kotlinx.coroutines.Job? = null
    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null

    val vaultUiState: StateFlow<OfflineVaultUiState> = combine(
        _studentScope,
        _isOnline,
        ragEngine.packStatus,
        ragEngine.downloader.miniLmStatus,
        ragEngine.downloader.qwenStatus,
        ragEngine.modelManager.lifecycleState,
        _bannerVisible,
        _voiceLangCode
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val scope = values[0] as Pair<String, String>
        val online = values[1] as Boolean
        val pack = values[2] as com.example.rag.StandardPackStatus
        val miniLm = values[3] as com.example.rag.ModelDownloadStatus
        val qwen = values[4] as com.example.rag.ModelDownloadStatus
        val lifecycle = values[5] as ModelLifecycleState
        val showBanner = values[6] as Boolean
        val voiceLang = values[7] as String

        val (freeRamMb, totalRamMb) = ragEngine.modelManager.getMemoryStatsMb()
        val availStorageMb = ragEngine.downloader.getAvailableStorageMb()

        val activeMode = when {
            online -> BrainMode.ONLINE_GEMINI
            qwen.state == DownloadTaskState.READY && ragEngine.modelManager.canSafelyLoadQwen4Bit() ->
                BrainMode.OFFLINE_QWEN_4BIT_RAG
            else -> BrainMode.OFFLINE_HYBRID_RAG
        }

        OfflineVaultUiState(
            standardId = scope.first,
            streamId = scope.second,
            isOnline = online,
            activeBrainMode = activeMode,
            modelLifecycle = lifecycle,
            freeRamMb = freeRamMb,
            totalRamMb = totalRamMb,
            availableStorageMb = availStorageMb,
            packStatus = pack,
            miniLmStatus = miniLm,
            qwenStatus = qwen,
            showOnboardingVaultBanner = showBanner && (!pack.isInstalled || qwen.state != DownloadTaskState.READY),
            selectedVoiceLanguageCode = voiceLang
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = OfflineVaultUiState()
    )

    init {
        voiceManager.setLanguage(_voiceLangCode.value)
        registerConnectivityObserver()
        refreshVaultStatus()
        initializeDefaultChatSession()
    }

    private fun initializeDefaultChatSession() {
        viewModelScope.launch {
            if (_isOnline.value) {
                runCatching { chatRepo.syncPendingToCloud() }
            }
            val sessions = chatRepo.getUserSessions()
            val initial = sessions.firstOrNull { !it.isDeleted }
            if (initial != null) {
                loadInitialPageForSession(initial.sessionId)
            } else {
                val (std, stream) = _studentScope.value
                val newSess = chatRepo.createNewSession(std, stream, "Cosmos Study Session")
                chatRepo.setActiveSession(newSess.sessionId)
                loadedMessageOffset = 0
                _hasOlderMessages.value = false
            }
        }
    }

    private suspend fun loadInitialPageForSession(sessionId: String) {
        chatRepo.setActiveSession(sessionId)
        val total = chatRepo.getMessageCount(sessionId)
        val pageDesc = chatRepo.getMessagesPaged(sessionId, limit = MESSAGE_PAGE_SIZE, offset = 0)
        val pageAsc = pageDesc.reversed()
        loadedMessageOffset = pageAsc.size
        _hasOlderMessages.value = total > loadedMessageOffset

        if (pageAsc.isNotEmpty()) {
            _messages.value = pageAsc.map { m ->
                ChatMessage(
                    id = m.messageId,
                    text = m.text,
                    isUser = m.isUser,
                    ragSources = m.ragSourcesJson.split("||").filter { it.isNotBlank() }
                )
            }
        } else {
            _messages.value = listOf(
                ChatMessage(
                    id = "cosmos_welcome",
                    text = "Welcome to the Cosmos! Whether you're untangling Physics equations, debugging Python, or studying offline with your personal study guide—drop your challenge below!",
                    isUser = false
                )
            )
        }
    }

    fun loadOlderMessages() {
        if (_isLoadingOlder.value || !_hasOlderMessages.value) return
        val sessionId = chatRepo.activeSessionId.value
        viewModelScope.launch {
            _isLoadingOlder.value = true
            try {
                val total = chatRepo.getMessageCount(sessionId)
                val olderDesc = chatRepo.getMessagesPaged(
                    sessionId = sessionId,
                    limit = MESSAGE_PAGE_SIZE,
                    offset = loadedMessageOffset
                )
                val olderAsc = olderDesc.reversed().map { m ->
                    ChatMessage(
                        id = m.messageId,
                        text = m.text,
                        isUser = m.isUser,
                        ragSources = m.ragSourcesJson.split("||").filter { it.isNotBlank() }
                    )
                }
                if (olderAsc.isNotEmpty()) {
                    val existingIds = _messages.value.map { it.id }.toSet()
                    val uniqueOlder = olderAsc.filter { it.id !in existingIds }
                    _messages.value = uniqueOlder + _messages.value
                    loadedMessageOffset += olderAsc.size
                }
                _hasOlderMessages.value = total > loadedMessageOffset && olderAsc.isNotEmpty()
            } finally {
                _isLoadingOlder.value = false
            }
        }
    }

    fun selectChatSession(sessionId: String) {
        viewModelScope.launch {
            loadInitialPageForSession(sessionId)
        }
    }

    fun startNewChatSession() {
        val (std, stream) = _studentScope.value
        viewModelScope.launch {
            val newSess = chatRepo.createNewSession(std, stream, "Cosmos Study Session")
            chatRepo.setActiveSession(newSess.sessionId)
            loadedMessageOffset = 0
            _hasOlderMessages.value = false
            _messages.value = listOf(
                ChatMessage(
                    id = "cosmos_welcome",
                    text = "Welcome to the Cosmos! Whether you're untangling Physics equations, debugging Python, or studying offline with your personal study guide—drop your challenge below!",
                    isUser = false
                )
            )
        }
    }

    fun togglePinSession(sessionId: String, isPinned: Boolean) {
        viewModelScope.launch {
            chatRepo.setChatPinned(sessionId, isPinned)
            if (_isOnline.value) {
                chatRepo.syncPendingToCloud()
            }
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            val wasActive = chatRepo.activeSessionId.value == sessionId
            chatRepo.deleteSession(sessionId)
            if (wasActive) {
                startNewChatSession()
            }
            if (_isOnline.value) {
                chatRepo.syncPendingToCloud()
            }
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            chatRepo.syncPendingToCloud()
        }
    }

    fun startVoiceQuiz(numQuestions: Int = 5) {
        val (std, stream) = _studentScope.value
        voiceQuizManager.startQuizSession(std, stream, numQuestions)
    }

    fun submitVoiceQuizAnswer(answerText: String) {
        voiceQuizManager.submitAnswer(answerText)
    }

    fun retryVoiceQuizListening() {
        voiceQuizManager.startListeningForAnswer()
    }

    fun nextVoiceQuizQuestion() {
        voiceQuizManager.nextQuestionNow()
    }

    fun stopVoiceQuiz() {
        voiceQuizManager.stopQuiz()
    }

    fun syncStudentScope(standardId: String, streamId: String) {
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        val newScope = standardId to effectiveStream
        if (_studentScope.value != newScope) {
            _studentScope.value = newScope
        }
        viewModelScope.launch {
            ragEngine.refreshPackStatus(standardId, effectiveStream)
        }
    }

    fun refreshVaultStatus() {
        val (std, stream) = _studentScope.value
        ragEngine.downloader.refreshStatuses()
        viewModelScope.launch {
            ragEngine.refreshPackStatus(std, stream)
        }
    }

    fun installStandardPack() {
        val (std, stream) = _studentScope.value
        viewModelScope.launch {
            ragEngine.installOrReindexPack(std, stream)
        }
    }

    fun clearStandardPack() {
        val (std, stream) = _studentScope.value
        viewModelScope.launch {
            ragEngine.clearPackForScope(std, stream)
        }
    }

    fun downloadModel(spec: OfflineModelSpec) {
        viewModelScope.launch {
            val ok = ragEngine.downloader.downloadModel(spec)
            if (ok && spec == OfflineModelSpec.MINILM_ENCODER && vaultUiState.value.packStatus.isInstalled) {
                // Re-index cached textbook vectors with freshly downloaded MiniLM encoder
                val (std, stream) = _studentScope.value
                ragEngine.installOrReindexPack(std, stream)
            }
        }
    }

    fun cancelModelDownload(spec: OfflineModelSpec) {
        ragEngine.downloader.cancelDownload(spec)
    }

    fun deleteModel(spec: OfflineModelSpec) {
        viewModelScope.launch {
            ragEngine.downloader.deleteModel(spec)
        }
    }

    fun unloadModelsFromRam() {
        viewModelScope.launch {
            ragEngine.unloadModelsFromRam()
        }
    }

    fun dismissVaultBanner() {
        _bannerVisible.value = false
        prefs.edit().putBoolean("vault_banner_dismissed", true).apply()
    }

    fun setVoiceLanguage(langCode: String) {
        val clean = langCode.ifBlank { "en" }
        _voiceLangCode.value = clean
        prefs.edit().putString("voice_lang_code", clean).apply()
        voiceManager.setLanguage(clean)
    }

    /**
     * Automatically provisions all offline study components in the background right after
     * onboarding storage permission is granted:
     * 1. Matching Preferred Language TTS setup
     * 2. Matching Standard + Stream syllabus database
     * 3. Core offline AI guide weights inside app-private storage
     */
    fun startOnboardingProvisioning(
        standardId: String,
        streamId: String,
        preferredLanguage: String,
        fetchRemoteWeights: Boolean = true,
        onReady: () -> Unit
    ) {
        provisioningJob?.cancel()
        val effectiveStream = SyllabusPackCatalog.normalizeStreamForStandard(standardId, streamId)
        _studentScope.value = standardId to effectiveStream

        provisioningJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _onboardingProvisioning.value = OnboardingProvisioningState(
                isRunning = true,
                progress = 0.05f,
                isComplete = false
            )

            // 1. Unpack offline LaTeX & KaTeX (KTeX) rendering assets into internal storage
            runCatching { com.example.rag.OfflineLaTeXAssetInstaller.ensureKaTeXAssetsInstalled(appContext) }

            // 2. Configure Preferred Language TTS
            setVoiceLanguage(preferredLanguage)
            runCatching { voiceManager.prepareLanguageForOffline(preferredLanguage) }
            _onboardingProvisioning.value = OnboardingProvisioningState(
                isRunning = true,
                progress = 0.14f,
                isComplete = false
            )

            // 3. Install & index matching Standard + Stream syllabus guide
            val packMonitorJob = launch {
                ragEngine.packStatus.collect { pack ->
                    if (pack.isInstalling) {
                        val mapped = (0.15f + pack.installProgress * 0.25f).coerceIn(0.15f, 0.40f)
                        if (mapped > _onboardingProvisioning.value.progress) {
                            _onboardingProvisioning.value = OnboardingProvisioningState(
                                isRunning = true,
                                progress = mapped,
                                isComplete = false
                            )
                        }
                    }
                }
            }
            runCatching { ragEngine.installOrReindexPack(standardId, effectiveStream) }
            packMonitorJob.cancel()

            _onboardingProvisioning.value = OnboardingProvisioningState(
                isRunning = true,
                progress = 0.42f,
                isComplete = false
            )

            // 3. Download core offline AI weights if connected and not already present
            if (fetchRemoteWeights && checkCurrentConnectivity()) {
                val miniMonitor = launch {
                    ragEngine.downloader.miniLmStatus.collect { st ->
                        if (st.state == DownloadTaskState.DOWNLOADING || st.state == DownloadTaskState.VERIFYING) {
                            val mapped = (0.42f + st.progress * 0.16f).coerceIn(0.42f, 0.58f)
                            if (mapped > _onboardingProvisioning.value.progress) {
                                _onboardingProvisioning.value = OnboardingProvisioningState(
                                    isRunning = true,
                                    progress = mapped,
                                    isComplete = false
                                )
                            }
                        }
                    }
                }
                val miniOk = if (!ragEngine.modelManager.isModelDownloaded(OfflineModelSpec.MINILM_ENCODER)) {
                    runCatching { ragEngine.downloader.downloadModel(OfflineModelSpec.MINILM_ENCODER) }.getOrDefault(false)
                } else {
                    true
                }
                miniMonitor.cancel()

                if (miniOk) {
                    _onboardingProvisioning.value = OnboardingProvisioningState(
                        isRunning = true,
                        progress = 0.60f,
                        isComplete = false
                    )

                    val coreMonitor = launch {
                        ragEngine.downloader.qwenStatus.collect { st ->
                            if (st.state == DownloadTaskState.DOWNLOADING || st.state == DownloadTaskState.VERIFYING) {
                                val mapped = (0.60f + st.progress * 0.34f).coerceIn(0.60f, 0.94f)
                                if (mapped > _onboardingProvisioning.value.progress) {
                                    _onboardingProvisioning.value = OnboardingProvisioningState(
                                        isRunning = true,
                                        progress = mapped,
                                        isComplete = false
                                    )
                                }
                            }
                        }
                    }
                    if (!ragEngine.modelManager.isModelDownloaded(OfflineModelSpec.QWEN_2_5_3B_INT4)) {
                        runCatching { ragEngine.downloader.downloadModel(OfflineModelSpec.QWEN_2_5_3B_INT4) }
                    }
                    coreMonitor.cancel()
                }
            }

            // 4. Silently finalize initialization of syllabus guide, offline engine, and TTS
            val currentProg = _onboardingProvisioning.value.progress.coerceAtLeast(0.65f)
            val steps = listOf(
                maxOf(currentProg, 0.78f),
                0.90f,
                0.97f
            )
            for (stepProg in steps) {
                _onboardingProvisioning.value = OnboardingProvisioningState(
                    isRunning = true,
                    progress = stepProg,
                    isComplete = false
                )
                kotlinx.coroutines.delay(140L)
            }

            runCatching { ragEngine.refreshPackStatus(standardId, effectiveStream) }
            runCatching { voiceManager.prepareLanguageForOffline(preferredLanguage) }

            _onboardingProvisioning.value = OnboardingProvisioningState(
                isRunning = false,
                progress = 1f,
                isComplete = true
            )
            kotlinx.coroutines.delay(220L)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onReady()
            }
        }
    }

    fun resetOnboardingProvisioning() {
        provisioningJob?.cancel()
        ragEngine.downloader.cancelDownload(OfflineModelSpec.MINILM_ENCODER)
        ragEngine.downloader.cancelDownload(OfflineModelSpec.QWEN_2_5_3B_INT4)
        _onboardingProvisioning.value = OnboardingProvisioningState()
    }

    fun speakReply(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stop()
    }

    fun sendMessage(prompt: String, attachedImage: Bitmap? = null) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank() && attachedImage == null) return

        val displayPrompt = if (attachedImage != null && cleanPrompt.isBlank()) {
            "[Attached problem image] Please solve this step by step."
        } else if (attachedImage != null) {
            "[Image attached] $cleanPrompt"
        } else {
            cleanPrompt
        }

        val userMsg = ChatMessage(text = displayPrompt, isUser = true)
        val replyId = java.util.UUID.randomUUID().toString()
        _messages.value = _messages.value + userMsg
        _uiState.value = AiUiState.Loading

        val (stdId, streamId) = _studentScope.value
        val (scopeTitle, _) = SyllabusPackCatalog.getPackMetadata(stdId, streamId)

        viewModelScope.launch {
            val sid = chatRepo.activeSessionId.value
            runCatching {
                chatRepo.saveMessage(sid, userMsg, stdId, streamId)
            }

            val localHits = runCatching {
                ragManager.retrieveRelevantChapters(
                    query = cleanPrompt.ifBlank { scopeTitle },
                    standardId = stdId,
                    streamId = streamId,
                    topK = 2
                )
            }.getOrDefault(emptyList())

            val sourceLabels = localHits.map {
                "${it.chunk.subjectName} Ch.${it.chunk.chapterNumber} • ${it.chunk.topicTitle}"
            }

            if (!_isOnline.value) {
                val offlineReply = OfflineCosmiumKnowledgeEngine.generateRagOfflineAnswer(
                    ragManager = ragManager,
                    query = cleanPrompt.ifBlank { "Summarize key formulas for $scopeTitle" },
                    standardId = stdId,
                    streamId = streamId
                )
                _messages.value = _messages.value + offlineReply
                _uiState.value = AiUiState.Success(offlineReply.text)
                runCatching {
                    chatRepo.saveMessage(sid, offlineReply, stdId, streamId)
                }
                return@launch
            }

            try {
                val ragContextBlock = if (localHits.isNotEmpty()) {
                    "\n\nGrounded Syllabus References ($scopeTitle):\n" +
                        localHits.joinToString("\n") { hit ->
                            "- [${hit.chunk.subjectName} Ch.${hit.chunk.chapterNumber}: ${hit.chunk.topicTitle}] ${hit.chunk.contentText} (Formulas: ${hit.chunk.formulaTags})"
                        }
                } else ""

                val gradeNum = when {
                    stdId.contains("9") -> 9
                    stdId.contains("10") -> 10
                    stdId.contains("12") -> 12
                    else -> 11
                }
                val baseDirective = com.example.utils.NativeCoreBridge.calibrateCosmosPrompt(
                    userQuery = cleanPrompt,
                    standardGrade = gradeNum,
                    streamId = streamId,
                    scopeTitle = scopeTitle
                )
                val systemDirective = "$baseDirective$ragContextBlock"
                val requestContent = content {
                    if (attachedImage != null) {
                        image(attachedImage)
                    }
                    text("$systemDirective\n\nStudent query: ${cleanPrompt.ifBlank { "Explain and solve what is shown in the attached study image step by step." }}")
                }

                val builder = StringBuilder()
                var placeholderAdded = false

                withTimeout(45_000L) {
                    model.generateContentStream(requestContent).collect { chunk ->
                        val part = chunk.text
                        if (!part.isNullOrEmpty()) {
                            builder.append(part)
                            val currentRaw = builder.toString()
                            val updatedText = com.example.utils.NativeCoreBridge.sanitizeAndFormatMath(
                                rawText = currentRaw,
                                standardGrade = gradeNum,
                                streamId = streamId
                            )
                            if (!placeholderAdded) {
                                placeholderAdded = true
                                _messages.value = _messages.value + ChatMessage(
                                    id = replyId,
                                    text = updatedText,
                                    isUser = false,
                                    ragSources = sourceLabels,
                                    brainBadge = null
                                )
                            } else {
                                _messages.value = _messages.value.map { msg ->
                                    if (msg.id == replyId) msg.copy(text = updatedText) else msg
                                }
                            }
                        }
                    }
                }

                val rawFinal = builder.toString().ifBlank {
                    "I couldn't generate an answer right now. Try rephrasing your challenge!"
                }
                val finalText = com.example.utils.NativeCoreBridge.sanitizeAndFormatMath(
                    rawText = rawFinal,
                    standardGrade = gradeNum,
                    streamId = streamId
                )
                val finalAiMsg = ChatMessage(
                    id = replyId,
                    text = finalText,
                    isUser = false,
                    ragSources = sourceLabels,
                    brainBadge = null
                )
                if (!placeholderAdded) {
                    _messages.value = _messages.value + finalAiMsg
                } else {
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == replyId) msg.copy(text = finalText) else msg
                    }
                }
                _uiState.value = AiUiState.Success(finalText)

                runCatching {
                    chatRepo.saveMessage(sid, finalAiMsg, stdId, streamId)
                    if (_isOnline.value) {
                        chatRepo.syncPendingToCloud()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Cloud AI failed or timed out; routing to Offline Dual-Model Brain.", e)
                val offlineReply = OfflineCosmiumKnowledgeEngine.generateRagOfflineAnswer(
                    ragManager = ragManager,
                    query = cleanPrompt.ifBlank { "Explain core concepts for $scopeTitle" },
                    standardId = stdId,
                    streamId = streamId
                )
                _messages.value = _messages.value + offlineReply
                _uiState.value = AiUiState.Success(offlineReply.text)
                runCatching {
                    chatRepo.saveMessage(sid, offlineReply, stdId, streamId)
                }
            }
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun registerConnectivityObserver() {
        runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    viewModelScope.launch {
                        chatRepo.syncPendingToCloud()
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkCurrentConnectivity()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasNet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    _isOnline.value = hasNet
                    if (hasNet) {
                        viewModelScope.launch {
                            chatRepo.syncPendingToCloud()
                        }
                    }
                }
            }
            connectivityCallback = callback
            cm.registerDefaultNetworkCallback(callback)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceQuizManager.release()
        voiceManager.shutdown()
        runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityCallback?.let { cm?.unregisterNetworkCallback(it) }
        }
        ragManager.close()
    }
}
