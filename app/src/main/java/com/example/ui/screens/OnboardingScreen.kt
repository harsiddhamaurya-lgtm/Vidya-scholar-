package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AcademicStream
import com.example.data.BatchType
import com.example.data.RealmId
import com.example.data.StandardGrade
import com.example.data.UserProfile
import com.example.data.repository.VidyaIdFormatter
import com.example.rag.CosmiumVoiceLanguageCatalog
import com.example.ui.components.HybridAvatarStudioContent
import com.example.ui.components.ScholarAvatarBadge
import com.example.ui.components.VidyaIdChip
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.SoundHelper
import com.example.viewmodel.AiTutorViewModel
import com.example.viewmodel.AppThemeMode
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ThemeViewModel
import com.example.viewmodel.VidyaAvailabilityState

private const val TOTAL_ONBOARDING_STEPS = 4

@Composable
fun OnboardingScreen(
    profile: UserProfile,
    isOfflinePreview: Boolean,
    authViewModel: AuthViewModel,
    aiTutorViewModel: AiTutorViewModel,
    themeViewModel: ThemeViewModel,
    currentThemeMode: AppThemeMode
) {
    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val availability by authViewModel.vidyaAvailability.collectAsStateWithLifecycle()
    val provisioningState by aiTutorViewModel.onboardingProvisioning.collectAsStateWithLifecycle()

    var currentStep by remember { mutableIntStateOf(0) }
    var displayName by remember(profile.uid) {
        mutableStateOf(profile.name.ifBlank { "Scholar" })
    }
    var handle by remember(profile.uid) {
        mutableStateOf(
            profile.vidyaHandle.ifBlank {
                VidyaIdFormatter.suggestHandle(profile.name, profile.email)
            }
        )
    }
    var discriminator by remember(profile.uid) {
        mutableStateOf(
            profile.vidyaDiscriminator.ifBlank {
                VidyaIdFormatter.randomDiscriminator()
            }
        )
    }
    var avatarSpec by remember(profile.uid) { mutableStateOf(profile.toAvatarSpec()) }
    var selectedStandard by remember(profile.uid) {
        mutableStateOf(StandardGrade.fromId(profile.standardId))
    }
    var selectedStream by remember(profile.uid, selectedStandard) {
        mutableStateOf(
            if (!selectedStandard.hasStreams) AcademicStream.GENERAL_FOUNDATION
            else AcademicStream.fromId(profile.streamId)
        )
    }
    var selectedLanguageCode by remember(profile.uid) {
        mutableStateOf(profile.preferredLanguage.ifBlank { "en" })
    }
    var languageSearchQuery by remember { mutableStateOf("") }
    var selectedBatch by remember(profile.uid) {
        mutableStateOf(BatchType.fromId(profile.batchType))
    }
    val selectedRealm = remember(profile.uid, selectedStream) {
        when (selectedStream) {
            AcademicStream.SCIENCE_PCM -> RealmId.MATHEMATICS
            AcademicStream.SCIENCE_PCB -> RealmId.CHEMISTRY
            AcademicStream.COMMERCE -> RealmId.MATHEMATICS
            AcademicStream.ARTS_HUMANITIES -> RealmId.CODING_AI
            AcademicStream.GENERAL_FOUNDATION -> runCatching { RealmId.valueOf(profile.focusRealm) }
                .getOrDefault(RealmId.MATHEMATICS)
        }
    }
    var storagePermissionHandled by remember { mutableStateOf(false) }
    var stepError by remember { mutableStateOf<String?>(null) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether granted on Android <= 9 or scoped inside app-private storage, proceed smoothly
        storagePermissionHandled = true
    }

    LaunchedEffect(storagePermissionHandled) {
        if (storagePermissionHandled && !provisioningState.isRunning && !provisioningState.isComplete) {
            aiTutorViewModel.startOnboardingProvisioning(
                standardId = selectedStandard.id,
                streamId = selectedStream.id,
                preferredLanguage = selectedLanguageCode,
                onReady = {
                    authViewModel.completeOnboarding(
                        displayName = displayName,
                        handle = VidyaIdFormatter.normalizeHandle(handle),
                        discriminator = discriminator,
                        avatarSpec = avatarSpec,
                        focusRealm = selectedRealm.name,
                        standardId = selectedStandard.id,
                        streamId = selectedStream.id,
                        batchType = selectedBatch.id,
                        preferredLanguage = selectedLanguageCode,
                        onError = { msg ->
                            storagePermissionHandled = false
                            aiTutorViewModel.resetOnboardingProvisioning()
                            stepError = msg
                        }
                    )
                }
            )
        }
    }

    BackHandler(enabled = currentStep > 0) {
        stepError = null
        if (currentStep == 3 && storagePermissionHandled) {
            aiTutorViewModel.resetOnboardingProvisioning()
            storagePermissionHandled = false
        } else {
            currentStep -= 1
        }
    }

    LaunchedEffect(handle, discriminator) {
        authViewModel.checkVidyaIdAvailability(handle, discriminator)
    }

    val normalizedHandle = VidyaIdFormatter.normalizeHandle(handle)
    val previewVidyaId = VidyaIdFormatter.formatVidyaId(
        normalizedHandle.ifBlank { "scholar" },
        discriminator.padEnd(4, '0').take(4)
    )
    val isIdentityReady = displayName.trim().length >= 2 &&
        VidyaIdFormatter.isValidHandle(normalizedHandle) &&
        VidyaIdFormatter.isValidDiscriminator(discriminator) &&
        availability !is VidyaAvailabilityState.Unavailable

    val selectedLanguageOption = remember(selectedLanguageCode) {
        CosmiumVoiceLanguageCatalog.allLanguages.firstOrNull { it.code == selectedLanguageCode }
            ?: CosmiumVoiceLanguageCatalog.allLanguages.first()
    }

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentSaffron) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.accentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isOfflinePreview) {
                                        "LOCAL PREVIEW • STEP ${currentStep + 1}/$TOTAL_ONBOARDING_STEPS"
                                    } else {
                                        "SCHOLAR ONBOARDING • STEP ${currentStep + 1}/$TOTAL_ONBOARDING_STEPS"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    color = palette.accentSaffron
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (currentStep) {
                                    0 -> stringResource(R.string.onboarding_step_identity)
                                    1 -> stringResource(R.string.onboarding_step_avatar)
                                    2 -> stringResource(R.string.onboarding_step_realm)
                                    else -> stringResource(R.string.onboarding_step_offline_setup)
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        TextButton(
                            onClick = {
                                aiTutorViewModel.resetOnboardingProvisioning()
                                authViewModel.signOut(context)
                            },
                            modifier = Modifier.testTag("onboarding_switch_account_button")
                        ) {
                            Text(
                                text = "Switch Account",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 4-Step Segmented Sacred Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(TOTAL_ONBOARDING_STEPS) { idx ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            idx < currentStep -> palette.accentGold
                                            idx == currentStep -> palette.accentSaffron
                                            else -> palette.elevatedSurface
                                        }
                                    )
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!(currentStep == 3 && storagePermissionHandled)) {
                    IndoToneSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        shape = RoundedCornerShape(20.dp),
                        containerColor = palette.cardSurface,
                        hairlineColor = palette.goldHairline
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = {
                                        SoundHelper.playClick(context)
                                        stepError = null
                                        currentStep -= 1
                                    },
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("onboarding_back_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, palette.goldHairline)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Back", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (currentStep < 3) {
                                Button(
                                    onClick = {
                                        SoundHelper.playWoodyTock(context)
                                        stepError = null
                                        if (currentStep == 0 && !isIdentityReady) {
                                            stepError = "Enter your name and claim a valid unique handle#code first."
                                        } else {
                                            currentStep += 1
                                        }
                                    },
                                    enabled = currentStep != 0 || isIdentityReady,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("onboarding_next_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accentSaffron,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = when (currentStep) {
                                            0 -> "Next: Craft Avatar"
                                            1 -> "Next: Course & Language"
                                            else -> "Next: Offline Setup"
                                        },
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        SoundHelper.playTempleChime(context)
                                        stepError = null
                                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        } else {
                                            storagePermissionHandled = true
                                        }
                                    },
                                    enabled = !isLoading && isIdentityReady,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("onboarding_allow_storage_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accentSaffron,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Allow Storage & Continue", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shared Live Identity Card Preview
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    containerColor = palette.secondaryIndigo,
                    hairlineColor = palette.goldHairline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ScholarAvatarBadge(
                            avatarSpec = avatarSpec,
                            displayName = displayName,
                            size = 64.dp,
                            animateRing = true
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = displayName.ifBlank { "Scholar" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            VidyaIdChip(
                                vidyaId = previewVidyaId,
                                compact = true,
                                showCopyIcon = false
                            )
                            Text(
                                text = "${selectedStandard.shortName} • ${selectedLanguageOption.displayName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (stepError != null) {
                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = palette.accentVermilion.copy(alpha = 0.14f),
                        hairlineColor = palette.accentVermilion.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = palette.accentVermilion)
                            Text(
                                text = stepError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.accentVermilion,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                    },
                    label = "onboarding_step_content"
                ) { step ->
                    when (step) {
                        0 -> {
                            IndoToneSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = palette.cardSurface,
                                hairlineColor = palette.goldHairline
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Your Scholar Name & Vidya Handle",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Friends can find you in Varta chats and challenge you in Khel using your handle#code.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = displayName,
                                        onValueChange = { displayName = it.take(48) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("onboarding_name_input"),
                                        label = { Text("Display Name") },
                                        placeholder = { Text("e.g., Aarav Patel") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = palette.accentSaffron,
                                            unfocusedBorderColor = palette.goldHairline
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = handle,
                                            onValueChange = { raw ->
                                                handle = VidyaIdFormatter.normalizeHandle(raw)
                                            },
                                            modifier = Modifier
                                                .weight(1.4f)
                                                .testTag("onboarding_handle_input"),
                                            label = { Text("Handle") },
                                            prefix = { Text("@", fontWeight = FontWeight.Bold) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = palette.accentSaffron,
                                                unfocusedBorderColor = palette.goldHairline
                                            )
                                        )

                                        OutlinedTextField(
                                            value = discriminator,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }.take(4)
                                                discriminator = digits
                                            },
                                            modifier = Modifier
                                                .weight(0.85f)
                                                .testTag("onboarding_code_input"),
                                            label = { Text("Code") },
                                            prefix = { Text("#", fontWeight = FontWeight.Bold) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = palette.accentSaffron,
                                                unfocusedBorderColor = palette.goldHairline
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            when (val state = availability) {
                                                VidyaAvailabilityState.Idle -> {
                                                    Text(
                                                        text = "3–16 lowercase letters, digits or _",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                VidyaAvailabilityState.Checking -> {
                                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = palette.accentGold, modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = "Checking availability...",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.accentGold
                                                    )
                                                }
                                                is VidyaAvailabilityState.Available -> {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = "${state.formattedId} is available!",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }
                                                is VidyaAvailabilityState.Unavailable -> {
                                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = palette.accentVermilion, modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = state.reason,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.accentVermilion
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                SoundHelper.playClick(context)
                                                discriminator = VidyaIdFormatter.randomDiscriminator()
                                            },
                                            modifier = Modifier.testTag("onboarding_shuffle_code"),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, palette.goldHairline),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Casino, contentDescription = null, tint = palette.accentGold, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Shuffle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            IndoToneSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = palette.cardSurface,
                                hairlineColor = palette.goldHairline
                            ) {
                                HybridAvatarStudioContent(
                                    avatarSpec = avatarSpec,
                                    displayName = displayName,
                                    onSpecChange = { avatarSpec = it },
                                    onPickPhoto = { uri -> authViewModel.savePickedPhotoLocally(context, uri) },
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // 1. STANDARD SELECTION (Class 9 - 12)
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    containerColor = palette.cardSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "1. Select Academic Standard",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StandardGrade.entries.forEach { grade ->
                                                val isSelected = selectedStandard == grade
                                                IndoToneSurface(
                                                    onClick = {
                                                        SoundHelper.playClick(context)
                                                        selectedStandard = grade
                                                        if (!grade.hasStreams) {
                                                            selectedStream = AcademicStream.GENERAL_FOUNDATION
                                                        } else if (selectedStream == AcademicStream.GENERAL_FOUNDATION) {
                                                            selectedStream = AcademicStream.SCIENCE_PCM
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("onboarding_grade_${grade.id}"),
                                                    shape = RoundedCornerShape(14.dp),
                                                    containerColor = if (isSelected) palette.secondaryIndigo else palette.elevatedSurface,
                                                    hairlineColor = if (isSelected) palette.accentGold else palette.goldHairline,
                                                    borderWidth = if (isSelected) 1.6.dp else 1.dp
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = grade.shortName,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 13.sp,
                                                            color = if (isSelected) palette.accentGold else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = if (grade.hasStreams) "Streams" else "Foundation",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. STREAM SELECTION (If Grade 11/12)
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    containerColor = palette.cardSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = if (selectedStandard.hasStreams) "2. Select Stream" else "2. Academic Stream",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (!selectedStandard.hasStreams) {
                                            IndoToneSurface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                containerColor = palette.elevatedSurface,
                                                hairlineColor = palette.goldHairline
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = palette.accentGold, modifier = Modifier.size(20.dp))
                                                    Column {
                                                        Text(
                                                            text = "Integrated Foundation Curriculum",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Maths, Science, Social Studies, English & Coding/AI",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            val applicable = AcademicStream.streamsForStandard(selectedStandard.id)
                                            applicable.forEach { stream ->
                                                val isSelected = selectedStream == stream
                                                IndoToneSurface(
                                                    onClick = {
                                                        SoundHelper.playClick(context)
                                                        selectedStream = stream
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("onboarding_stream_${stream.id}"),
                                                    shape = RoundedCornerShape(14.dp),
                                                    containerColor = if (isSelected) palette.secondaryIndigo else palette.elevatedSurface,
                                                    hairlineColor = if (isSelected) palette.accentGold else palette.goldHairline,
                                                    borderWidth = if (isSelected) 1.6.dp else 1.dp
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Text(
                                                                    text = stream.title,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Surface(
                                                                    shape = RoundedCornerShape(50),
                                                                    color = palette.accentSaffron.copy(alpha = 0.15f),
                                                                    border = BorderStroke(0.7.dp, palette.accentSaffron.copy(alpha = 0.5f))
                                                                ) {
                                                                    Text(
                                                                        text = stream.tagLabel,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = palette.accentSaffron
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                text = stream.description,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (isSelected) {
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = "Selected",
                                                                tint = palette.accentGold,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. PREFERRED LANGUAGE SELECTION
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    containerColor = palette.cardSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Translate,
                                                contentDescription = null,
                                                tint = palette.accentCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "3. Preferred Study & Voice Language",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "Cosmium will prepare voice read-aloud support in your chosen language for seamless offline study.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        OutlinedTextField(
                                            value = languageSearchQuery,
                                            onValueChange = { languageSearchQuery = it.take(24) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("onboarding_language_search"),
                                            placeholder = { Text("Filter languages (e.g. Hindi, Tamil, Bengali...)") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = palette.accentCyan,
                                                unfocusedBorderColor = palette.goldHairline
                                            )
                                        )

                                        val filteredLanguages = remember(languageSearchQuery) {
                                            val q = languageSearchQuery.trim().lowercase()
                                            if (q.isBlank()) CosmiumVoiceLanguageCatalog.allLanguages
                                            else CosmiumVoiceLanguageCatalog.allLanguages.filter {
                                                it.displayName.lowercase().contains(q) ||
                                                    it.nativeScript.lowercase().contains(q) ||
                                                    it.code.lowercase().contains(q)
                                            }
                                        }

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(vertical = 2.dp)
                                        ) {
                                            items(filteredLanguages, key = { it.code }) { lang ->
                                                val isSelected = selectedLanguageCode == lang.code
                                                IndoToneSurface(
                                                    onClick = {
                                                        SoundHelper.playClick(context)
                                                        selectedLanguageCode = lang.code
                                                    },
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("onboarding_lang_${lang.code}"),
                                                    shape = RoundedCornerShape(14.dp),
                                                    containerColor = if (isSelected) palette.secondaryIndigo else palette.elevatedSurface,
                                                    hairlineColor = if (isSelected) palette.accentCyan else palette.goldHairline,
                                                    borderWidth = if (isSelected) 1.6.dp else 1.dp
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = lang.displayName,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) palette.accentCyan else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = lang.nativeScript,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. BATCH MODALITY SELECTION
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    containerColor = palette.cardSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "4. Learning Modality / Batch",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        BatchType.entries.forEach { batch ->
                                            val isSelected = selectedBatch == batch
                                            IndoToneSurface(
                                                onClick = {
                                                    SoundHelper.playClick(context)
                                                    selectedBatch = batch
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("onboarding_batch_${batch.id}"),
                                                shape = RoundedCornerShape(14.dp),
                                                containerColor = if (isSelected) palette.secondaryIndigo else palette.elevatedSurface,
                                                hairlineColor = if (isSelected) palette.accentGold else palette.goldHairline,
                                                borderWidth = if (isSelected) 1.6.dp else 1.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text(
                                                                text = batch.title,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Surface(
                                                                shape = RoundedCornerShape(50),
                                                                color = palette.accentCyan.copy(alpha = 0.15f),
                                                                border = BorderStroke(0.7.dp, palette.accentCyan.copy(alpha = 0.5f))
                                                            ) {
                                                                Text(
                                                                    text = batch.badgeLabel,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = palette.accentCyan
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = batch.description,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Selected",
                                                            tint = palette.accentGold,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    containerColor = palette.cardSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Display & Reading Mode",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val chipColors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = palette.accentSaffron,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = palette.elevatedSurface,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = currentThemeMode == AppThemeMode.LIGHT,
                                                onClick = { themeViewModel.setThemeMode(AppThemeMode.LIGHT) },
                                                label = { Text("Sandstone", fontWeight = FontWeight.SemiBold) },
                                                colors = chipColors,
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = currentThemeMode == AppThemeMode.DARK,
                                                onClick = { themeViewModel.setThemeMode(AppThemeMode.DARK) },
                                                label = { Text("Obsidian", fontWeight = FontWeight.SemiBold) },
                                                colors = chipColors,
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = currentThemeMode == AppThemeMode.EINK,
                                                onClick = { themeViewModel.setThemeMode(AppThemeMode.EINK) },
                                                label = { Text("E-Ink", fontWeight = FontWeight.SemiBold) },
                                                colors = chipColors,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            if (!storagePermissionHandled) {
                                IndoToneSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_storage_permission_card"),
                                    shape = RoundedCornerShape(22.dp),
                                    containerColor = palette.cardSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(48.dp),
                                                shape = CircleShape,
                                                color = palette.accentSaffron.copy(alpha = 0.16f),
                                                border = BorderStroke(1.dp, palette.accentSaffron.copy(alpha = 0.5f))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.FolderOpen,
                                                        contentDescription = null,
                                                        tint = palette.accentSaffron,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Enable Offline Study Storage",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Smooth offline learning on your device",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.accentCyan
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Allow private device storage so Cosmium can keep your personal study guide, textbooks, and voice support ready whenever you study without internet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 21.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        IndoToneSurface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            containerColor = palette.elevatedSurface,
                                            hairlineColor = palette.goldHairline
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = "Prepared automatically for you:",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = palette.accentGold, modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = "Offline AI study guide",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = palette.accentSaffron, modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = "${selectedStandard.shortName} • ${selectedStream.title} textbooks & notes",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = palette.accentCyan, modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = "${selectedLanguageOption.displayName} (${selectedLanguageOption.nativeScript}) voice support",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = provisioningState.progress.coerceIn(0f, 1f),
                                    animationSpec = tween(durationMillis = 320),
                                    label = "onboarding_guide_progress"
                                )
                                val percent = (animatedProgress * 100).toInt().coerceIn(0, 100)

                                IndoToneSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_download_progress_card"),
                                    shape = RoundedCornerShape(22.dp),
                                    containerColor = palette.cardSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(22.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(56.dp),
                                            shape = CircleShape,
                                            color = palette.accentCyan.copy(alpha = 0.14f),
                                            border = BorderStroke(1.2.dp, palette.accentCyan.copy(alpha = 0.5f))
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = palette.accentCyan,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = stringResource(R.string.onboarding_offline_downloading),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { animatedProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(10.dp)
                                                    .clip(CircleShape)
                                                    .testTag("onboarding_download_progress_bar"),
                                                color = palette.accentSaffron,
                                                trackColor = palette.elevatedSurface,
                                                strokeCap = StrokeCap.Round
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${selectedStandard.shortName} • ${selectedLanguageOption.displayName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "$percent%",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = palette.accentGold,
                                                    modifier = Modifier.testTag("onboarding_download_percentage")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
