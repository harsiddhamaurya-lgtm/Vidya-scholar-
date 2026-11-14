package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AvatarSpec
import com.example.data.Chat
import com.example.data.PracticePeersCatalog
import com.example.data.UserProfile
import com.example.data.VidyaIdentityCard
import com.example.ui.components.ScholarAvatarBadge
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.IndoMotion
import com.example.utils.MandalaUnfoldContainer
import com.example.utils.SoundHelper
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.LiveStudySabha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingLandingScreen(
    userId: String,
    userProfile: UserProfile?,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit
) {
    val chats by chatViewModel.chats.collectAsState()
    val liveSabhas by chatViewModel.liveSabhas.collectAsState()
    val palette = LocalIndoPalette.current
    val context = LocalContext.current

    // 0 = Personal Chats, 1 = Discussion Rooms
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        chatViewModel.loadChats(userId)
    }

    val personalChats = remember(chats, searchQuery) {
        chats.filter { it.type == "individual" }
            .filter {
                searchQuery.isBlank() ||
                    (it.name ?: "").contains(searchQuery, ignoreCase = true) ||
                    it.peerVidyaId.contains(searchQuery, ignoreCase = true) ||
                    it.subjectTag.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
    }
    val discussionRooms = remember(chats, searchQuery) {
        chats.filter { it.type == "group" }
            .filter {
                searchQuery.isBlank() ||
                    (it.name ?: "").contains(searchQuery, ignoreCase = true) ||
                    it.peerVidyaId.contains(searchQuery, ignoreCase = true) ||
                    it.subjectTag.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
    }

    val personalUnread = remember(chats) {
        chats.filter { it.type == "individual" }.sumOf { it.unreadCount }
    }
    val roomsUnread = remember(chats) {
        chats.filter { it.type == "group" }.sumOf { it.unreadCount }
    }

    val activeThreads = if (selectedTab == 0) personalChats else discussionRooms

    if (showCreateDialog) {
        CreateVartaThreadDialog(
            initialIsGroupRoom = selectedTab == 1,
            onLookupPeer = { query -> authViewModel.lookupPeerByVidyaId(query) },
            onDismiss = { showCreateDialog = false },
            onCreateFromPeer = { peerCard, subject ->
                SoundHelper.playWoodyTock(context)
                val newChatId = chatViewModel.createThreadFromPeer(userProfile, peerCard, subject)
                selectedTab = 0
                showCreateDialog = false
                onNavigateToChat(newChatId)
            },
            onCreate = { title, subject, isRoom ->
                SoundHelper.playWoodyTock(context)
                val newChatId = chatViewModel.createThread(userId, title, subject, isRoom, userProfile)
                selectedTab = if (isRoom) 1 else 0
                showCreateDialog = false
                onNavigateToChat(newChatId)
            }
        )
    }

    val backdropTint = if (palette.isDark || palette.isEInk) palette.baseBg else Color(0xFFF2EFE6)
    val cardTint = if (palette.isDark || palette.isEInk) palette.cardSurface else Color(0xFFF8F5EE)
    val bronzeHairline = palette.goldHairline

    IndoAtmosphericBackdrop(
        ambientGlowColor = palette.accentSaffron,
        baseColor = backdropTint
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Varta • Talks & Rooms",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Peer learning & study discussions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VartaHeaderIconButton(
                                icon = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (isSearchOpen) "Close Search" else "Search Talks",
                                testTag = "varta_search_button",
                                active = isSearchOpen,
                                onClick = {
                                    SoundHelper.playClick(context)
                                    isSearchOpen = !isSearchOpen
                                    if (!isSearchOpen) searchQuery = ""
                                }
                            )
                            VartaHeaderIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = "New Room or Chat",
                                testTag = "varta_new_thread_button",
                                active = false,
                                isPrimaryAccent = true,
                                onClick = {
                                    SoundHelper.playWoodyTock(context)
                                    showCreateDialog = true
                                }
                            )
                        }
                    }

                    // Animated Inline Search Bar
                    AnimatedVisibility(
                        visible = isSearchOpen,
                        enter = expandVertically(tween(200, easing = IndoMotion.OrganicDeceleration)) + fadeIn(),
                        exit = shrinkVertically(tween(180, easing = IndoMotion.OrganicDeceleration)) + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .testTag("varta_search_input"),
                            placeholder = { Text("Search peers, rooms, or subjects...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = palette.accentSaffron)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.accentSaffron,
                                unfocusedBorderColor = bronzeHairline,
                                focusedContainerColor = cardTint,
                                unfocusedContainerColor = cardTint
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SEGMENTED PILL SWITCHER (220ms cubic-bezier(0.16, 1, 0.3, 1))
                    VartaSegmentedTabBar(
                        selectedTab = selectedTab,
                        personalUnread = personalUnread,
                        roomsUnread = roomsUnread,
                        onSelectTab = { idx ->
                            SoundHelper.playClick(context)
                            selectedTab = idx
                        }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // MODULE A: LIVE STUDY SABHAS (HORIZONTAL CAROUSEL)
                item {
                    LiveStudySabhasSection(
                        sabhas = liveSabhas,
                        cardTint = cardTint,
                        hairlineColor = bronzeHairline,
                        onJoinSabha = { sabha ->
                            SoundHelper.playWoodyTock(context)
                            onNavigateToChat(sabha.id)
                        }
                    )
                }

                // SECTION HEADER FOR ACTIVE LIST
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Personal Chats" else "Discussion Rooms",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${activeThreads.size} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // MODULE C: DESIGNED EMPTY STATE (FALLBACK)
                if (activeThreads.isEmpty()) {
                    item {
                        VartaDesignedEmptyState(
                            isPersonalTab = selectedTab == 0,
                            hasQuery = searchQuery.isNotBlank(),
                            cardTint = cardTint,
                            hairlineColor = bronzeHairline,
                            onCreateClick = {
                                SoundHelper.playWoodyTock(context)
                                showCreateDialog = true
                            },
                            onClearSearch = { searchQuery = "" }
                        )
                    }
                } else {
                    // MODULE B: MODULAR CHAT & ROOM CARDS WITH STAGGERED 40ms ENTRY + 0.98 PRESS SCALE
                    itemsIndexed(
                        items = activeThreads,
                        key = { _, chat -> "${selectedTab}_${chat.id}" }
                    ) { index, chat ->
                        ModularVartaThreadCard(
                            chat = chat,
                            index = index,
                            tabKey = selectedTab,
                            cardTint = cardTint,
                            hairlineColor = bronzeHairline,
                            onClick = {
                                SoundHelper.playWoodyTock(context)
                                chatViewModel.markChatAsRead(chat.id)
                                onNavigateToChat(chat.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VartaHeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    active: Boolean,
    isPrimaryAccent: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val bgColor = when {
        isPrimaryAccent && !palette.isEInk -> Color(0xFFE88D14)
        active -> palette.accentSaffron.copy(alpha = 0.22f)
        palette.isDark -> palette.elevatedSurface
        else -> Color(0xFFE6E0D2)
    }
    val iconColor = when {
        isPrimaryAccent && !palette.isEInk -> Color.White
        active -> palette.accentSaffron
        else -> MaterialTheme.colorScheme.onSurface
    }

    IndoToneSurface(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = CircleShape,
        containerColor = bgColor,
        hairlineColor = palette.goldHairline
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun VartaSegmentedTabBar(
    selectedTab: Int,
    personalUnread: Int,
    roomsUnread: Int,
    onSelectTab: (Int) -> Unit
) {
    val palette = LocalIndoPalette.current
    val trackColor = when {
        palette.isEInk -> palette.cardSurface
        palette.isDark -> palette.elevatedSurface
        else -> Color(0xFFE6E0D2)
    }
    val activePillColor = if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14)

    IndoToneSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = trackColor,
        hairlineColor = palette.goldHairline
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val tabWidth = maxWidth / 2
            val indicatorOffset by animateDpAsState(
                targetValue = if (selectedTab == 0) 0.dp else tabWidth,
                animationSpec = tween(durationMillis = 220, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
                label = "varta_tab_slide"
            )

            // Sliding Saffron/Marigold Amber active pill
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(activePillColor)
            )

            Row(modifier = Modifier.fillMaxWidth().height(42.dp)) {
                SegmentedTabLabel(
                    title = "Personal Chats",
                    unreadCount = personalUnread,
                    selected = selectedTab == 0,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("varta_tab_personal")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectTab(0) }
                )
                SegmentedTabLabel(
                    title = "Discussion Rooms",
                    unreadCount = roomsUnread,
                    selected = selectedTab == 1,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("varta_tab_rooms")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectTab(1) }
                )
            }
        }
    }
}

@Composable
private fun SegmentedTabLabel(
    title: String,
    unreadCount: Int,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalIndoPalette.current
    val textColor = if (selected) {
        if (palette.isEInk) MaterialTheme.colorScheme.onPrimary else Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (unreadCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) Color.White.copy(alpha = 0.25f)
                        else palette.accentSaffron.copy(alpha = 0.20f)
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$unreadCount",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) textColor else palette.accentSaffron
                )
            }
        }
    }
}

@Composable
private fun LiveStudySabhasSection(
    sabhas: List<LiveStudySabha>,
    cardTint: Color,
    hairlineColor: Color,
    onJoinSabha: (LiveStudySabha) -> Unit
) {
    val palette = LocalIndoPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "sabha_live")
    val livePulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = livePulse))
                )
                Text(
                    text = "Live Study Sabhas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE88D14).copy(alpha = if (palette.isEInk) 0.1f else 0.14f)
            ) {
                Text(
                    text = "Live Now",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (palette.isEInk) MaterialTheme.colorScheme.onSurface else Color(0xFFC26A06)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(sabhas, key = { it.id }) { sabha ->
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (pressed) 0.98f else 1f,
                    animationSpec = tween(150, easing = IndoMotion.OrganicDeceleration),
                    label = "sabha_press"
                )

                IndoToneSurface(
                    modifier = Modifier
                        .width(214.dp)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = interaction,
                            indication = LocalIndication.current
                        ) { onJoinSabha(sabha) },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = cardTint,
                    hairlineColor = hairlineColor
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Subtle geometric corner mandala motif
                        if (!palette.isEInk) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val corner = Offset(size.width * 0.92f, size.height * 0.14f)
                                val motifColor = palette.accentGold.copy(alpha = 0.13f)
                                drawCircle(motifColor, radius = 28.dp.toPx(), center = corner, style = Stroke(1.dp.toPx()))
                                drawCircle(motifColor, radius = 18.dp.toPx(), center = corner, style = Stroke(0.8.dp.toPx()))
                            }
                        }

                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = livePulse))
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.secondaryCopper.copy(alpha = 0.65f)
                                ) {
                                    Text(
                                        text = sabha.subjectTag,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (palette.isEInk) MaterialTheme.colorScheme.onSurface else palette.accentCyan
                                    )
                                }
                            }

                            Text(
                                text = sabha.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Peer avatar stack + "+N studying"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                    val stackColors = listOf(
                                        Color(0xFFE88D14),
                                        Color(0xFF0E7490),
                                        palette.accentGold
                                    )
                                    sabha.peerInitials.take(3).forEachIndexed { idx, initial ->
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (palette.isEInk) palette.elevatedSurface else stackColors[idx % stackColors.size])
                                                .border(1.2.dp, cardTint, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initial,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (palette.isEInk) MaterialTheme.colorScheme.onSurface else Color.White
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "+${sabha.activePeersCount} studying",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModularVartaThreadCard(
    chat: Chat,
    index: Int,
    tabKey: Int,
    cardTint: Color,
    hairlineColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150, easing = IndoMotion.OrganicDeceleration),
        label = "thread_press"
    )

    // Staggered fade-up entry (40ms delay per card)
    val entryAlpha = remember(tabKey, chat.id) { Animatable(0f) }
    val entrySlide = remember(tabKey, chat.id) { Animatable(18f) }

    LaunchedEffect(tabKey, chat.id) {
        delay((index * 40L).coerceAtMost(240L))
        entryAlpha.animateTo(1f, tween(220, easing = IndoMotion.OrganicDeceleration))
    }
    LaunchedEffect(tabKey, chat.id) {
        delay((index * 40L).coerceAtMost(240L))
        entrySlide.animateTo(0f, tween(220, easing = IndoMotion.OrganicDeceleration))
    }

    val isRoom = chat.type == "group"
    val ringBrush = if (chat.isOnline && !palette.isEInk) {
        Brush.linearGradient(listOf(Color(0xFFE88D14), palette.accentCyan))
    } else {
        Brush.linearGradient(listOf(hairlineColor, hairlineColor))
    }

    IndoToneSurface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entryAlpha.value
                translationY = entrySlide.value
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        containerColor = cardTint,
        hairlineColor = hairlineColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar / Room Emblem with ScholarAvatarBadge for Peers
            if (isRoom) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(width = if (chat.isOnline) 2.dp else 1.dp, brush = ringBrush, shape = CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(palette.secondaryCopper),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            tint = palette.accentSaffron,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (chat.isOnline && !palette.isEInk) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(1.8.dp, cardTint, CircleShape)
                        )
                    }
                }
            } else {
                ScholarAvatarBadge(
                    avatarSpec = chat.peerAvatarSpec ?: AvatarSpec(),
                    displayName = chat.name ?: "Scholar",
                    size = 48.dp,
                    isOnline = chat.isOnline
                )
            }

            // Core Info: Title + Vidya ID / Subject Tag Pill + 1-line Message Preview
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = chat.name ?: if (isRoom) "Study Room" else "Scholar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = palette.elevatedSurface
                    ) {
                        Text(
                            text = chat.subjectTag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accentCyan
                        )
                    }
                }

                if (chat.peerVidyaId.isNotBlank()) {
                    Text(
                        text = chat.peerVidyaId,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Metadata: Timestamp + High-Contrast Unread Pill
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = chat.timestampLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 22.dp, minHeight = 20.dp)
                            .clip(CircleShape)
                            .background(if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${chat.unreadCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (palette.isEInk) MaterialTheme.colorScheme.onPrimary else Color.White
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VartaDesignedEmptyState(
    isPersonalTab: Boolean,
    hasQuery: Boolean,
    cardTint: Color,
    hairlineColor: Color,
    onCreateClick: () -> Unit,
    onClearSearch: () -> Unit
) {
    val palette = LocalIndoPalette.current
    IndoToneSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(22.dp),
        containerColor = cardTint,
        hairlineColor = hairlineColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Centered Low-Opacity Sacred Geometry / Lotus-Mandala Line Illustration
            Canvas(modifier = Modifier.size(92.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension * 0.42f
                val strokeColor = if (palette.isEInk) Color(0xFF171717).copy(alpha = 0.25f) else palette.accentGold.copy(alpha = 0.42f)
                val stroke = Stroke(width = 1.3.dp.toPx())

                drawCircle(strokeColor, radius = r, center = c, style = stroke)
                drawCircle(strokeColor, radius = r * 0.62f, center = c, style = stroke)

                // 8-petal geometric lotus outline
                for (i in 0 until 8) {
                    val a = i * PI / 4.0
                    val tip = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
                    val left = Offset(c.x + (r * 0.5f * cos(a - 0.35)).toFloat(), c.y + (r * 0.5f * sin(a - 0.35)).toFloat())
                    val right = Offset(c.x + (r * 0.5f * cos(a + 0.35)).toFloat(), c.y + (r * 0.5f * sin(a + 0.35)).toFloat())
                    val petal = Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(left.x, left.y)
                        lineTo(tip.x, tip.y)
                        lineTo(right.x, right.y)
                        close()
                    }
                    drawPath(petal, color = strokeColor, style = stroke)
                }
            }

            Text(
                text = when {
                    hasQuery -> "No matching discussions found"
                    isPersonalTab -> "Start a Scholar Conversation"
                    else -> "Host a Study Sabha"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = when {
                    hasQuery -> "Try searching a different topic tag or create a brand-new study room."
                    else -> "Gather peers to trade notes, solve problems together, and keep your daily Vidya streak strong."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasQuery) {
                    OutlinedButton(
                        onClick = onClearSearch,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, hairlineColor)
                    ) {
                        Text("Clear Search", fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14),
                        contentColor = if (palette.isEInk) MaterialTheme.colorScheme.onPrimary else Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Discussion Room", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CreateVartaThreadDialog(
    initialIsGroupRoom: Boolean,
    onLookupPeer: suspend (String) -> VidyaIdentityCard?,
    onDismiss: () -> Unit,
    onCreateFromPeer: (peerCard: VidyaIdentityCard, subject: String) -> Unit,
    onCreate: (title: String, subject: String, isRoom: Boolean) -> Unit
) {
    val palette = LocalIndoPalette.current
    val scope = rememberCoroutineScope()
    var isRoom by remember { mutableStateOf(initialIsGroupRoom) }
    var title by remember { mutableStateOf("") }
    var vidyaQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var matchedPeer by remember { mutableStateOf<VidyaIdentityCard?>(null) }
    var searchAttempted by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("Chemistry") }
    val subjects = listOf("Chemistry", "Architecture", "Mathematics", "Physics", "Coding & AI", "General")

    Dialog(onDismissRequest = onDismiss) {
        MandalaUnfoldContainer {
            IndoToneSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                containerColor = palette.cardSurface,
                hairlineColor = palette.goldHairline
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isRoom) "Create Discussion Room" else "Find Scholar by Vidya ID",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14),
                        selectedLabelColor = if (palette.isEInk) MaterialTheme.colorScheme.onPrimary else Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isRoom,
                            onClick = { isRoom = true },
                            label = { Text("Discussion Room") },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = !isRoom,
                            onClick = { isRoom = false },
                            label = { Text("Personal Chat") },
                            colors = chipColors
                        )
                    }

                    if (!isRoom) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = vidyaQuery,
                                onValueChange = {
                                    vidyaQuery = it
                                    searchAttempted = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("varta_lookup_vidya_input"),
                                label = { Text("Vidya ID (handle#code)") },
                                placeholder = { Text("e.g. ananya#2048") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Button(
                                onClick = {
                                    isSearching = true
                                    searchAttempted = true
                                    scope.launch {
                                        matchedPeer = onLookupPeer(vidyaQuery)
                                        isSearching = false
                                    }
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("varta_lookup_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accentSaffron)
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Lookup")
                                }
                            }
                        }

                        // Quick suggested scholars
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PracticePeersCatalog.sampleScholars, key = { it.vidyaId }) { peer ->
                                AssistChip(
                                    onClick = {
                                        vidyaQuery = peer.vidyaId
                                        matchedPeer = peer
                                        searchAttempted = true
                                    },
                                    label = { Text(peer.vidyaId, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        if (matchedPeer != null) {
                            val peer = matchedPeer!!
                            IndoToneSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = palette.secondaryIndigo,
                                hairlineColor = palette.accentGold
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ScholarAvatarBadge(
                                        avatarSpec = peer.toAvatarSpec(),
                                        displayName = peer.displayName,
                                        size = 44.dp,
                                        isOnline = true
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(peer.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "${peer.vidyaId} • ${peer.level}",
                                            fontSize = 11.sp,
                                            color = palette.accentCyan
                                        )
                                    }
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = palette.accentGold)
                                }
                            }
                        } else if (searchAttempted && !isSearching) {
                            Text(
                                text = "No scholar found for \"$vidyaQuery\" — you can still enter a name below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Scholar Name (Manual)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Room Topic") },
                            placeholder = { Text("e.g., Thermodynamics Revision") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Text(
                        text = "Subject Tag",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { subject ->
                            FilterChip(
                                selected = selectedSubject == subject,
                                onClick = { selectedSubject = subject },
                                label = { Text(subject, fontSize = 12.sp) },
                                colors = chipColors
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (!isRoom && matchedPeer != null) {
                                    onCreateFromPeer(matchedPeer!!, selectedSubject)
                                } else {
                                    onCreate(title.ifBlank { vidyaQuery }, selectedSubject, isRoom)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14),
                                contentColor = if (palette.isEInk) MaterialTheme.colorScheme.onPrimary else Color.White
                            )
                        ) {
                            Text(
                                text = if (isRoom) "Create Room" else if (matchedPeer != null) "Message ${matchedPeer!!.vidyaHandle}" else "Start Chat",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    senderId: String,
    userProfile: UserProfile?,
    chatViewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by chatViewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val palette = LocalIndoPalette.current
    val effectiveSender = senderId.ifBlank { "demo_student_123" }
    val chatInfo = remember(chatId) { chatViewModel.getChatById(chatId) }

    LaunchedEffect(chatId, effectiveSender) {
        chatViewModel.loadMessages(chatId, effectiveSender)
    }

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentSaffron) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (chatInfo != null && chatInfo.type != "group") {
                                ScholarAvatarBadge(
                                    avatarSpec = chatInfo.peerAvatarSpec ?: AvatarSpec(),
                                    displayName = chatInfo.name ?: "Scholar",
                                    size = 38.dp,
                                    isOnline = chatInfo.isOnline
                                )
                            }
                            Column {
                                Text(
                                    text = chatInfo?.name ?: "Varta Discussion",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (chatInfo != null) {
                                    val vidyaPart = if (chatInfo.peerVidyaId.isNotBlank()) " • ${chatInfo.peerVidyaId}" else ""
                                    Text(
                                        text = "${chatInfo.subjectTag}$vidyaPart",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.accentSaffron
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.elevatedSurface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.reversed()) { msg ->
                        val isMe = msg.isUser || msg.senderId == effectiveSender
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            if (!isMe && (msg.senderName.isNotBlank() || msg.senderVidyaId.isNotBlank())) {
                                val senderCaption = listOfNotNull(
                                    msg.senderName.takeIf { it.isNotBlank() },
                                    msg.senderVidyaId.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                Text(
                                    text = senderCaption,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accentGold,
                                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                                )
                            }
                            IndoToneSurface(
                                shape = RoundedCornerShape(14.dp),
                                containerColor = if (isMe) palette.secondaryCopper else palette.cardSurface,
                                hairlineColor = palette.goldHairline,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message as ${userProfile?.effectiveVidyaId() ?: "scholar"}...") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accentSaffron,
                            unfocusedBorderColor = palette.goldHairline,
                            focusedContainerColor = palette.cardSurface,
                            unfocusedContainerColor = palette.cardSurface
                        )
                    )
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                chatViewModel.sendMessage(
                                    chatId = chatId,
                                    senderId = effectiveSender,
                                    text = inputText,
                                    senderName = userProfile?.name ?: "Scholar",
                                    senderVidyaId = userProfile?.effectiveVidyaId().orEmpty()
                                )
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(palette.accentSaffron, shape = RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
