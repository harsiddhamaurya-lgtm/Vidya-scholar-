package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.RealmId
import com.example.data.UserProfile
import com.example.data.repository.VidyaIdFormatter
import com.example.ui.components.HybridAvatarStudioContent
import com.example.ui.components.ScholarAvatarBadge
import com.example.ui.components.VidyaIdChip
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.SoundHelper
import com.example.viewmodel.AppThemeMode
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ThemeViewModel
import com.example.viewmodel.VidyaAvailabilityState

@Composable
fun OnboardingScreen(
    profile: UserProfile,
    isOfflinePreview: Boolean,
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    currentThemeMode: AppThemeMode
) {
    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    val isLoading by authViewModel.isLoading.collectAsState()
    val availability by authViewModel.vidyaAvailability.collectAsState()

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
    var selectedRealm by remember(profile.uid) {
        mutableStateOf(
            runCatching { RealmId.valueOf(profile.focusRealm) }
                .getOrDefault(RealmId.MATHEMATICS)
        )
    }
    var stepError by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = currentStep > 0) {
        stepError = null
        currentStep -= 1
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
                                    text = if (isOfflinePreview) "LOCAL PREVIEW • STEP ${currentStep + 1}/3" else "SCHOLAR ONBOARDING • STEP ${currentStep + 1}/3",
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
                                    else -> stringResource(R.string.onboarding_step_realm)
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        TextButton(
                            onClick = { authViewModel.signOut(context) },
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

                    // 3-Step Segmented Sacred Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { idx ->
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

                        if (currentStep < 2) {
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
                                    text = if (currentStep == 0) "Next: Craft Avatar" else "Next: Focus Realm",
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
                                    authViewModel.completeOnboarding(
                                        displayName = displayName,
                                        handle = normalizedHandle,
                                        discriminator = discriminator,
                                        avatarSpec = avatarSpec,
                                        focusRealm = selectedRealm.name,
                                        onError = { msg -> stepError = msg }
                                    )
                                },
                                enabled = !isLoading && isIdentityReady,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("onboarding_complete_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accentSaffron,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text("Enter Sanctuary →", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
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
                                text = "One identity across Gyan, Varta Chats & Khel Arena",
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
                                        // Live Availability Indicator
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

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                            text = "Select Your Primary Focus Realm",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        RealmId.entries.forEach { realm ->
                                            val isSelected = selectedRealm == realm
                                            IndoToneSurface(
                                                onClick = {
                                                    SoundHelper.playClick(context)
                                                    selectedRealm = realm
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("onboarding_realm_${realm.name.lowercase()}"),
                                                shape = RoundedCornerShape(16.dp),
                                                containerColor = if (isSelected) palette.secondaryIndigo else palette.elevatedSurface,
                                                hairlineColor = if (isSelected) palette.accentGold else palette.goldHairline,
                                                borderWidth = if (isSelected) 1.6.dp else 1.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = realm.subjectName,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = realm.subtitle,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = palette.accentSaffron
                                                        ) {
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = "Selected",
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.padding(4.dp).size(18.dp)
                                                            )
                                                        }
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
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
