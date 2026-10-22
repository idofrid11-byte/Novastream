package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle stremio:// intent if launched via deep link
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme.equals("stremio", ignoreCase = true) || data.path?.endsWith("manifest.json") == true) {
            viewModel.installAddon(data.toString())
            viewModel.switchTab(AppTab.SETTINGS)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.screenDestination.value is ScreenDestination.Player) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    // Ignored on hardware without PiP
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val screenDestination by viewModel.screenDestination.collectAsState()

    // Native Back Handler
    BackHandler(enabled = screenDestination !is ScreenDestination.Tabs || currentTab != AppTab.BOARD) {
        viewModel.navigateBack()
    }

    val isPlayerScreen = screenDestination is ScreenDestination.Player

    Scaffold(
        bottomBar = {
            if (!isPlayerScreen) {
                NavigationBar(
                    containerColor = StremioDark,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    val tabs = listOf(
                        AppTab.BOARD to ("Board" to Icons.Default.Home),
                        AppTab.DISCOVER to ("Discover" to Icons.Default.Explore),
                        AppTab.LIBRARY to ("Library" to Icons.Default.Bookmark),
                        AppTab.SEARCH to ("Search" to Icons.Default.Search),
                        AppTab.ADDONS to ("Addons" to Icons.Default.Extension),
                        AppTab.SETTINGS to ("Settings" to Icons.Default.Settings)
                    )

                    tabs.forEach { (tab, info) ->
                        val (label, icon) = info
                        val isSelected = (currentTab == tab ||
                                (tab == AppTab.BOARD && currentTab == AppTab.BOARD) ||
                                (tab == AppTab.DISCOVER && currentTab == AppTab.STREAMING) ||
                                (tab == AppTab.LIBRARY && currentTab == AppTab.WATCHLIST) ||
                                (tab == AppTab.SETTINGS && currentTab == AppTab.ACCOUNT))
                                && screenDestination is ScreenDestination.Tabs

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.switchTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = StremioPurpleLight,
                                indicatorColor = StremioPurple,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = StremioBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPlayerScreen) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            when (val dest = screenDestination) {
                is ScreenDestination.Player -> {
                    PlayerScreen(
                        stream = dest.stream,
                        title = dest.title,
                        subtitle = dest.subtitle,
                        itemId = dest.itemId,
                        startPositionMs = dest.startPositionMs,
                        meta = dest.meta,
                        episode = dest.episode,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is ScreenDestination.Detail -> {
                    DetailScreen(
                        id = dest.id,
                        type = dest.type,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is ScreenDestination.Tabs -> {
                    when (currentTab) {
                        AppTab.BOARD -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id, type ->
                                    viewModel.navigateToDetail(id, type)
                                }
                            )
                        }

                        AppTab.DISCOVER, AppTab.STREAMING -> {
                            StreamingServicesScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id, type ->
                                    viewModel.navigateToDetail(id, type)
                                }
                            )
                        }

                        AppTab.LIBRARY, AppTab.WATCHLIST -> {
                            WatchlistScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id, type ->
                                    viewModel.navigateToDetail(id, type)
                                }
                            )
                        }

                        AppTab.SEARCH -> {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id, type ->
                                    viewModel.navigateToDetail(id, type)
                                }
                            )
                        }

                        AppTab.ADDONS -> {
                            AddonsScreen(viewModel = viewModel)
                        }

                        AppTab.SETTINGS, AppTab.ACCOUNT -> {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
