package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import com.example.ui.components.CosmosChatHistoryDialog
import com.example.ui.components.CosmosVoiceQuizDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ChatMessage
import com.example.data.QuestNodeState
import com.example.data.VidyaRepository
import com.example.ui.components.CosmiumMood
import com.example.ui.components.CosmiumOrb
import com.example.ui.components.CosmiumStatusPill
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.SoundHelper
import com.example.viewmodel.AiTutorViewModel
import com.example.viewmodel.AiUiState
import com.example.viewmodel.VidyaVaultViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class CosmosQuickPrompt(
    val chipLabel: String,
    val promptBuilder: (activeLevelSummary: String) -> String
)

private val COSMOS_QUICK_PROMPTS = listOf(
    CosmosQuickPrompt("🎙️ Voice Quiz on textbook") { _ -> "" },
    CosmosQuickPrompt("⚡ Quiz me on my current level") { active ->
        "Quiz me with 3 interactive challenge questions on my current active level: $active. Wait for my answers or guide me step-by-step!"
    },
    CosmosQuickPrompt("🛡️ Prep me for the 50-mark Boss Battle") { active ->
        "Give me a crisp high-yield Boss Battle strategy checklist and 2 tough synthesis questions for: $active."
    },
    CosmosQuickPrompt("🧠 Explain this concept visually") { active ->
        "Explain the core intuition behind $active using a visual analogy and step-by-step breakdown."
    },
    CosmosQuickPrompt("💻 Debug my code step-by-step") { _ ->
        "Help me debug my code step-by-step! Ask me to paste the snippet or give me a 4-step debugging checklist with a clean Kotlin/Python example."
    }
)

@Composable
fun AiTutorScreen(
    aiViewModel: AiTutorViewModel,
    vidyaViewModel: VidyaVaultViewModel = viewModel(),
    isCosmosActive: Boolean = true,
    studentStandardId: String = "class_11",
    studentStreamId: String = "science_pcm"
) {
    LaunchedEffect(studentStandardId, studentStreamId) {
        aiViewModel.syncStudentScope(studentStandardId, studentStreamId)
    }

    val messages by aiViewModel.messages.collectAsStateWithLifecycle()
    val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val isVoiceSpeaking by aiViewModel.voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val voiceRms by aiViewModel.voiceManager.currentRms.collectAsStateWithLifecycle()
    val completedIds by vidyaViewModel.completedLevelIds.collectAsStateWithLifecycle()

    val chatSessions by aiViewModel.chatSessions.collectAsStateWithLifecycle()
    val activeSessionId by aiViewModel.activeSessionId.collectAsStateWithLifecycle()
    val isSyncingChats by aiViewModel.isSyncingChats.collectAsStateWithLifecycle()
    val isOnline by aiViewModel.isOnline.collectAsStateWithLifecycle()
    val voiceQuizState by aiViewModel.voiceQuizState.collectAsStateWithLifecycle()
    val hasOlderMessages by aiViewModel.hasOlderMessages.collectAsStateWithLifecycle()
    val isLoadingOlder by aiViewModel.isLoadingOlder.collectAsStateWithLifecycle()

    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showVoiceQuizDialog by remember { mutableStateOf(false) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        showVoiceQuizDialog = true
        aiViewModel.startVoiceQuiz()
    }

    val onLaunchVoiceQuiz: () -> Unit = {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasAudio) {
            showVoiceQuizDialog = true
            aiViewModel.startVoiceQuiz()
        } else {
            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedLabel by remember { mutableStateOf<String?>(null) }
    var transientMood by remember { mutableStateOf<CosmiumMood?>(CosmiumMood.SPEAKING) }
    var tapIntensity by remember { mutableStateOf(0f) }

    // Track messages that have already completed their entrance/reveal animation in this session
    val completedAnimIds = remember(activeSessionId) { mutableStateMapOf<String, Boolean>() }
    var sessionInteractiveStarted by remember(activeSessionId) { mutableStateOf(false) }

    // Initial welcome greeting speaking animation (~4.5s)
    LaunchedEffect(Unit) {
        delay(4_500L)
        if (transientMood == CosmiumMood.SPEAKING && uiState !is AiUiState.Loading) {
            transientMood = null
        }
    }

    // Expressive emotion transitions driven by Cosmos streaming/reply/error state
    LaunchedEffect(uiState, messages.lastOrNull()?.text) {
        when (uiState) {
            is AiUiState.Loading -> {
                sessionInteractiveStarted = true
                val lastMsg = messages.lastOrNull()
                if (lastMsg != null && !lastMsg.isUser && lastMsg.id != "cosmos_welcome") {
                    // AI is actively streaming tokens -> speaking cadence
                    transientMood = CosmiumMood.SPEAKING
                } else {
                    transientMood = null
                }
            }
            is AiUiState.Success -> {
                transientMood = CosmiumMood.SPEAKING
                delay(4_200L)
                if (transientMood == CosmiumMood.SPEAKING) {
                    transientMood = null
                }
            }
            is AiUiState.Error -> {
                transientMood = CosmiumMood.ALERT
                delay(3_000L)
                if (transientMood == CosmiumMood.ALERT) {
                    transientMood = null
                }
            }
            AiUiState.Idle -> Unit
        }
    }

    LaunchedEffect(tapIntensity) {
        if (tapIntensity > 0f) {
            delay(1_400L)
            tapIntensity = 0f
            if (transientMood == CosmiumMood.CELEBRATE) {
                transientMood = null
            }
        }
    }

    val activeMood = when {
        isVoiceSpeaking -> CosmiumMood.SPEAKING
        transientMood == CosmiumMood.CELEBRATE -> CosmiumMood.CELEBRATE
        transientMood == CosmiumMood.SPEAKING -> CosmiumMood.SPEAKING
        uiState is AiUiState.Loading -> CosmiumMood.THINKING
        transientMood == CosmiumMood.ALERT -> CosmiumMood.ALERT
        inputText.isNotBlank() -> CosmiumMood.CURIOUS
        else -> CosmiumMood.IDLE
    }

    val typingIntensity = (inputText.length.coerceAtMost(30) / 30f) * 0.45f
    val rmsBoost = if (isVoiceSpeaking) (voiceRms * 0.85f).coerceIn(0.25f, 0.9f) else 0f
    val interactionIntensity = (typingIntensity + tapIntensity + rmsBoost + if (activeMood == CosmiumMood.SPEAKING) 0.4f else 0f)
        .coerceIn(0f, 1.4f)

    // Determine the student's current active level for contextual quick prompts
    val activeLevelSummary = remember(completedIds) {
        val realm = VidyaRepository.realms.firstOrNull { r ->
            r.levels.any { vidyaViewModel.getNodeState(r, it, completedIds) == QuestNodeState.ACTIVE }
        } ?: VidyaRepository.realms.first()
        val level = realm.levels.firstOrNull {
            vidyaViewModel.getNodeState(realm, it, completedIds) == QuestNodeState.ACTIVE
        } ?: realm.levels.first()
        "${realm.realmId.subjectName} — Level ${level.levelNumber}: ${level.title}"
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let { bmp ->
                attachedBitmap = bmp
                attachedLabel = "Study image attached"
                SoundHelper.playClick(context)
            }
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length, uiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentCyan) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ultra-subtle ambient twinkling Constellation / Mandala Star-Chart Watermark
            if (!palette.isEInk) {
                CosmosConstellationWatermark(
                    goldTint = palette.accentGold,
                    cyanTint = palette.accentCyan,
                    animateStars = isCosmosActive,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. THE "COSMOS" REALM HEADER & CELESTIAL ASTROLABE AVATAR STAGE
                CosmosHeaderCard(
                    mood = activeMood,
                    interactionIntensity = interactionIntensity,
                    isCosmosActive = isCosmosActive,
                    onAvatarTap = {
                        SoundHelper.playClick(context)
                        tapIntensity = 0.85f
                        transientMood = CosmiumMood.CELEBRATE
                    },
                    onHistoryClick = {
                        SoundHelper.playClick(context)
                        showHistoryDialog = true
                    },
                    onVoiceQuizClick = {
                        SoundHelper.playClick(context)
                        onLaunchVoiceQuiz()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. CONVERSATIONAL FEED
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    if (hasOlderMessages) {
                        item(key = "load_older_messages") {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                IndoToneSurface(
                                    onClick = { aiViewModel.loadOlderMessages() },
                                    shape = RoundedCornerShape(14.dp),
                                    containerColor = palette.elevatedSurface,
                                    hairlineColor = palette.goldHairline,
                                    modifier = Modifier.testTag("cosmos_load_older_button")
                                ) {
                                    Text(
                                        text = if (isLoadingOlder) "Loading earlier study notes..." else "↑ Load earlier messages",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.accentCyan
                                    )
                                }
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        val shouldAnimateMsg = sessionInteractiveStarted && completedAnimIds[msg.id] != true && !palette.isEInk
                        when {
                            msg.id == "cosmos_welcome" -> CosmosWelcomeSanctuaryCard(
                                greeting = msg.text,
                                mood = activeMood,
                                interactionIntensity = interactionIntensity
                            )
                            msg.isUser -> CosmosUserBubble(
                                text = msg.text,
                                animateIn = shouldAnimateMsg,
                                onAnimationDone = { completedAnimIds[msg.id] = true }
                            )
                            else -> CosmosAiReplyCard(
                                message = msg,
                                animateIn = shouldAnimateMsg,
                                isStreaming = (uiState is AiUiState.Loading && msg.id == messages.lastOrNull()?.id),
                                isVoiceSpeaking = isVoiceSpeaking,
                                onAnimationDone = { completedAnimIds[msg.id] = true },
                                onSpeak = {
                                    if (isVoiceSpeaking) aiViewModel.stopSpeaking() else aiViewModel.speakReply(msg.text)
                                }
                            )
                        }
                    }

                    if (uiState is AiUiState.Loading && messages.lastOrNull()?.isUser == true) {
                        item(key = "cosmos_thinking_indicator") {
                            CosmosThinkingIndicator()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. QUICK-ACTION PROMPT PILLS TRAY
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(COSMOS_QUICK_PROMPTS) { item ->
                        IndoToneSurface(
                            onClick = {
                                SoundHelper.playWoodyTock(context)
                                val built = item.promptBuilder(activeLevelSummary)
                                if (built.isBlank()) {
                                    onLaunchVoiceQuiz()
                                } else {
                                    sessionInteractiveStarted = true
                                    aiViewModel.sendMessage(built)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = palette.elevatedSurface,
                            hairlineColor = palette.goldHairline
                        ) {
                            Text(
                                text = item.chipLabel,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Optional Attachment Preview Pill
                AnimatedVisibility(visible = attachedBitmap != null) {
                    IndoToneSurface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        containerColor = palette.secondaryCopper,
                        hairlineColor = palette.goldHairline
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = palette.accentCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = attachedLabel ?: "Image attached",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    attachedBitmap = null
                                    attachedLabel = null
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove attachment", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. TACTILE COSMOS INPUT BAR WITH PHOTO PICKER + 0.92F SPRING SEND BUTTON
                CosmosInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    hasAttachment = attachedBitmap != null,
                    onAttachClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSendClick = {
                        if (inputText.isNotBlank() || attachedBitmap != null) {
                            SoundHelper.playTempleChime(context)
                            sessionInteractiveStarted = true
                            aiViewModel.sendMessage(inputText, attachedBitmap)
                            inputText = ""
                            attachedBitmap = null
                            attachedLabel = null
                        }
                    }
                )
            }

            if (showHistoryDialog) {
                CosmosChatHistoryDialog(
                    sessions = chatSessions,
                    activeSessionId = activeSessionId,
                    isSyncing = isSyncingChats,
                    isOnline = isOnline,
                    onSelectSession = { sid -> aiViewModel.selectChatSession(sid) },
                    onNewChat = { aiViewModel.startNewChatSession() },
                    onTogglePin = { sid, pinned -> aiViewModel.togglePinSession(sid, pinned) },
                    onDeleteSession = { sid -> aiViewModel.deleteChatSession(sid) },
                    onTriggerSync = { aiViewModel.triggerCloudSync() },
                    onDismiss = { showHistoryDialog = false }
                )
            }

            if (showVoiceQuizDialog && voiceQuizState.step != com.example.rag.VoiceQuizStep.INACTIVE) {
                CosmosVoiceQuizDialog(
                    state = voiceQuizState,
                    onSubmitAnswer = { ans -> aiViewModel.submitVoiceQuizAnswer(ans) },
                    onRetryListening = { aiViewModel.retryVoiceQuizListening() },
                    onNextQuestion = { aiViewModel.nextVoiceQuizQuestion() },
                    onStopQuiz = { aiViewModel.stopVoiceQuiz() },
                    onDismiss = { showVoiceQuizDialog = false }
                )
            }
        }
    }
}

@Composable
private fun CosmosConstellationWatermark(
    goldTint: Color,
    cyanTint: Color,
    animateStars: Boolean = true,
    modifier: Modifier = Modifier
) {
    val starPulse: Float
    if (animateStars) {
        val infiniteTransition = rememberInfiniteTransition(label = "cosmos_constellation")
        val anim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "star_twinkle"
        )
        starPulse = anim
    } else {
        starPulse = 0.5f
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val starGold = goldTint.copy(alpha = 0.038f + 0.028f * starPulse)
        val arcCyan = cyanTint.copy(alpha = 0.032f + 0.025f * (1f - starPulse))
        val stroke = Stroke(width = 1.dp.toPx())

        // Constellation star points across canvas
        val stars = listOf(
            Offset(w * 0.14f, h * 0.22f),
            Offset(w * 0.34f, h * 0.17f),
            Offset(w * 0.56f, h * 0.26f),
            Offset(w * 0.82f, h * 0.20f),
            Offset(w * 0.72f, h * 0.44f),
            Offset(w * 0.28f, h * 0.52f),
            Offset(w * 0.18f, h * 0.72f),
            Offset(w * 0.62f, h * 0.68f),
            Offset(w * 0.86f, h * 0.76f)
        )
        for (i in 0 until stars.lastIndex) {
            drawLine(starGold, stars[i], stars[i + 1], strokeWidth = 0.9.dp.toPx())
        }
        stars.forEachIndexed { idx, pt ->
            val phase = if (idx % 2 == 0) starPulse else (1f - starPulse)
            drawCircle(
                color = if (idx % 2 == 0) starGold else arcCyan,
                radius = (2.2f + 1.1f * phase).dp.toPx(),
                center = pt
            )
        }

        // Large central celestial mandala astrolabe rings
        val center = Offset(w * 0.5f, h * 0.48f)
        drawCircle(arcCyan, radius = w * (0.375f + 0.01f * starPulse), center = center, style = stroke)
        drawCircle(starGold, radius = w * (0.265f + 0.01f * (1f - starPulse)), center = center, style = stroke)
    }
}

@Composable
fun CelestialAstrolabeAvatar(
    size: Dp,
    mood: CosmiumMood = CosmiumMood.IDLE,
    interactionIntensity: Float = 0f,
    useLive3D: Boolean = true,
    onTap: (() -> Unit)? = null
) {
    val palette = LocalIndoPalette.current
    val shouldAnimate = useLive3D && !palette.isEInk
    val isPulsingFast = mood == CosmiumMood.SPEAKING || mood == CosmiumMood.THINKING || mood == CosmiumMood.CELEBRATE

    // Gentle breathing pulse & orbit animation only while live Cosmos 3D stage is active
    val breathScale: Float
    val orbitAngle: Float
    if (shouldAnimate) {
        val infiniteTransition = rememberInfiniteTransition(label = "celestial_astrolabe")
        val animatedBreath by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isPulsingFast) 900 else 2400,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "astrolabe_breath"
        )
        val animatedOrbit by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 22000, easing = LinearEasing)
            ),
            label = "astrolabe_orbit"
        )
        breathScale = animatedBreath
        orbitAngle = animatedOrbit
    } else {
        breathScale = 1f
        orbitAngle = 0f
    }

    val tapModifier = if (onTap != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onTap
        )
    } else Modifier

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                if (shouldAnimate) {
                    scaleX = breathScale
                    scaleY = breathScale
                }
            }
            .then(tapModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension / 2f - 2.dp.toPx()
            val innerR = outerR * 0.78f
            val gold = if (palette.isEInk) palette.accentSaffron else Color(0xFFD4AF37)
            val cyan = if (palette.isEInk) palette.accentCyan else Color(0xFF00D2E6)

            // Soft celestial radial aura
            if (!palette.isEInk) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFFE066).copy(alpha = 0.24f), gold.copy(alpha = 0.08f), Color.Transparent),
                        center = c,
                        radius = outerR
                    ),
                    radius = outerR,
                    center = c
                )
            }

            // Outer Golden-Cyan Astrolabe Ring
            drawCircle(
                brush = Brush.sweepGradient(listOf(gold, Color(0xFFFFE066), cyan, gold), c),
                radius = outerR,
                center = c,
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Sacred geometric inner ring + astrolabe notches
            drawCircle(
                color = gold.copy(alpha = 0.55f),
                radius = innerR,
                center = c,
                style = Stroke(width = 0.9.dp.toPx())
            )

            if (!palette.isEInk) {
                rotate(orbitAngle, pivot = c) {
                    for (i in 0 until 8) {
                        val a = i * PI / 4.0
                        val p1 = Offset(c.x + (innerR * cos(a)).toFloat(), c.y + (innerR * sin(a)).toFloat())
                        val p2 = Offset(c.x + (outerR * cos(a)).toFloat(), c.y + (outerR * sin(a)).toFloat())
                        drawLine(gold.copy(alpha = 0.65f), p1, p2, strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
                    }
                    drawCircle(
                        color = Color(0xFFFFE066),
                        radius = 2.4.dp.toPx(),
                        center = Offset(c.x + outerR, c.y)
                    )
                }
            }
        }

        // Smooth Yellow Lava-Lamp Oil-Blob Cosmium Core inside the Astrolabe!
        Box(
            modifier = Modifier
                .size(size * 0.80f)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (useLive3D && !palette.isEInk) {
                com.example.ui.components.CosmiumNativeView(
                    modifier = Modifier.fillMaxSize(),
                    isActive = useLive3D,
                    state = mood.stateKey,
                    interactionIntensity = interactionIntensity,
                    moodTurbulence = when (mood) {
                        CosmiumMood.SPEAKING -> 0.55f
                        CosmiumMood.THINKING -> 0.65f
                        CosmiumMood.CELEBRATE -> 0.70f
                        CosmiumMood.CURIOUS -> 0.40f
                        CosmiumMood.ALERT -> 0.35f
                        CosmiumMood.IDLE -> 0.15f
                    },
                    colorHex = mood.colorHex
                )
            } else {
                CosmiumOrb(
                    size = size * 0.80f,
                    mood = mood,
                    interactionIntensity = interactionIntensity,
                    onTap = onTap
                )
            }
        }
    }
}

@Composable
private fun CosmosHeaderCard(
    mood: CosmiumMood,
    interactionIntensity: Float,
    isCosmosActive: Boolean = true,
    onAvatarTap: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onVoiceQuizClick: () -> Unit = {}
) {
    val palette = LocalIndoPalette.current
    IndoToneSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cosmos_header"),
        shape = RoundedCornerShape(22.dp),
        containerColor = if (palette.isDark) palette.elevatedSurface else palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CelestialAstrolabeAvatar(
                size = 64.dp,
                mood = mood,
                interactionIntensity = interactionIntensity,
                useLive3D = isCosmosActive,
                onTap = onAvatarTap
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cosmos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CosmiumStatusPill(mood = mood)
                }
                Text(
                    text = when (mood) {
                        CosmiumMood.SPEAKING -> "Cosmium is explaining your concepts..."
                        CosmiumMood.THINKING -> "Cosmium is synthesizing an answer..."
                        CosmiumMood.CURIOUS -> "Cosmium is listening to your question..."
                        CosmiumMood.CELEBRATE -> "Cosmium is glowing with inspiration!"
                        CosmiumMood.ALERT -> "Cosmium is ready whenever you retry."
                        CosmiumMood.IDLE -> "Your Personal AI Study Sanctuary & Doubt Solver"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Voice Quiz Button Pill
                Surface(
                    onClick = onVoiceQuizClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE88D14),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cosmos_voice_quiz_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Start Voice Quiz",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Quiz",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Chat History Button
                IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cosmos_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Cosmos History",
                        tint = palette.accentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CosmosWelcomeSanctuaryCard(
    greeting: String,
    mood: CosmiumMood,
    interactionIntensity: Float
) {
    val palette = LocalIndoPalette.current
    IndoToneSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CelestialAstrolabeAvatar(
                size = 78.dp,
                mood = mood,
                interactionIntensity = interactionIntensity,
                useLive3D = false
            )
            Text(
                text = "COSMIUM COMPANION",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = Color(0xFFE88D14)
            )
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CosmosUserBubble(
    text: String,
    animateIn: Boolean = false,
    onAnimationDone: () -> Unit = {}
) {
    val palette = LocalIndoPalette.current
    val appearProgress = remember { Animatable(if (animateIn) 0f else 1f) }

    LaunchedEffect(animateIn) {
        if (animateIn && appearProgress.value < 1f) {
            appearProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 560,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
            onAnimationDone()
        }
    }

    val progress = appearProgress.value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress
                val s = 0.92f + 0.08f * progress
                scaleX = s
                scaleY = s
                translationY = (1f - progress) * 20.dp.toPx()
            },
        horizontalArrangement = Arrangement.End
    ) {
        IndoToneSurface(
            modifier = Modifier.widthIn(max = 295.dp),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            containerColor = if (palette.isDark) palette.elevatedSurface else palette.secondaryIndigo,
            hairlineColor = palette.goldHairline
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CosmosAiReplyCard(
    message: ChatMessage,
    animateIn: Boolean = false,
    isStreaming: Boolean = false,
    isVoiceSpeaking: Boolean = false,
    onAnimationDone: () -> Unit = {},
    onSpeak: (() -> Unit)? = null
) {
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val fullText = message.text

    // 1. Gentle standard-sized bubble entrance animation
    val cardEnter = remember(message.id) { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(message.id, animateIn) {
        if (animateIn && cardEnter.value < 1f) {
            cardEnter.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 480,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
        }
    }

    // 2. Gradual text reveal so the standard-sized bubble slowly expands as text appears
    var revealedChars by remember(message.id) {
        mutableIntStateOf(if (animateIn && !palette.isEInk) 0 else fullText.length)
    }
    val latestFullText by rememberUpdatedState(fullText)
    val latestIsStreaming by rememberUpdatedState(isStreaming)
    LaunchedEffect(message.id, animateIn) {
        if (!animateIn || palette.isEInk) {
            revealedChars = latestFullText.length
            onAnimationDone()
            return@LaunchedEffect
        }
        // Small calm pause on initial standard-sized card before text begins flowing in
        if (revealedChars == 0) {
            delay(180L)
        }
        while (latestIsStreaming || revealedChars < latestFullText.length) {
            val targetLen = latestFullText.length
            if (revealedChars < targetLen) {
                val remaining = targetLen - revealedChars
                val step = when {
                    revealedChars < 120 -> 2
                    remaining > 450 -> 5
                    else -> 3
                }
                revealedChars = (revealedChars + step).coerceAtMost(targetLen)
            }
            delay(20L)
        }
        onAnimationDone()
    }

    val isStillRevealing = revealedChars < fullText.length
    val displayedText = if (isStillRevealing || isStreaming) fullText.take(revealedChars) else fullText
    val segments = remember(displayedText) { parseCosmosSegments(displayedText) }
    val enterVal = cardEnter.value

    IndoToneSurface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enterVal
                val s = 0.95f + 0.05f * enterVal
                scaleX = s
                scaleY = s
                translationY = (1f - enterVal) * 16.dp.toPx()
            }
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 360,
                    easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
                )
            ),
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isStillRevealing || isStreaming) {
                        Modifier.defaultMinSize(minHeight = 92.dp)
                    } else {
                        Modifier
                    }
                )
                .height(IntrinsicSize.Min)
        ) {
            // Vibrant Saffron Amber (#E88D14) Left Accent Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isStillRevealing || isStreaming) {
                    // Render smooth 60fps text during reveal/streaming to expand card cleanly without WebView flicker
                    Text(
                        text = if (displayedText.isEmpty()) "…" else displayedText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    segments.forEach { seg ->
                        when (seg) {
                            is CosmosSegment.CodeOrFormula -> {
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = if (palette.isEInk) palette.elevatedSurface else palette.highlightSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Text(
                                        text = seg.content,
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .padding(12.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = when {
                                            palette.isEInk -> MaterialTheme.colorScheme.onSurface
                                            palette.isDark -> Color(0xFF7CE8FF)
                                            else -> palette.accentCyan
                                        }
                                    )
                                }
                            }
                            is CosmosSegment.Prose -> {
                                com.example.ui.components.CosmosMathProseView(
                                    text = seg.content,
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !isStillRevealing && !isStreaming && message.ragSources.isNotEmpty(),
                    enter = fadeIn(tween(320)) + expandVertically(tween(320))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.ragSources.forEach { src ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.elevatedSurface,
                                border = BorderStroke(0.8.dp, palette.goldHairline)
                            ) {
                                Text(
                                    text = src,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (onSpeak != null && !isStillRevealing && !isStreaming) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                SoundHelper.playClick(context)
                                onSpeak()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cosmos_listen_button"),
                            shape = RoundedCornerShape(10.dp),
                            color = palette.elevatedSurface,
                            border = BorderStroke(1.dp, palette.goldHairline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVoiceSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isVoiceSpeaking) "Stop Cosmium voice" else "Listen to Cosmium",
                                    tint = palette.accentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isVoiceSpeaking) "Stop" else "Listen",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class CosmosSegment {
    data class Prose(val content: String) : CosmosSegment()
    data class CodeOrFormula(val content: String) : CosmosSegment()
}

private fun parseCosmosSegments(raw: String): List<CosmosSegment> {
    val result = mutableListOf<CosmosSegment>()
    val regex = Regex("```(?:[a-zA-Z0-9_+-]*\\n)?([\\s\\S]*?)```")
    var cursor = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > cursor) {
            val prose = raw.substring(cursor, match.range.first).trim()
            if (prose.isNotEmpty()) result += CosmosSegment.Prose(prose)
        }
        val code = match.groupValues[1].trim()
        if (code.isNotEmpty()) result += CosmosSegment.CodeOrFormula(code)
        cursor = match.range.last + 1
    }
    if (cursor < raw.length) {
        val tail = raw.substring(cursor).trim()
        if (tail.isNotEmpty()) result += CosmosSegment.Prose(tail)
    }
    if (result.isEmpty()) result += CosmosSegment.Prose(raw)
    return result
}

@Composable
private fun CosmosThinkingIndicator() {
    val palette = LocalIndoPalette.current
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    val breathAlpha: Float
    if (!palette.isEInk) {
        val infiniteTransition = rememberInfiniteTransition(label = "cosmos_thinking_breath")
        val anim by infiniteTransition.animateFloat(
            initialValue = 0.62f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "thinking_text_pulse"
        )
        breathAlpha = anim
    } else {
        breathAlpha = 1f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enterAlpha.value
                translationY = (1f - enterAlpha.value) * 10.dp.toPx()
            },
        horizontalArrangement = Arrangement.Start
    ) {
        IndoToneSurface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .defaultMinSize(minHeight = 68.dp),
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            containerColor = palette.cardSurface,
            hairlineColor = palette.goldHairline
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CosmiumOrb(
                    size = 28.dp,
                    mood = CosmiumMood.THINKING
                )
                Text(
                    text = "Cosmium is weaving an explanation...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = breathAlpha)
                )
            }
        }
    }
}

@Composable
private fun CosmosInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    hasAttachment: Boolean,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val sendInteraction = remember { MutableInteractionSource() }
    val sendPressed by sendInteraction.collectIsPressedAsState()
    val sendScale by animateFloatAsState(
        targetValue = if (sendPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.65f),
        label = "cosmos_send_scale"
    )

    IndoToneSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        containerColor = palette.elevatedSurface,
        hairlineColor = palette.goldHairline
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier
                    .size(44.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("cosmos_attach_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach Study Image",
                    tint = if (hasAttachment) Color(0xFFE88D14) else palette.accentCyan
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cosmos_input_field"),
                placeholder = {
                    Text(
                        text = "Ask anything in the Cosmos...",
                        fontSize = 13.sp
                    )
                },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .minimumInteractiveComponentSize()
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .clip(CircleShape)
                    .background(if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14))
                    .border(1.dp, Color(0xFFFFD166), CircleShape)
                    .clickable(
                        interactionSource = sendInteraction,
                        indication = null,
                        onClick = onSendClick
                    )
                    .testTag("cosmos_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send to Cosmos",
                    tint = if (palette.isEInk) MaterialTheme.colorScheme.onPrimary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
