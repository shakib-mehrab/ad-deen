package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.screens.*
import com.example.ui.theme.AdDinTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdDinTheme {
                AdDinApp()
            }
        }
    }
}

@Composable
fun AdDinApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val lang = settings.language

    val navController = rememberNavController()

    // Request location and notification permissions at startup
    val startupPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.requestGpsLocation { }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                // Already granted, auto-detect GPS on startup
                viewModel.requestGpsLocation { }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            startupPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: "home"

    var azkharInitialTab by remember { mutableIntStateOf(0) }
    var showLoadingScreen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }

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

    LaunchedEffect(Unit) {
        // Ensure custom sounds folder exists on device filesystem
        com.example.alarm.SoundHelper.ensureCustomSoundFolders(context)
    }

    if (showLoadingScreen) {
        LoadingScreen(onFinished = { showLoadingScreen = false })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = com.example.ui.theme.AppColors.TextPrimary,
                        contentColor = com.example.ui.theme.AppColors.Surface,
                        actionColor = com.example.ui.theme.AppColors.Primary,
                        modifier = Modifier.testTag("undo_snackbar")
                    )
                }
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = com.example.ui.theme.AppColors.Divider
                    )
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_nav_bar"),
                        containerColor = com.example.ui.theme.AppColors.Background,
                        tonalElevation = 0.dp
                    ) {
                        val navItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.example.ui.theme.AppColors.OnPrimary,
                            selectedTextColor = com.example.ui.theme.AppColors.OnPrimary,
                            indicatorColor = com.example.ui.theme.AppColors.Primary,
                            unselectedIconColor = com.example.ui.theme.AppColors.TextSecondary,
                            unselectedTextColor = com.example.ui.theme.AppColors.TextSecondary
                        )

                        // Home
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = {
                                navController.navigate("home") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = navItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = Strings.t("nav_home", lang),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = Strings.t("nav_home", lang),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (currentRoute == "home") FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            alwaysShowLabel = true,
                            modifier = Modifier.testTag("nav_item_home")
                        )

                        // Prayer Times (ওয়াক্ত)
                        NavigationBarItem(
                            selected = currentRoute == "prayer_times",
                            onClick = {
                                navController.navigate("prayer_times") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = navItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == "prayer_times") Icons.Filled.AccessTimeFilled else Icons.Outlined.AccessTime,
                                    contentDescription = Strings.t("nav_prayer", lang),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = Strings.t("nav_prayer", lang),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (currentRoute == "prayer_times") FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            alwaysShowLabel = true,
                            modifier = Modifier.testTag("nav_item_prayer_times")
                        )

                        // Tracker (আমল)
                        NavigationBarItem(
                            selected = currentRoute == "tracker",
                            onClick = {
                                navController.navigate("tracker") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = navItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == "tracker") Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                    contentDescription = Strings.t("nav_tracker", lang),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = Strings.t("nav_tracker", lang),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (currentRoute == "tracker") FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            alwaysShowLabel = true,
                            modifier = Modifier.testTag("nav_item_tracker")
                        )

                        // Azkhar (আযকার)
                        NavigationBarItem(
                            selected = currentRoute == "azkhar",
                            onClick = {
                                azkharInitialTab = 0
                                navController.navigate("azkhar") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = navItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == "azkhar") Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                    contentDescription = Strings.t("nav_azkhar", lang),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = Strings.t("nav_azkhar", lang),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (currentRoute == "azkhar") FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            alwaysShowLabel = true,
                            modifier = Modifier.testTag("nav_item_azkhar")
                        )

                        // More (আরও)
                        NavigationBarItem(
                            selected = currentRoute == "more",
                            onClick = {
                                navController.navigate("more") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = navItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == "more") Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                                    contentDescription = Strings.t("nav_more", lang),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = Strings.t("nav_more", lang),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (currentRoute == "more") FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            alwaysShowLabel = true,
                            modifier = Modifier.testTag("nav_item_more")
                        )
                    }
                }
            }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToPrayerTimes = {
                        navController.navigate("prayer_times") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTracker = {
                        navController.navigate("tracker") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAzkhar = { tabIndex ->
                        azkharInitialTab = tabIndex
                        navController.navigate("azkhar") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("prayer_times") {
                PrayerTimesScreen(viewModel = viewModel)
            }

            composable("tracker") {
                TrackerScreen(viewModel = viewModel)
            }

            composable("azkhar") {
                AzkharScreen(viewModel = viewModel, initialTab = azkharInitialTab)
            }

            composable("more") {
                MoreScreen(viewModel = viewModel)
            }
        }
    }
}
}

