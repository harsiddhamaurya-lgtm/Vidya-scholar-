package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AvatarMode
import com.example.data.AvatarSpec
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class AvatarPresetOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val crestId: String,
    val frameId: String,
    val toneId: String
) {
    fun toAvatarSpec(): AvatarSpec = AvatarSpec(
        mode = AvatarMode.PRESET,
        presetId = id,
        crestId = crestId,
        frameId = frameId,
        toneId = toneId
    )
}

data class AvatarToneOption(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val surfaceWell: Color
)

data class AvatarPartOption(
    val id: String,
    val label: String
)

object AvatarCatalog {
    val presets: List<AvatarPresetOption> = listOf(
        AvatarPresetOption(
            id = "vedic_strategist",
            title = "Vedic Strategist",
            subtitle = "Sri Yantra & logic",
            crestId = "yantra",
            frameId = "astrolabe",
            toneId = "saffron"
        ),
        AvatarPresetOption(
            id = "quantum_alchemist",
            title = "Quantum Alchemist",
            subtitle = "Atomic orbit pioneer",
            crestId = "chakra",
            frameId = "ashtapada",
            toneId = "cyan"
        ),
        AvatarPresetOption(
            id = "celestial_architect",
            title = "Celestial Architect",
            subtitle = "Astrolabe precision",
            crestId = "compass",
            frameId = "astrolabe",
            toneId = "indigo"
        ),
        AvatarPresetOption(
            id = "lotus_sage",
            title = "Lotus Sage",
            subtitle = "Serene focus mastery",
            crestId = "lotus",
            frameId = "lotus_halo",
            toneId = "emerald"
        ),
        AvatarPresetOption(
            id = "vidyut_maverick",
            title = "Vidyut Maverick",
            subtitle = "Khel arena spark",
            crestId = "flame",
            frameId = "ashtapada",
            toneId = "vermilion"
        ),
        AvatarPresetOption(
            id = "night_observatory",
            title = "Night Observatory",
            subtitle = "Stellar scholar crest",
            crestId = "trishul",
            frameId = "minimal_gold",
            toneId = "gold"
        )
    )

    val crests: List<AvatarPartOption> = listOf(
        AvatarPartOption("yantra", "Sri Yantra"),
        AvatarPartOption("lotus", "Sacred Lotus"),
        AvatarPartOption("chakra", "Atomic Chakra"),
        AvatarPartOption("compass", "Star Compass"),
        AvatarPartOption("trishul", "Trishul"),
        AvatarPartOption("flame", "Agni Flame")
    )

    val frames: List<AvatarPartOption> = listOf(
        AvatarPartOption("astrolabe", "Astrolabe"),
        AvatarPartOption("ashtapada", "Ashtapada"),
        AvatarPartOption("lotus_halo", "Lotus Halo"),
        AvatarPartOption("minimal_gold", "Gold Seal")
    )

    val tones: List<AvatarToneOption> = listOf(
        AvatarToneOption("saffron", "Saffron", Color(0xFFE88D14), Color(0xFFFFD166), Color(0xFF3A221B)),
        AvatarToneOption("cyan", "Cyan", Color(0xFF00B8D4), Color(0xFF80F3FF), Color(0xFF152C34)),
        AvatarToneOption("gold", "Gold", Color(0xFFD4AF37), Color(0xFFFFE58F), Color(0xFF2D2514)),
        AvatarToneOption("indigo", "Indigo", Color(0xFF4F6DF5), Color(0xFF8CA8FF), Color(0xFF1B243B)),
        AvatarToneOption("vermilion", "Vermilion", Color(0xFFD6335C), Color(0xFFFF8BA7), Color(0xFF391926)),
        AvatarToneOption("emerald", "Emerald", Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF132E27))
    )

    fun toneFor(id: String): AvatarToneOption =
        tones.firstOrNull { it.id == id } ?: tones.first()

    fun presetFor(id: String): AvatarPresetOption =
        presets.firstOrNull { it.id == id } ?: presets.first()
}

@Composable
fun ScholarAvatarBadge(
    avatarSpec: AvatarSpec,
    displayName: String,
    size: Dp = 64.dp,
    isOnline: Boolean = false,
    animateRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalIndoPalette.current
    val tone = AvatarCatalog.toneFor(avatarSpec.toneId)
    val effectiveCrest = if (avatarSpec.mode == AvatarMode.PRESET) {
        AvatarCatalog.presetFor(avatarSpec.presetId).crestId
    } else {
        avatarSpec.crestId
    }
    val effectiveFrame = avatarSpec.frameId.ifBlank { "astrolabe" }

    val hasValidPhoto = avatarSpec.mode == AvatarMode.PHOTO &&
        avatarSpec.localPhotoPath.isNotBlank() &&
        remember(avatarSpec.localPhotoPath) { File(avatarSpec.localPhotoPath).exists() }

    val rotation by if (animateRing && !palette.isEInk) {
        val infiniteTransition = rememberInfiniteTransition(label = "avatar_frame_rot")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
            label = "ring_angle"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val ringPrimary = if (palette.isEInk) palette.accentSaffron else tone.primary
    val ringSecondary = if (palette.isEInk) palette.accentGold else tone.secondary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer Indo-Futuristic Frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension / 2f - 2.dp.toPx()
            drawGeometricFrame(
                frameId = effectiveFrame,
                center = c,
                outerR = outerR,
                primary = ringPrimary,
                secondary = ringSecondary,
                gold = palette.accentGold,
                rotation = rotation,
                isEInk = palette.isEInk
            )
        }

        // Inner Medallion (Photo or Layered Vector Crest)
        val innerPadding = (size * 0.13f).coerceAtLeast(5.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clip(CircleShape)
                .background(if (palette.isEInk) palette.cardSurface else tone.surfaceWell)
                .border(
                    width = 1.dp,
                    color = palette.accentGold.copy(alpha = if (palette.isEInk) 0.7f else 0.55f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasValidPhoto) {
                AsyncImage(
                    model = File(avatarSpec.localPhotoPath),
                    contentDescription = "$displayName avatar photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val r = this.size.minDimension / 2f
                    if (!palette.isEInk) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ringPrimary.copy(alpha = 0.28f),
                                    tone.surfaceWell.copy(alpha = 0.94f),
                                    Color(0xFF0A0D12).copy(alpha = 0.92f)
                                ),
                                center = Offset(c.x - r * 0.2f, c.y - r * 0.2f),
                                radius = r * 1.35f
                            ),
                            radius = r,
                            center = c
                        )
                    }
                    drawAvatarCrest(
                        crestId = effectiveCrest,
                        center = c,
                        radius = r * 0.58f,
                        primary = ringPrimary,
                        secondary = ringSecondary
                    )
                }
            }
        }

        if (isOnline && !palette.isEInk) {
            val dotSize = (size * 0.22f).coerceIn(10.dp, 16.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(2.dp, palette.cardSurface, CircleShape)
            )
        }
    }
}

private fun DrawScope.drawGeometricFrame(
    frameId: String,
    center: Offset,
    outerR: Float,
    primary: Color,
    secondary: Color,
    gold: Color,
    rotation: Float,
    isEInk: Boolean
) {
    val ringWidth = (outerR * 0.09f).coerceIn(1.8.dp.toPx(), 3.4.dp.toPx())
    val ringBrush = if (isEInk) {
        Brush.linearGradient(listOf(primary, gold))
    } else {
        Brush.sweepGradient(listOf(primary, gold, secondary, primary), center)
    }

    drawCircle(
        brush = ringBrush,
        radius = outerR,
        center = center,
        style = Stroke(width = ringWidth)
    )

    when (frameId) {
        "astrolabe" -> {
            rotate(rotation, pivot = center) {
                for (i in 0 until 4) {
                    val a = i * PI / 2.0
                    val pt = Offset(
                        center.x + (outerR * cos(a)).toFloat(),
                        center.y + (outerR * sin(a)).toFloat()
                    )
                    drawCircle(secondary, radius = ringWidth * 0.85f, center = pt)
                }
            }
        }
        "ashtapada" -> {
            rotate(rotation * 0.5f, pivot = center) {
                for (i in 0 until 8) {
                    val a = i * PI / 4.0
                    val p1 = Offset(
                        center.x + ((outerR - ringWidth) * cos(a)).toFloat(),
                        center.y + ((outerR - ringWidth) * sin(a)).toFloat()
                    )
                    val p2 = Offset(
                        center.x + ((outerR + ringWidth * 0.45f) * cos(a)).toFloat(),
                        center.y + ((outerR + ringWidth * 0.45f) * sin(a)).toFloat()
                    )
                    drawLine(gold, p1, p2, strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
        "lotus_halo" -> {
            rotate(-rotation * 0.4f, pivot = center) {
                for (i in 0 until 8) {
                    val a = i * PI / 4.0
                    val tip = Offset(
                        center.x + ((outerR + ringWidth * 0.3f) * cos(a)).toFloat(),
                        center.y + ((outerR + ringWidth * 0.3f) * sin(a)).toFloat()
                    )
                    drawCircle(
                        color = if (i % 2 == 0) gold else primary,
                        radius = ringWidth * 0.72f,
                        center = tip
                    )
                }
            }
        }
        else -> {
            drawCircle(
                color = gold.copy(alpha = 0.5f),
                radius = outerR - ringWidth * 1.4f,
                center = center,
                style = Stroke(width = 0.9.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawAvatarCrest(
    crestId: String,
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color
) {
    val stroke = Stroke(width = (radius * 0.14f).coerceIn(1.4.dp.toPx(), 2.6.dp.toPx()), cap = StrokeCap.Round)
    when (crestId) {
        "yantra" -> {
            val up = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.86f, center.y + radius * 0.58f)
                lineTo(center.x - radius * 0.86f, center.y + radius * 0.58f)
                close()
            }
            val down = Path().apply {
                moveTo(center.x, center.y + radius)
                lineTo(center.x + radius * 0.86f, center.y - radius * 0.58f)
                lineTo(center.x - radius * 0.86f, center.y - radius * 0.58f)
                close()
            }
            drawPath(up, primary, style = stroke)
            drawPath(down, secondary, style = stroke)
            drawCircle(secondary, radius = radius * 0.16f, center = center)
        }
        "lotus" -> {
            for (k in -1..1) {
                val angleOffset = k * 28f
                rotate(angleOffset, pivot = center) {
                    val petal = Path().apply {
                        moveTo(center.x, center.y + radius * 0.7f)
                        quadraticTo(
                            center.x - radius * 0.45f, center.y - radius * 0.1f,
                            center.x, center.y - radius * 0.95f
                        )
                        quadraticTo(
                            center.x + radius * 0.45f, center.y - radius * 0.1f,
                            center.x, center.y + radius * 0.7f
                        )
                        close()
                    }
                    drawPath(petal, if (k == 0) primary else secondary, style = stroke)
                }
            }
            drawCircle(secondary, radius = radius * 0.14f, center = Offset(center.x, center.y + radius * 0.25f))
        }
        "chakra" -> {
            for (angle in listOf(0f, 60f, 120f)) {
                rotate(angle, pivot = center) {
                    drawOval(
                        color = primary,
                        topLeft = Offset(center.x - radius, center.y - radius * 0.36f),
                        size = Size(radius * 2f, radius * 0.72f),
                        style = stroke
                    )
                }
            }
            drawCircle(secondary, radius = radius * 0.22f, center = center)
        }
        "compass" -> {
            val star = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.26f, center.y - radius * 0.26f)
                lineTo(center.x + radius, center.y)
                lineTo(center.x + radius * 0.26f, center.y + radius * 0.26f)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius * 0.26f, center.y + radius * 0.26f)
                lineTo(center.x - radius, center.y)
                lineTo(center.x - radius * 0.26f, center.y - radius * 0.26f)
                close()
            }
            drawPath(star, primary, style = stroke)
            drawCircle(secondary, radius = radius * 0.22f, center = center)
        }
        "trishul" -> {
            // Central shaft
            drawLine(
                color = primary,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )
            // Trident curved prongs
            val prongs = Path().apply {
                moveTo(center.x - radius * 0.65f, center.y - radius * 0.55f)
                quadraticTo(center.x - radius * 0.5f, center.y + radius * 0.15f, center.x, center.y + radius * 0.05f)
                quadraticTo(center.x + radius * 0.5f, center.y + radius * 0.15f, center.x + radius * 0.65f, center.y - radius * 0.55f)
            }
            drawPath(prongs, secondary, style = stroke)
            drawCircle(primary, radius = radius * 0.15f, center = Offset(center.x, center.y + radius * 0.45f))
        }
        else -> {
            // Agni Flame
            val flame = Path().apply {
                moveTo(center.x, center.y - radius * 0.95f)
                quadraticTo(center.x + radius * 0.85f, center.y, center.x + radius * 0.48f, center.y + radius * 0.68f)
                quadraticTo(center.x, center.y + radius, center.x - radius * 0.48f, center.y + radius * 0.68f)
                quadraticTo(center.x - radius * 0.85f, center.y, center.x, center.y - radius * 0.95f)
                close()
            }
            drawPath(flame, primary, style = stroke)
            drawCircle(secondary, radius = radius * 0.24f, center = Offset(center.x, center.y + radius * 0.28f))
        }
    }
}

@Composable
fun VidyaIdChip(
    vidyaId: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showCopyIcon: Boolean = true,
    onCopied: (() -> Unit)? = null
) {
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val displayId = vidyaId.ifBlank { "scholar#1000" }

    IndoToneSurface(
        onClick = {
            clipboard.setText(AnnotatedString(displayId))
            Toast.makeText(context, context.getString(R.string.vidya_id_copied), Toast.LENGTH_SHORT).show()
            onCopied?.invoke()
        },
        modifier = modifier.testTag("vidya_id_chip"),
        shape = RoundedCornerShape(if (compact) 10.dp else 14.dp),
        containerColor = if (palette.isEInk) palette.elevatedSurface else palette.secondaryCopper.copy(alpha = 0.65f),
        hairlineColor = palette.goldHairline
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = palette.accentGold,
                modifier = Modifier.size(if (compact) 12.dp else 15.dp)
            )
            Text(
                text = displayId,
                fontSize = if (compact) 11.sp else 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (palette.isEInk) MaterialTheme.colorScheme.onSurface else palette.accentCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showCopyIcon) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Vidya ID",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(if (compact) 11.dp else 14.dp)
                )
            }
        }
    }
}

@Composable
fun HybridAvatarStudioContent(
    avatarSpec: AvatarSpec,
    displayName: String,
    onSpecChange: (AvatarSpec) -> Unit,
    onPickPhoto: suspend (Uri) -> String?,
    modifier: Modifier = Modifier
) {
    val palette = LocalIndoPalette.current
    val scope = rememberCoroutineScope()
    var activeTab by remember {
        mutableIntStateOf(
            when (avatarSpec.mode) {
                AvatarMode.PRESET -> 0
                AvatarMode.BUILDER -> 1
                AvatarMode.PHOTO -> 2
            }
        )
    }
    var isSavingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isSavingPhoto = true
            scope.launch {
                val savedPath = onPickPhoto(uri)
                isSavingPhoto = false
                if (!savedPath.isNullOrBlank()) {
                    onSpecChange(
                        avatarSpec.copy(
                            mode = AvatarMode.PHOTO,
                            localPhotoPath = savedPath
                        )
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Identity Badge Preview Stage
        IndoToneSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            containerColor = palette.elevatedSurface,
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
                    size = 76.dp,
                    animateRing = true
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (avatarSpec.mode) {
                            AvatarMode.PRESET -> AvatarCatalog.presetFor(avatarSpec.presetId).title
                            AvatarMode.BUILDER -> "Custom Crest Forged"
                            AvatarMode.PHOTO -> "Framed Scholar Portrait"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (avatarSpec.mode) {
                            AvatarMode.PRESET -> AvatarCatalog.presetFor(avatarSpec.presetId).subtitle
                            AvatarMode.BUILDER -> "${avatarSpec.crestId.replaceFirstChar { it.uppercase() }} • ${avatarSpec.frameId.replace('_', ' ')}"
                            AvatarMode.PHOTO -> "Displayed inside your ${avatarSpec.frameId.replace('_', ' ')} frame across Varta & Khel"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Studio Mode Switcher
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = palette.accentSaffron,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = palette.cardSurface,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Presets", "Custom Builder", "Photo Upload").forEachIndexed { idx, label ->
                FilterChip(
                    selected = activeTab == idx,
                    onClick = { activeTab = idx },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = chipColors,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("avatar_studio_tab_$idx")
                )
            }
        }

        when (activeTab) {
            0 -> {
                // 6 Curated Indo-Futurist Archetypes
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AvatarCatalog.presets.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { preset ->
                                val isSelected = avatarSpec.mode == AvatarMode.PRESET && avatarSpec.presetId == preset.id
                                IndoToneSurface(
                                    onClick = { onSpecChange(preset.toAvatarSpec()) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("avatar_preset_${preset.id}"),
                                    shape = RoundedCornerShape(18.dp),
                                    containerColor = if (isSelected) palette.secondaryIndigo else palette.cardSurface,
                                    hairlineColor = if (isSelected) palette.accentGold else palette.goldHairline,
                                    borderWidth = if (isSelected) 1.6.dp else 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ScholarAvatarBadge(
                                            avatarSpec = preset.toAvatarSpec(),
                                            displayName = displayName,
                                            size = 44.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = preset.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = preset.subtitle,
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
                }
            }

            1 -> {
                // Layered Indo-Futuristic Vector Builder
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sacred Crest",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AvatarCatalog.crests, key = { it.id }) { crest ->
                            val selected = avatarSpec.crestId == crest.id && avatarSpec.mode == AvatarMode.BUILDER
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onSpecChange(
                                        avatarSpec.copy(
                                            mode = AvatarMode.BUILDER,
                                            crestId = crest.id
                                        )
                                    )
                                },
                                label = { Text(crest.label, fontSize = 12.sp) },
                                colors = chipColors
                            )
                        }
                    }

                    Text(
                        text = "Geometric Frame",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AvatarCatalog.frames, key = { it.id }) { frame ->
                            val selected = avatarSpec.frameId == frame.id
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onSpecChange(
                                        avatarSpec.copy(
                                            mode = AvatarMode.BUILDER,
                                            frameId = frame.id
                                        )
                                    )
                                },
                                label = { Text(frame.label, fontSize = 12.sp) },
                                colors = chipColors
                            )
                        }
                    }

                    Text(
                        text = "Realm Accent Tone",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AvatarCatalog.tones, key = { it.id }) { tone ->
                            val selected = avatarSpec.toneId == tone.id
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(tone.primary)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) palette.accentGold else palette.goldHairline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onSpecChange(
                                            avatarSpec.copy(
                                                mode = AvatarMode.BUILDER,
                                                toneId = tone.id
                                            )
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = tone.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Zero-Permission Photo Picker Upload + Frame Preservation
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    containerColor = palette.cardSurface,
                    hairlineColor = palette.goldHairline
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Personal Portrait Inside Sacred Frame",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Uses Android Photo Picker (no storage permission needed). Kept neatly inside your chosen geometric frame.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isSavingPhoto,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_avatar_photo_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accentSaffron,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSavingPhoto) "Saving Portrait..." else if (avatarSpec.localPhotoPath.isNotBlank()) "Change Personal Photo" else "Choose Personal Photo",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Frame Style Around Photo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AvatarCatalog.frames, key = { it.id }) { frame ->
                                FilterChip(
                                    selected = avatarSpec.frameId == frame.id,
                                    onClick = {
                                        onSpecChange(avatarSpec.copy(frameId = frame.id))
                                    },
                                    label = { Text(frame.label, fontSize = 12.sp) },
                                    colors = chipColors
                                )
                            }
                        }

                        if (avatarSpec.mode == AvatarMode.PHOTO && avatarSpec.localPhotoPath.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    onSpecChange(
                                        avatarSpec.copy(
                                            mode = AvatarMode.PRESET,
                                            localPhotoPath = ""
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.goldHairline)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Back to Sacred Crest", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HybridAvatarStudioDialog(
    initialSpec: AvatarSpec,
    displayName: String,
    onPickPhoto: suspend (Uri) -> String?,
    onDismiss: () -> Unit,
    onSave: (AvatarSpec) -> Unit
) {
    val palette = LocalIndoPalette.current
    var draftSpec by remember(initialSpec) { mutableStateOf(initialSpec) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        IndoToneSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            containerColor = palette.baseBg,
            hairlineColor = palette.goldHairline
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scholar Avatar Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = palette.goldHairline)

                HybridAvatarStudioContent(
                    avatarSpec = draftSpec,
                    displayName = displayName,
                    onSpecChange = { draftSpec = it },
                    onPickPhoto = onPickPhoto
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.goldHairline)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onSave(draftSpec) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_avatar_studio_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentSaffron,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Apply Crest", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
