package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.repository.ScholarIdentityRepository
import com.example.ui.screens.*
import com.example.ui.theme.EduPulseTheme
import com.example.utils.AppCheckHelper
import com.example.utils.SoundHelper
import com.example.viewmodel.AiTutorViewModel
import com.example.viewmodel.AuthGateState
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.GamesViewModel
import com.example.viewmodel.ThemeViewModel
import com.example.viewmodel.VidyaVaultViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val firestoreDb: FirebaseFirestore by lazy {
        val databaseId = applicationContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    private val authViewModel: AuthViewModel by viewModels {
        viewModelFactory {
            initializer {
                AuthViewModel(ScholarIdentityRepository(firestoreDb))
            }
        }
    }
    private val aiTutorViewModel: AiTutorViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels {
        viewModelFactory {
            initializer {
                ChatViewModel(firestoreDb)
            }
        }
    }
    private val gamesViewModel: GamesViewModel by viewModels()
    private val vidyaVaultViewModel: VidyaVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppCheckHelper.configureAppCheck(this, intent)

        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            val authGateState by authViewModel.authGateState.collectAsStateWithLifecycle()

            EduPulseTheme(themeMode = themeMode) {
                when (val gate = authGateState) {
                    AuthGateState.Loading -> {
                        LazyTabSkeleton(page = 0)
                    }

                    AuthGateState.Unauthenticated -> {
                        AuthScreen(authViewModel = authViewModel)
                    }

                    is AuthGateState.NeedsOnboarding -> {
                        OnboardingScreen(
                            profile = gate.profile,
                            isOfflinePreview = gate.isOfflinePreview,
                            authViewModel = authViewModel,
                            themeViewModel = themeViewModel,
                            currentThemeMode = themeMode
                        )
                    }

                    is AuthGateState.Authenticated -> {
                        AuthenticatedSanctuary(
                            userProfile = gate.profile,
                            isOfflinePreview = gate.isOfflinePreview,
                            authViewModel = authViewModel,
                            aiTutorViewModel = aiTutorViewModel,
                            themeViewModel = themeViewModel,
                            chatViewModel = chatViewModel,
                            gamesViewModel = gamesViewModel,
                            vidyaVaultViewModel = vidyaVaultViewModel,
                            themeMode = themeMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedSanctuary(
    userProfile: com.example.data.UserProfile,
    isOfflinePreview: Boolean,
    authViewModel: AuthViewModel,
    aiTutorViewModel: AiTutorViewModel,
    themeViewModel: ThemeViewModel,
    chatViewModel: ChatViewModel,
    gamesViewModel: GamesViewModel,
    vidyaVaultViewModel: VidyaVaultViewModel,
    themeMode: com.example.viewmodel.AppThemeMode
) {
    key(userProfile.uid) {
        val navController = rememberNavController()
        val initialVirtualPage = remember {
            (PAGER_VIRTUAL_COUNT / 2) - ((PAGER_VIRTUAL_COUNT / 2) % TAB_COUNT)
        }
        val pagerState = rememberPagerState(
            initialPage = initialVirtualPage,
            pageCount = { PAGER_VIRTUAL_COUNT }
        )
        val currentTab = pagerState.currentPage.toTabIndex()
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val loadedTabs = remember { mutableStateListOf(0) }
        var isFirstLaunch by remember { mutableStateOf(true) }

        fun navigatePagerToTab(targetTab: Int, animate: Boolean) {
            val delta = shortestTabDelta(pagerState.currentPage.toTabIndex(), targetTab)
            if (delta != 0) {
                val destination = pagerState.currentPage + delta
                coroutineScope.launch {
                    if (animate) {
                        pagerState.animateScrollToPage(
                            page = destination,
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = com.example.utils.IndoMotion.OrganicDeceleration
                            )
                        )
                    } else {
                        pagerState.scrollToPage(destination)
                    }
                }
            }
        }

        LaunchedEffect(currentTab) {
            if (!loadedTabs.contains(currentTab)) {
                loadedTabs.add(currentTab)
            }
            if (isFirstLaunch) {
                isFirstLaunch = false
            } else {
                SoundHelper.playSwipe(context)
            }
        }

        val palette = com.example.ui.theme.LocalIndoPalette.current
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.baseBg,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                if (currentRoute != null &&
                    currentRoute != "auth" &&
                    currentRoute != "profile" &&
                    !currentRoute.startsWith("lesson_") &&
                    !currentRoute.startsWith("chat_") &&
                    !currentRoute.startsWith("vidya_quest_")
                ) {
                    ConveyorBeltTaskbar(
                        currentPage = currentTab,
                        onTabSelected = { targetTab, animate ->
                            navigatePagerToTab(targetTab, animate)
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "main_pager",
                modifier = Modifier.padding(innerPadding),
                enterTransition = { com.example.utils.IndoMotion.mandalaEnterTransition() },
                exitTransition = { com.example.utils.IndoMotion.mandalaExitTransition() },
                popEnterTransition = { com.example.utils.IndoMotion.mandalaEnterTransition() },
                popExitTransition = { com.example.utils.IndoMotion.mandalaExitTransition() }
            ) {
                composable("main_pager") {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 0
                    ) { virtualPage ->
                        val page = virtualPage.toTabIndex()
                        val targetTab = pagerState.targetPage.toTabIndex()
                        if (page == currentTab || page == targetTab || loadedTabs.contains(page)) {
                            when (page) {
                                0 -> HomeScreen(
                                    userProfile = userProfile,
                                    onNavigateToCourse = { courseId -> navController.navigate("lesson_$courseId") },
                                    onNavigateToAiTutor = { navigatePagerToTab(2, animate = true) },
                                    onNavigateToCommunity = { navigatePagerToTab(3, animate = true) },
                                    onNavigateToProfile = { navController.navigate("profile") },
                                    vidyaViewModel = vidyaVaultViewModel,
                                    onResumeQuestLevel = { realmId, level ->
                                        navController.navigate("vidya_quest_${realmId.name}_${level.id}")
                                    },
                                    onLaunchDailyChallenge = { prompt ->
                                        aiTutorViewModel.sendMessage(prompt)
                                        navigatePagerToTab(2, animate = true)
                                    }
                                )
                                1 -> CoursesScreen(
                                    onNavigateToCourse = { courseId -> navController.navigate("lesson_$courseId") },
                                    vidyaViewModel = vidyaVaultViewModel,
                                    authViewModel = authViewModel
                                )
                                2 -> AiTutorScreen(
                                    aiViewModel = aiTutorViewModel,
                                    vidyaViewModel = vidyaVaultViewModel
                                )
                                3 -> MessagingLandingScreen(
                                    userId = if (isOfflinePreview) "" else userProfile.uid,
                                    userProfile = userProfile,
                                    authViewModel = authViewModel,
                                    chatViewModel = chatViewModel,
                                    onNavigateToChat = { chatId -> navController.navigate("chat_$chatId") }
                                )
                                4 -> GamesScreen(
                                    userProfile = userProfile,
                                    gamesViewModel = gamesViewModel
                                )
                            }
                        } else {
                            LazyTabSkeleton(page = page)
                        }
                    }
                }

                composable(
                    route = "lesson_{courseId}",
                    arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getString("courseId") ?: "math_101"
                    LessonDetailScreen(
                        courseId = courseId,
                        authViewModel = authViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "vidya_quest_{realmName}_{levelId}",
                    arguments = listOf(
                        navArgument("realmName") { type = NavType.StringType },
                        navArgument("levelId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val realmName = backStackEntry.arguments?.getString("realmName") ?: com.example.data.RealmId.MATHEMATICS.name
                    val levelId = backStackEntry.arguments?.getString("levelId") ?: ""
                    val realmId = runCatching { com.example.data.RealmId.valueOf(realmName) }
                        .getOrDefault(com.example.data.RealmId.MATHEMATICS)
                    val realm = com.example.data.VidyaRepository.getRealm(realmId)
                    val level = realm.levels.firstOrNull { it.id == levelId } ?: realm.levels.first()
                    if (level.isBossBattle) {
                        BossBattleArenaScreen(
                            level = level,
                            realm = realm,
                            vidyaViewModel = vidyaVaultViewModel,
                            authViewModel = authViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        LevelQuestRunnerScreen(
                            level = level,
                            realm = realm,
                            vidyaViewModel = vidyaVaultViewModel,
                            authViewModel = authViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable("chat_{chatId}") { backStackEntry ->
                    ChatDetailScreen(
                        chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                        senderId = if (isOfflinePreview) "" else userProfile.uid,
                        userProfile = userProfile,
                        chatViewModel = chatViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        userProfile = userProfile,
                        isOfflinePreview = isOfflinePreview,
                        authViewModel = authViewModel,
                        themeViewModel = themeViewModel,
                        currentThemeMode = themeMode,
                        onBack = { navController.popBackStack() },
                        onSignedOut = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

data class BeltTabItem(
    val index: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

private const val TAB_COUNT = 5
private const val PAGER_VIRTUAL_COUNT = 10_000
private const val VIRTUAL_ITEM_COUNT = 10_000

private fun Int.toTabIndex(): Int = ((this % TAB_COUNT) + TAB_COUNT) % TAB_COUNT

private fun shortestTabDelta(fromTab: Int, toTab: Int): Int {
    val diff = toTab - fromTab
    return when {
        diff > TAB_COUNT / 2 -> diff - TAB_COUNT
        diff < -(TAB_COUNT / 2) -> diff + TAB_COUNT
        else -> diff
    }
}

@Composable
fun ConveyorBeltTaskbar(
    currentPage: Int,
    onTabSelected: (targetTab: Int, animate: Boolean) -> Unit
) {
    val context = LocalContext.current
    val palette = com.example.ui.theme.LocalIndoPalette.current
    val beltScope = rememberCoroutineScope()
    val tabs = remember(palette) {
        listOf(
            BeltTabItem(0, "Gyan", Icons.Default.Dashboard, palette.accentSaffron),
            BeltTabItem(1, "Vidya", Icons.Default.AutoStories, palette.accentGold),
            BeltTabItem(2, "Cosmos", Icons.Default.AutoAwesome, palette.accentCyan),
            BeltTabItem(3, "Varta", Icons.Default.ChatBubbleOutline, Color(0xFFE88D14)),
            BeltTabItem(4, "Khel", Icons.Default.SportsEsports, palette.accentVermilion)
        )
    }

    val startIndex = remember {
        (VIRTUAL_ITEM_COUNT / 2) - ((VIRTUAL_ITEM_COUNT / 2) % tabs.size) + currentPage
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState, snapPosition = SnapPosition.Center)

    val currentCenterVirtualIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            val closest = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2f) - viewportCenter)
            }
            closest?.index
        }
    }

    // Active tab in real-time as conveyor belt is sliding
    val activeTab = remember(currentCenterVirtualIndex) {
        currentCenterVirtualIndex?.let { idx ->
            ((idx % tabs.size) + tabs.size) % tabs.size
        } ?: currentPage
    }

    var isTapScrollingBelt by remember { mutableStateOf(false) }
    var tapScrollToken by remember { mutableIntStateOf(0) }
    var isSyncingFromPager by remember { mutableStateOf(false) }

    // Notify instantly as the user drags/flings the conveyor belt
    var lastNotifiedTab by remember { mutableIntStateOf(currentPage) }
    LaunchedEffect(activeTab) {
        if (activeTab != lastNotifiedTab) {
            lastNotifiedTab = activeTab
            if (!isTapScrollingBelt && !isSyncingFromPager) {
                onTabSelected(activeTab, false)
            }
        }
    }

    // When the screen pager is swiped or switched externally, smoothly rotate conveyor belt
    LaunchedEffect(currentPage) {
        lastNotifiedTab = currentPage
        if (!listState.isScrollInProgress && !isTapScrollingBelt) {
            val currentCenter = currentCenterVirtualIndex ?: startIndex
            val currentMod = ((currentCenter % tabs.size) + tabs.size) % tabs.size
            val shortestDiff = shortestTabDelta(currentMod, currentPage)
            if (shortestDiff != 0) {
                isSyncingFromPager = true
                try {
                    listState.animateScrollToItem(currentCenter + shortestDiff)
                } finally {
                    isSyncingFromPager = false
                }
            }
        }
    }

    val currentTabColor = if (palette.isEInk) {
        palette.accentSaffron
    } else {
        tabs.getOrNull(activeTab)?.color ?: palette.accentSaffron
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = palette.elevatedSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, palette.goldHairline)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            contentAlignment = Alignment.Center
        ) {
            val itemSlotWidth = 72.dp
            val centerPadding = (maxWidth - itemSlotWidth) / 2

            // Subtle atmospheric glow behind active center aperture
            val infiniteTransition = rememberInfiniteTransition(label = "bubble_glow")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.03f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            if (!palette.isEInk) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    currentTabColor.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Center targeting ring with 10% accent focus (amber-gold & celestial cyan for Cosmos)
            Surface(
                modifier = Modifier.size(62.dp),
                shape = CircleShape,
                color = currentTabColor.copy(alpha = 0.14f),
                border = BorderStroke(
                    width = if (activeTab == 2 && !palette.isEInk) 1.6.dp else 1.2.dp,
                    brush = if (activeTab == 2 && !palette.isEInk) {
                        Brush.sweepGradient(listOf(palette.accentGold, palette.accentCyan, Color(0xFFE88D14), palette.accentGold))
                    } else {
                        Brush.linearGradient(listOf(currentTabColor.copy(alpha = 0.75f), currentTabColor.copy(alpha = 0.75f)))
                    }
                )
            ) {}

            // Infinite Rotating Conveyor Belt
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                flingBehavior = snapFlingBehavior,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(horizontal = centerPadding),
                userScrollEnabled = true
            ) {
                items(VIRTUAL_ITEM_COUNT) { virtualIndex ->
                    val tabItem = tabs[((virtualIndex % tabs.size) + tabs.size) % tabs.size]
                    val isCentered = activeTab == tabItem.index
                    val activeAccent = if (palette.isEInk) palette.accentSaffron else tabItem.color

                    val slotInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .width(itemSlotWidth)
                            .height(82.dp)
                            .testTag("belt_tab_${tabItem.label.lowercase()}")
                            .clickable(
                                interactionSource = slotInteraction,
                                indication = null
                            ) {
                                SoundHelper.playWoodyTock(context)
                                onTabSelected(tabItem.index, true)
                                val myToken = ++tapScrollToken
                                beltScope.launch {
                                    isTapScrollingBelt = true
                                    try {
                                        listState.animateScrollToItem(virtualIndex)
                                    } finally {
                                        if (tapScrollToken == myToken) {
                                            isTapScrollingBelt = false
                                        }
                                    }
                                }
                            }
                            .graphicsLayer {
                                val layoutInfo = listState.layoutInfo
                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == virtualIndex }
                                if (itemInfo != null && viewportCenter > 0f) {
                                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                    val distance = kotlin.math.abs(itemCenter - viewportCenter)
                                    val maxDist = itemSlotWidth.toPx() * 1.8f
                                    val fraction = (1f - (distance / maxDist)).coerceIn(0f, 1f)

                                    val s = 0.72f + 0.58f * fraction
                                    scaleX = s
                                    scaleY = s
                                    alpha = 0.45f + 0.55f * fraction

                                    val sign = if (itemCenter > viewportCenter) -1f else 1f
                                    rotationY = (distance / maxDist).coerceIn(0f, 1f) * 18f * sign
                                } else {
                                    scaleX = 0.72f
                                    scaleY = 0.72f
                                    alpha = 0.45f
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCentered) {
                                        activeAccent
                                    } else {
                                        palette.cardSurface
                                    }
                                )
                                .border(
                                    width = if (isCentered) 1.dp else 0.8.dp,
                                    color = if (isCentered) Color.White.copy(alpha = 0.65f) else palette.goldHairline,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tabItem.icon,
                                contentDescription = tabItem.label,
                                tint = if (isCentered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LazyTabSkeleton(page: Int) {
    val palette = com.example.ui.theme.LocalIndoPalette.current
    com.example.ui.theme.IndoAtmosphericBackdrop(ambientGlowColor = palette.accentGold) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(38.dp),
                    color = palette.accentSaffron,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Initializing realm...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

