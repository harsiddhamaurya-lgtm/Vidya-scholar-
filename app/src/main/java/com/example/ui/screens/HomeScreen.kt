package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.QuestLevel
import com.example.data.QuestNodeState
import com.example.data.RealmId
import com.example.data.SubjectRealm
import com.example.data.UserProfile
import com.example.data.VidyaRepository
import com.example.ui.components.ScholarAvatarBadge
import com.example.ui.components.VidyaIdChip
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.IndoMotion
import com.example.utils.SoundHelper
import com.example.viewmodel.VidyaVaultViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    onNavigateToCourse: (String) -> Unit,
    onNavigateToAiTutor: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToProfile: () -> Unit,
    vidyaViewModel: VidyaVaultViewModel = viewModel(),
    onResumeQuestLevel: ((RealmId, QuestLevel) -> Unit)? = null,
    onLaunchDailyChallenge: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    val completedIds by vidyaViewModel.completedLevelIds.collectAsState()

    // Select up to 3 active realms to showcase as Mini-Quest Campaign Previews
    val resumeRealms = remember(completedIds) {
        val preferred = listOf(RealmId.MATHEMATICS, RealmId.CODING_AI, RealmId.PHYSICS)
        preferred.map { realmId ->
            val realm = VidyaRepository.getRealm(realmId)
            val activeLvl = realm.levels.firstOrNull {
                vidyaViewModel.getNodeState(realm, it, completedIds) == QuestNodeState.ACTIVE
            } ?: realm.levels.last()
            realm to activeLvl
        }
    }

    val dailyChallengePrompt = "Daily Challenge: Explain intuitively why d/dx(e^x) equals e^x and give a 1-minute physics example."

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentSaffron) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. THE WARRIOR STATUS HEADER (TOP PROFILE BAR)
            item {
                StaggeredHomeSection(index = 0) {
                    WarriorStatusHeader(
                        userProfile = userProfile,
                        completedQuestCount = completedIds.size,
                        onOpenProfile = {
                            SoundHelper.playClick(context)
                            onNavigateToProfile()
                        }
                    )
                }
            }

            // 2. THE SHAKTI AI MENTOR BANNER ("THE ORACLE PORTAL")
            item {
                StaggeredHomeSection(index = 1) {
                    OraclePortalHeroBanner(
                        onAskGuru = {
                            SoundHelper.playWoodyTock(context)
                            onNavigateToAiTutor()
                        }
                    )
                }
            }

            // 3. QUICK-PORTAL ACTION CARDS (VARTA & AI COACH BENTO GRID)
            item {
                StaggeredHomeSection(index = 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VartaQuickPortalCard(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundHelper.playWoodyTock(context)
                                onNavigateToCommunity()
                            }
                        )
                        AiCoachQuickPortalCard(
                            modifier = Modifier.weight(1f),
                            challengeTitle = "Why d/dx(e^x) = e^x?",
                            onClick = {
                                SoundHelper.playTempleChime(context)
                                if (onLaunchDailyChallenge != null) {
                                    onLaunchDailyChallenge(dailyChallengePrompt)
                                } else {
                                    onNavigateToAiTutor()
                                }
                            }
                        )
                    }
                }
            }

            // 4. "CONTINUE LEARNING" QUEST RESUME MODULE HEADER
            item {
                StaggeredHomeSection(index = 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Continue Quest • Vidya",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Teleport straight into your active chapter milestone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // MINI-QUEST PROGRESS CARDS WITH MILESTONE PATH + DIRECT RESUME TRIGGER
            itemsIndexed(resumeRealms, key = { _, pair -> pair.first.realmId.name }) { idx, (realm, activeLevel) ->
                StaggeredHomeSection(index = 4 + idx) {
                    QuestResumeCampaignCard(
                        realm = realm,
                        activeLevel = activeLevel,
                        completedIds = completedIds,
                        vidyaViewModel = vidyaViewModel,
                        onResume = {
                            SoundHelper.playTempleChime(context)
                            if (onResumeQuestLevel != null) {
                                onResumeQuestLevel(realm.realmId, activeLevel)
                            } else {
                                onNavigateToCourse(realm.realmId.courseId)
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
        }
    }
}

@Composable
private fun StaggeredHomeSection(
    index: Int,
    content: @Composable () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val slideAnim = remember { Animatable(22f) }

    LaunchedEffect(Unit) {
        delay((index * 55L).coerceAtMost(300L))
        alphaAnim.animateTo(1f, tween(240, easing = IndoMotion.OrganicDeceleration))
    }
    LaunchedEffect(Unit) {
        delay((index * 55L).coerceAtMost(300L))
        slideAnim.animateTo(0f, tween(260, easing = IndoMotion.OrganicDeceleration))
    }

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = slideAnim.value
        }
    ) {
        content()
    }
}

@Composable
private fun WarriorStatusHeader(
    userProfile: UserProfile?,
    completedQuestCount: Int,
    onOpenProfile: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val xp = userProfile?.xp ?: 1450
    val streak = userProfile?.streak ?: 7
    val rankLevel = (completedQuestCount / 2).coerceAtLeast(1) + 1
    val rankLabel = when {
        xp >= 2500 -> "Level $rankLevel Sovereign Sage"
        xp >= 1200 -> "Level $rankLevel Architect Initiate"
        else -> "Level $rankLevel Vidya Seeker"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "warrior_hud")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Scholar Avatar Badge + Greeting, Vidya ID & Earned Rank
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenProfile)
                .padding(vertical = 4.dp)
                .testTag("home_warrior_profile_trigger")
        ) {
            ScholarAvatarBadge(
                avatarSpec = userProfile?.toAvatarSpec() ?: com.example.data.AvatarSpec(),
                displayName = userProfile?.name ?: "Scholar",
                size = 56.dp,
                animateRing = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Namaste, Scholar",
                    fontSize = 11.sp,
                    color = if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = userProfile?.name ?: "Aarav Scholar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VidyaIdChip(
                        vidyaId = userProfile?.effectiveVidyaId() ?: "scholar#1000",
                        compact = true,
                        showCopyIcon = false
                    )
                    Text(
                        text = rankLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accentGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right: Glassmorphic HUD Capsules with Gold Metallic Borders
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val capsuleBg = if (palette.isEInk) palette.cardSurface else palette.elevatedSurface

            IndoToneSurface(
                shape = RoundedCornerShape(16.dp),
                containerColor = capsuleBg,
                hairlineColor = palette.goldHairline
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14),
                        modifier = Modifier
                            .size(17.dp)
                            .graphicsLayer {
                                if (!palette.isEInk) {
                                    scaleX = flameScale
                                    scaleY = flameScale
                                }
                            }
                    )
                    Text(
                        text = "${streak}d",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IndoToneSurface(
                shape = RoundedCornerShape(16.dp),
                containerColor = capsuleBg,
                hairlineColor = palette.goldHairline
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "XP",
                        tint = palette.accentGold,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "$xp",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun OraclePortalHeroBanner(
    onAskGuru: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "oracle_pulse")
    val energyAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oracle_energy"
    )

    val bannerBg = when {
        palette.isEInk -> Brush.verticalGradient(listOf(palette.cardSurface, palette.cardSurface))
        palette.isDark -> Brush.linearGradient(listOf(Color(0xFF171E30), Color(0xFF13222A)))
        else -> Brush.linearGradient(listOf(Color(0xFF232B3E), Color(0xFF1E3338)))
    }
    val bannerTextColor = if (palette.isEInk) MaterialTheme.colorScheme.onSurface else Color(0xFFF6F1E6)
    val bannerSubColor = if (palette.isEInk) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFC5CCD9)

    IndoToneSurface(
        onClick = onAskGuru,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("oracle_portal_banner"),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.Transparent,
        hairlineColor = palette.goldHairline
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerBg)
        ) {
            // Subtle Glowing Sri Yantra Sacred Geometry Watermark in Corner
            if (!palette.isEInk) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val c = Offset(size.width * 0.87f, size.height * 0.42f)
                    val r = size.height * 0.56f
                    val yantraGold = Color(0xFFD4AF37).copy(alpha = 0.14f)
                    val yantraCyan = Color(0xFF00D2E6).copy(alpha = 0.12f)
                    val stroke = Stroke(width = 1.2.dp.toPx())

                    drawCircle(yantraGold, radius = r, center = c, style = stroke)
                    drawCircle(yantraCyan, radius = r * 0.68f, center = c, style = stroke)
                    for (rot in listOf(0f, 30f, 60f)) {
                        rotate(rot, pivot = c) {
                            val tri = Path().apply {
                                for (v in 0..2) {
                                    val a = (-90.0 + v * 120.0) * PI / 180.0
                                    val px = c.x + (r * 0.78f * cos(a)).toFloat()
                                    val py = c.y + (r * 0.78f * sin(a)).toFloat()
                                    if (v == 0) moveTo(px, py) else lineTo(px, py)
                                }
                                close()
                            }
                            drawPath(tri, yantraGold, style = stroke)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = if (palette.isEInk) 1f else energyAlpha))
                    )
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "THE ORACLE PORTAL • COSMOS AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFD4AF37)
                    )
                }

                Text(
                    text = "Consult Your AI Guru for Instant Mastery",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = bannerTextColor
                )

                Text(
                    text = "Step-by-step concept clarity, derivations, and instant doubt resolution across every realm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = bannerSubColor
                )

                Button(
                    onClick = onAskGuru,
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("oracle_ask_guru_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14),
                        contentColor = if (palette.isEInk) MaterialTheme.colorScheme.onPrimary else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Ask the Guru →",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VartaQuickPortalCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.97f
            hovered -> 1.015f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.72f),
        label = "varta_portal_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "varta_live")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "varta_dot"
    )

    IndoToneSurface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag("home_varta_portal"),
        shape = RoundedCornerShape(20.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.secondaryCopper),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Forum, contentDescription = null, tint = palette.accentCyan, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = if (palette.isEInk) 1f else pulseAlpha))
                )
            }

            Text(
                text = "Varta Talks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.14f)
            ) {
                Text(
                    text = "3 friends online in Physics Sabha",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (palette.isEInk) MaterialTheme.colorScheme.onSurface else Color(0xFF0F8C62)
                )
            }
        }
    }
}

@Composable
private fun AiCoachQuickPortalCard(
    modifier: Modifier = Modifier,
    challengeTitle: String,
    onClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.97f
            hovered -> 1.015f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.72f),
        label = "coach_portal_scale"
    )

    IndoToneSurface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag("home_ai_coach_portal"),
        shape = RoundedCornerShape(20.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.secondaryTerracotta),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = palette.accentSaffron, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "DAILY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE88D14)
                )
            }

            Text(
                text = "AI Guru Coach",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE88D14).copy(alpha = 0.14f)
            ) {
                Text(
                    text = "Challenge: $challengeTitle",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (palette.isEInk) MaterialTheme.colorScheme.onSurface else Color(0xFFC2590B)
                )
            }
        }
    }
}

@Composable
private fun QuestResumeCampaignCard(
    realm: SubjectRealm,
    activeLevel: QuestLevel,
    completedIds: Set<String>,
    vidyaViewModel: VidyaVaultViewModel,
    onResume: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.72f),
        label = "resume_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "node_flash_${realm.realmId.name}")
    val flashScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "flash_scale"
    )
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "flash_alpha"
    )

    val accentColor = if (palette.isEInk) palette.accentSaffron else realm.accentPrimary

    IndoToneSurface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onResume
            )
            .testTag("resume_quest_${realm.realmId.name.lowercase()}"),
        shape = RoundedCornerShape(22.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = realm.realmId.subjectName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = accentColor
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeLevel.isBossBattle) palette.accentVermilion.copy(alpha = 0.16f) else Color(0xFFE88D14).copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (activeLevel.isBossBattle) "BOSS ARENA" else "LVL ${activeLevel.levelNumber} ACTIVE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (activeLevel.isBossBattle) palette.accentVermilion else Color(0xFFC2590B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = activeLevel.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeLevel.chapterSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Glowing Direct Resume Emblem Button
                Box(contentAlignment = Alignment.Center) {
                    if (!palette.isEInk) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = flashScale * 0.96f
                                    scaleY = flashScale * 0.96f
                                    alpha = flashAlpha
                                }
                                .clip(CircleShape)
                                .background(Color(0xFFE88D14))
                        )
                    }
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14))
                            .border(1.2.dp, Color(0xFFFFD166), CircleShape)
                            .testTag("resume_play_${realm.realmId.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume ${activeLevel.title}",
                            tint = if (palette.isEInk) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Mini-Milestone Campaign Path (Completed Nodes + Flashing Active Node + Locked Seals)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    val y = size.height / 2f
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFD4AF37).copy(alpha = 0.7f), palette.accentCyan.copy(alpha = 0.55f))
                        ),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    realm.levels.forEach { lvl ->
                        val nodeState = vidyaViewModel.getNodeState(realm, lvl, completedIds)
                        Box(contentAlignment = Alignment.Center) {
                            if (nodeState == QuestNodeState.ACTIVE && !palette.isEInk) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .graphicsLayer {
                                            scaleX = flashScale
                                            scaleY = flashScale
                                            alpha = flashAlpha
                                        }
                                        .clip(CircleShape)
                                        .background(if (lvl.isBossBattle) palette.accentVermilion else Color(0xFFE88D14))
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(if (lvl.isBossBattle) 30.dp else 26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            nodeState == QuestNodeState.COMPLETED -> Color(0xFFD4AF37)
                                            nodeState == QuestNodeState.ACTIVE && lvl.isBossBattle -> palette.accentVermilion
                                            nodeState == QuestNodeState.ACTIVE -> Color(0xFFE88D14)
                                            else -> palette.elevatedSurface
                                        }
                                    )
                                    .border(
                                        width = if (nodeState == QuestNodeState.ACTIVE) 1.6.dp else 1.dp,
                                        color = if (nodeState == QuestNodeState.ACTIVE) Color(0xFFFFD166) else palette.goldHairline,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    lvl.isBossBattle -> Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = "Boss Level",
                                        tint = if (nodeState == QuestNodeState.LOCKED) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    nodeState == QuestNodeState.COMPLETED -> Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    nodeState == QuestNodeState.ACTIVE -> Text(
                                        text = "${lvl.levelNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    else -> Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
