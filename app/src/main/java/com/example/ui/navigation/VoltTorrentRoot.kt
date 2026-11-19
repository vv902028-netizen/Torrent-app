package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.components.AddTorrentSheet
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.ControlsScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.TorrentDetailsScreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainTorrentViewModel

private data class TopTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val tag: String
)

@Composable
fun VoltTorrentRoot(
    viewModel: MainTorrentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val downloadsState by viewModel.downloadsUiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.globalTelemetry.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val addSheetState by viewModel.addSheetState.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val torrentFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                if (!addSheetState.isVisible) {
                    viewModel.openAddSheet()
                }
                viewModel.onTorrentFileSelected(uri)
            }
        }
    )

    val launchFilePicker: () -> Unit = {
        torrentFileLauncher.launch(arrayOf("application/x-bittorrent", "application/octet-stream", "*/*"))
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevelRoute = currentDestination?.hasRoute<TorrentDetailsRoute>() != true

    val topTabs = remember {
        listOf(
            TopTabItem(
                label = "Загрузки",
                selectedIcon = Icons.Filled.Download,
                unselectedIcon = Icons.Outlined.Download,
                route = DownloadsRoute,
                tag = "nav_downloads"
            ),
            TopTabItem(
                label = "Каталог",
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                route = CatalogRoute,
                tag = "nav_catalog"
            ),
            TopTabItem(
                label = "Лимиты",
                selectedIcon = Icons.Filled.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                route = ControlsRoute,
                tag = "nav_controls"
            )
        )
    }

    val navigateTopLevel: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = OledBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = ObsidianCard,
                        contentColor = TextPrimary,
                        actionColor = NeonCyan,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            floatingActionButton = {
                if (currentDestination?.hasRoute<DownloadsRoute>() == true || currentDestination == null) {
                    FloatingActionButton(
                        onClick = { viewModel.openAddSheet() },
                        containerColor = NeonCyan,
                        contentColor = OledBlack,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("fab_add_torrent")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Добавить торрент"
                        )
                    }
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useRail) {
                    Box {
                        HorizontalDivider(thickness = 1.dp, color = HairlineBorder)
                        NavigationBar(
                            containerColor = CarbonSurface,
                            tonalElevation = 0.dp
                        ) {
                            topTabs.forEach { tab ->
                                val selected = when (tab.route) {
                                    is DownloadsRoute -> currentDestination?.hasRoute<DownloadsRoute>() == true
                                    is CatalogRoute -> currentDestination?.hasRoute<CatalogRoute>() == true
                                    is ControlsRoute -> currentDestination?.hasRoute<ControlsRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    modifier = Modifier.testTag(tab.tag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyan.copy(alpha = 0.14f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useRail) {
                    NavigationRail(
                        containerColor = CarbonSurface,
                        contentColor = TextPrimary
                    ) {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is DownloadsRoute -> currentDestination?.hasRoute<DownloadsRoute>() == true
                                is CatalogRoute -> currentDestination?.hasRoute<CatalogRoute>() == true
                                is ControlsRoute -> currentDestination?.hasRoute<ControlsRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                modifier = Modifier.testTag(tab.tag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan.copy(alpha = 0.14f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = DownloadsRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<DownloadsRoute> {
                        DownloadsScreen(
                            uiState = downloadsState,
                            onSelectFilter = viewModel::setFilter,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onOpenTorrentDetails = { hash ->
                                navController.navigate(TorrentDetailsRoute(hash))
                            },
                            onTogglePauseResume = viewModel::togglePauseResume,
                            onDeleteTorrent = viewModel::removeTorrent,
                            onPauseAll = viewModel::pauseAll,
                            onResumeAll = viewModel::resumeAll,
                            onOpenAddMagnet = { viewModel.openAddSheet() },
                            onPickTorrentFile = launchFilePicker,
                            onOpenCatalog = { navigateTopLevel(CatalogRoute) },
                            onOpenControls = { navigateTopLevel(ControlsRoute) },
                            onReconnectNetwork = viewModel::reconnectNetworkAndTrackers
                        )
                    }

                    composable<CatalogRoute> {
                        CatalogScreen(
                            entries = viewModel.catalogItems,
                            selectedCategory = selectedCategory,
                            existingHashes = downloadsState.allInfoHashes,
                            onSelectCategory = viewModel::setCatalogCategory,
                            onAddCatalogEntry = { entry ->
                                viewModel.addFromCatalog(entry)
                            }
                        )
                    }

                    composable<ControlsRoute> {
                        ControlsScreen(
                            telemetry = telemetry,
                            settings = settings,
                            timerState = timerState,
                            downloadDirPath = viewModel.downloadDirectoryPath,
                            onSetDownloadLimit = viewModel::setDownloadLimit,
                            onSetUploadLimit = viewModel::setUploadLimit,
                            onStartTimer = viewModel::startTimer,
                            onCancelTimer = viewModel::cancelTimer,
                            onSetTimerAction = viewModel::setTimerAction,
                            onSetStopOnComplete = viewModel::setStopOnComplete,
                            onSetWifiOnly = viewModel::setWifiOnly,
                            onSetDhtEnabled = viewModel::setDhtEnabled,
                            onSetPexEnabled = viewModel::setPexEnabled,
                            onSetLsdEnabled = viewModel::setLsdEnabled,
                            onSetMaxConnections = viewModel::setMaxConnections
                        )
                    }

                    composable<TorrentDetailsRoute> { backStack ->
                        val route = backStack.toRoute<TorrentDetailsRoute>()
                        val torrent by viewModel.observeTorrent(route.infoHash)
                            .collectAsStateWithLifecycle(initialValue = null)
                        TorrentDetailsScreen(
                            torrent = torrent,
                            onBack = { navController.popBackStack() },
                            onTogglePauseResume = viewModel::togglePauseResume,
                            onDeleteTorrent = viewModel::removeTorrent,
                            onSetFilePriority = viewModel::setFilePriority,
                            onForceReannounce = viewModel::forceReannounce,
                            onAddTracker = viewModel::addTracker
                        )
                    }
                }
            }
        }
    }

    AddTorrentSheet(
        state = addSheetState,
        onDismiss = viewModel::closeAddSheet,
        onMagnetChanged = viewModel::onMagnetInputChanged,
        onPickTorrentFile = launchFilePicker,
        onToggleFileSelection = viewModel::togglePreviewFileSelection,
        onSubmit = viewModel::submitAddSheet,
        onOpenCatalog = { navigateTopLevel(CatalogRoute) }
    )
}
