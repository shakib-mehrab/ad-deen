package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.data.UiPrefs
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.components.pressScale
import com.example.ui.screens.*
import com.example.ui.theme.AdDinTheme
import com.example.ui.theme.AppColors
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val nightByPrayerTime by viewModel.isNightTime.collectAsState()
            val settings by viewModel.settings.collectAsState()
            // Warm night palette between Maghrib and Fajr, or whenever the phone is in dark mode.
            AdDinTheme(isNight = nightByPrayerTime || isSystemInDarkTheme(), lang = settings.language) {
                AdDinApp(viewModel)
            }
        }
    }
}

private data class NavDest(val route: String, val labelKey: String, val selected: ImageVector, val unselected: ImageVector)

private val NAV_DESTINATIONS = listOf(
    NavDest("home", "nav_home", Icons.Filled.Home, Icons.Outlined.Home),
    NavDest("prayer_times", "nav_prayer", Icons.Filled.AccessTimeFilled, Icons.Outlined.AccessTime),
    NavDest("tracker", "nav_tracker", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline),
    NavDest("azkhar", "nav_azkhar", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    NavDest("more", "nav_more", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
)

private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AdDinApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val lang = settings.language
    val uiPrefs = remember { UiPrefs(context) }

    var showLoadingScreen by rememberSaveable { mutableStateOf(true) }
    var onboardingDone by rememberSaveable { mutableStateOf(uiPrefs.onboardingDone) }
    var moreInitialSubScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var azkharInitialTab by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()

    // Refresh GPS silently on launch when the user chose GPS and already granted permission.
    // (First-run permission requests now happen in onboarding, with an explanation.)
    LaunchedEffect(onboardingDone) {
        if (!onboardingDone) return@LaunchedEffect
        com.example.alarm.SoundHelper.ensureCustomSoundFolders(context)
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted && viewModel.settings.value.locationMode == "gps") viewModel.requestGpsLocation { }
    }

    // Undo snackbar (~5 s) for prayer / Witr / Tahajjud / Duha / Quran changes on Home and Tracker.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.undoEvents.collectLatest { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = kotlinx.coroutines.withTimeoutOrNull(5_000L) {
                snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = if (viewModel.settings.value.language == "bn") "আনডু" else "Undo",
                    duration = SnackbarDuration.Indefinite
                )
            }
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
        }
    }

    val stage = when {
        showLoadingScreen -> 0
        !onboardingDone -> 1
        else -> 2
    }
    AnimatedContent(
        targetState = stage,
        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(350)) },
        label = "app_stage"
    ) { s ->
        when (s) {
            0 -> LoadingScreen(onFinished = { showLoadingScreen = false })
            1 -> OnboardingScreen(viewModel) { openSignIn ->
                uiPrefs.onboardingDone = true
                if (openSignIn) moreInitialSubScreen = "auth"
                onboardingDone = true
            }
            else -> MainScaffold(
                viewModel = viewModel,
                lang = lang,
                navController = navController,
                snackbarHostState = snackbarHostState,
                azkharInitialTab = azkharInitialTab,
                onAzkharTab = { azkharInitialTab = it },
                moreInitialSubScreen = moreInitialSubScreen,
                onMoreOpened = { moreInitialSubScreen = null }
            )
        }
    }
}

@Composable
private fun MainScaffold(
    viewModel: MainViewModel,
    lang: String,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    azkharInitialTab: Int,
    onAzkharTab: (Int) -> Unit,
    moreInitialSubScreen: String?,
    onMoreOpened: () -> Unit
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "home"

    // After onboarding with "Sign in", land directly on the account screen.
    LaunchedEffect(moreInitialSubScreen) {
        if (moreInitialSubScreen != null) navController.navigateTab("more")
    }

    Box(Modifier.fillMaxSize().background(AppColors.BackgroundBrush)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = if (AppColors.IsNight) AppColors.SurfaceElevated else Color(0xFF2B2118),
                        contentColor = Color.White,
                        actionColor = AppColors.Primary,
                        modifier = Modifier.testTag("undo_snackbar")
                    )
                }
            },
            bottomBar = {
                FloatingNavBar(currentRoute, lang) { route ->
                    if (route == "azkhar") onAzkharTab(0)
                    navController.navigateTab(route)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(260)) },
                popExitTransition = { fadeOut(tween(180)) }
            ) {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToPrayerTimes = { navController.navigateTab("prayer_times") },
                        onNavigateToTracker = { navController.navigateTab("tracker") },
                        onNavigateToAzkhar = { tab -> onAzkharTab(tab); navController.navigateTab("azkhar") },
                        onNavigateToMore = { navController.navigateTab("more") }
                    )
                }
                composable("prayer_times") { PrayerTimesScreen(viewModel = viewModel) }
                composable("tracker") { TrackerScreen(viewModel = viewModel) }
                composable("azkhar") { AzkharScreen(viewModel = viewModel, initialTab = azkharInitialTab) }
                composable("more") {
                    MoreScreen(viewModel = viewModel, initialSubScreen = moreInitialSubScreen)
                    LaunchedEffect(Unit) { onMoreOpened() }
                }
            }
        }
    }
}

/** Flat bottom bar (reference style): white, hairline top edge, brown active icon + bold label. */
@Composable
private fun FloatingNavBar(currentRoute: String, lang: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(AppColors.Surface).testTag("bottom_nav_bar")) {
        HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NAV_DESTINATIONS.forEach { d ->
                val selected = currentRoute == d.route
                val interaction = remember { MutableInteractionSource() }
                val label = Strings.t(d.labelKey, lang)
                val tint by androidx.compose.animation.animateColorAsState(if (selected) AppColors.Secondary else AppColors.TextSecondary.copy(alpha = 0.8f), label = "tint")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pressScale(interaction, 0.94f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(interactionSource = interaction, indication = ripple(), onClick = { onSelect(d.route) })
                        .padding(vertical = 6.dp)
                        .testTag("nav_item_${d.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (selected) d.selected else d.unselected,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = label,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                        color = tint
                    )
                }
            }
        }
    }
}
