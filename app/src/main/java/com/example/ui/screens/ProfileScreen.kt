package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.PracticePeersCatalog
import com.example.data.RealmId
import com.example.data.UserProfile
import com.example.data.repository.VidyaIdFormatter
import com.example.ui.components.HybridAvatarStudioDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    isOfflinePreview: Boolean,
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    currentThemeMode: AppThemeMode,
    onBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    val profile = userProfile ?: UserProfile()
    val avatarSpec = profile.toAvatarSpec()
    val focusRealm = runCatching { RealmId.valueOf(profile.focusRealm) }.getOrDefault(RealmId.MATHEMATICS)

    var showAvatarStudio by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showChangeVidyaDialog by remember { mutableStateOf(false) }
    var showPracticeRosterDialog by remember { mutableStateOf(false) }

    if (showAvatarStudio) {
        HybridAvatarStudioDialog(
            initialSpec = avatarSpec,
            displayName = profile.name,
            onPickPhoto = { uri -> authViewModel.savePickedPhotoLocally(context, uri) },
            onDismiss = { showAvatarStudio = false },
            onSave = { newSpec ->
                authViewModel.updateIdentityCustomization(
                    displayName = profile.name,
                    avatarSpec = newSpec,
                    focusRealm = profile.focusRealm
                ) { ok, err ->
                    if (ok) {
                        showAvatarStudio = false
                        Toast.makeText(context, "Avatar & frame updated across all realms", Toast.LENGTH_SHORT).show()
                    } else if (err != null) {
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showEditNameDialog) {
        var draftName by remember(profile.name) { mutableStateOf(profile.name) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Scholar Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it.take(48) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.updateIdentityCustomization(
                            displayName = draftName,
                            avatarSpec = avatarSpec,
                            focusRealm = profile.focusRealm
                        ) { ok, err ->
                            if (ok) {
                                showEditNameDialog = false
                            } else if (err != null) {
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentSaffron)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showChangeVidyaDialog) {
        ChangeVidyaIdDialog(
            profile = profile,
            authViewModel = authViewModel,
            onDismiss = { showChangeVidyaDialog = false }
        )
    }

    if (showPracticeRosterDialog) {
        PracticeScholarsDialog(onDismiss = { showPracticeRosterDialog = false })
    }

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentGold) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Scholar Identity Dossier",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isOfflinePreview) {
                    IndoToneSurface(
                        onClick = { authViewModel.signOut(context, onSignedOut) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = palette.secondaryTerracotta,
                        hairlineColor = palette.accentSaffron.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = palette.accentSaffron)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Local Preview Active",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap to connect your Google Account and claim your global Vidya ID.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.accentGold)
                        }
                    }
                }

                // Hero Scholar Identity Card
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    containerColor = palette.cardSurface,
                    hairlineColor = palette.goldHairline
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScholarAvatarBadge(
                            avatarSpec = avatarSpec,
                            displayName = profile.name,
                            size = 96.dp,
                            animateRing = true
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.name.ifBlank { "Scholar" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showEditNameDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("profile_edit_name_button")
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = palette.accentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (profile.email.isNotBlank()) {
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VidyaIdChip(vidyaId = profile.effectiveVidyaId())
                            OutlinedButton(
                                onClick = { showChangeVidyaDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.goldHairline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("profile_change_vidya_id_button")
                            ) {
                                Text("Change ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                SoundHelper.playClick(context)
                                showAvatarStudio = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_customize_avatar_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accentSaffron,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_customize_avatar),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Cross-Realm Identity Preview (Varta & Khel)
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = palette.cardSurface,
                    hairlineColor = palette.goldHairline
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Cross-Realm Identity Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Varta Preview
                            IndoToneSurface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = palette.elevatedSurface,
                                hairlineColor = palette.goldHairline
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ScholarAvatarBadge(
                                        avatarSpec = avatarSpec,
                                        displayName = profile.name,
                                        size = 36.dp,
                                        isOnline = true
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "VARTA CHAT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = palette.accentCyan
                                        )
                                        Text(
                                            text = profile.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = profile.effectiveVidyaId(),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Khel Preview
                            IndoToneSurface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = palette.elevatedSurface,
                                hairlineColor = palette.goldHairline
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ScholarAvatarBadge(
                                        avatarSpec = avatarSpec,
                                        displayName = profile.name,
                                        size = 36.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "KHEL ARENA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = palette.accentGold
                                        )
                                        Text(
                                            text = profile.level,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = focusRealm.subjectName,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Display & Reading Mode + Focus Realm Card
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = palette.cardSurface,
                    hairlineColor = palette.goldHairline
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Focus Realm",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.secondaryIndigo
                            ) {
                                Text(
                                    text = focusRealm.subjectName,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentGold
                                )
                            }
                        }

                        HorizontalDivider(color = palette.goldHairline)

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
                        val chipBorder = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = palette.goldHairline,
                            selectedBorderColor = palette.accentGold
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
                                border = chipBorder,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = currentThemeMode == AppThemeMode.DARK,
                                onClick = { themeViewModel.setThemeMode(AppThemeMode.DARK) },
                                label = { Text("Obsidian", fontWeight = FontWeight.SemiBold) },
                                colors = chipColors,
                                border = chipBorder,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = currentThemeMode == AppThemeMode.EINK,
                                onClick = { themeViewModel.setThemeMode(AppThemeMode.EINK) },
                                label = { Text("E-Ink", fontWeight = FontWeight.SemiBold) },
                                colors = chipColors,
                                border = chipBorder,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Stats Card
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = palette.cardSurface,
                    hairlineColor = palette.goldHairline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = palette.accentSaffron, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${profile.streak}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = palette.accentGold, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${profile.xp}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = palette.accentCyan, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${profile.completedLessons.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showPracticeRosterDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_practice_scholars_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, palette.goldHairline)
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = palette.accentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Practice Scholars Directory", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        authViewModel.signOut(context, onSignedOut)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_sign_out_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentVermilion,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isOfflinePreview) "Exit Preview & Sign In" else "Sign Out", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChangeVidyaIdDialog(
    profile: UserProfile,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val availability by authViewModel.vidyaAvailability.collectAsStateWithLifecycle()
    var handle by remember(profile.vidyaHandle) {
        mutableStateOf(profile.vidyaHandle.ifBlank { VidyaIdFormatter.suggestHandle(profile.name, profile.email) })
    }
    var code by remember(profile.vidyaDiscriminator) {
        mutableStateOf(profile.vidyaDiscriminator.ifBlank { VidyaIdFormatter.randomDiscriminator() })
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(handle, code) {
        authViewModel.checkVidyaIdAvailability(handle, code)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Vidya ID", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Claim a new handle#code. Your previous Vidya ID will be released automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = VidyaIdFormatter.normalizeHandle(it) },
                        label = { Text("Handle") },
                        prefix = { Text("@") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(4) },
                        label = { Text("Code") },
                        prefix = { Text("#") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (val state = availability) {
                            VidyaAvailabilityState.Checking -> "Checking..."
                            is VidyaAvailabilityState.Available -> "${state.formattedId} available"
                            is VidyaAvailabilityState.Unavailable -> state.reason
                            VidyaAvailabilityState.Idle -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (availability is VidyaAvailabilityState.Unavailable) palette.accentVermilion else palette.accentCyan,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { code = VidyaIdFormatter.randomDiscriminator() }) {
                        Text("Shuffle", fontWeight = FontWeight.Bold)
                    }
                }
                if (errorText != null) {
                    Text(errorText.orEmpty(), color = palette.accentVermilion, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = availability !is VidyaAvailabilityState.Unavailable &&
                    VidyaIdFormatter.isValidHandle(VidyaIdFormatter.normalizeHandle(handle)) &&
                    VidyaIdFormatter.isValidDiscriminator(code),
                onClick = {
                    authViewModel.completeOnboarding(
                        displayName = profile.name,
                        handle = handle,
                        discriminator = code,
                        avatarSpec = profile.toAvatarSpec(),
                        focusRealm = profile.focusRealm,
                        standardId = profile.standardId,
                        streamId = profile.streamId,
                        batchType = profile.batchType,
                        preferredLanguage = profile.preferredLanguage,
                        onError = { msg -> errorText = msg }
                    )
                    Toast.makeText(context, "Vidya ID updated", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.accentSaffron)
            ) {
                Text("Claim ID", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PracticeScholarsDialog(onDismiss: () -> Unit) {
    val palette = LocalIndoPalette.current
    Dialog(onDismissRequest = onDismiss) {
        IndoToneSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            containerColor = palette.cardSurface,
            hairlineColor = palette.goldHairline
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Practice Scholars Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Search any Vidya ID below inside Varta → New Chat or challenge them in Khel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PracticePeersCatalog.sampleScholars.forEach { card ->
                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = palette.elevatedSurface,
                        hairlineColor = palette.goldHairline
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ScholarAvatarBadge(
                                avatarSpec = card.toAvatarSpec(),
                                displayName = card.displayName,
                                size = 40.dp,
                                isOnline = true
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(card.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "${card.vidyaId} • ${card.level}",
                                    fontSize = 11.sp,
                                    color = palette.accentCyan
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
