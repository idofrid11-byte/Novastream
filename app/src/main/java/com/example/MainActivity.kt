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
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val screenDestination by viewModel.screenDestination.collectAsState()

    // Native Back Handler
    BackHandler(enabled = screenDestination !is ScreenDestination.Tabs || currentTab != AppTab.DISCOVER) {
        viewModel.navigateBack()
    }

    val isPlayerScreen = screenDestination is ScreenDestination.Player

    Scaffold(
        bottomBar = {
            if (!isPlayerScreen) {
                NavigationBar(
                    containerColor = StreamSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    val tabs = listOf(
                        AppTab.DISCOVER to ("Home" to Icons.Default.Explore),
                        AppTab.STREAMING to ("Networks" to Icons.Default.Tv),
                        AppTab.SEARCH to ("Search" to Icons.Default.Search),
                        AppTab.WATCHLIST to ("Watchlist" to Icons.Default.Bookmark),
                        AppTab.SETTINGS to ("Settings" to Icons.Default.Settings)
                    )

                    tabs.forEach { (tab, info) ->
                        val (label, icon) = info
                        val isSelected = (currentTab == tab ||
                                (tab == AppTab.WATCHLIST && currentTab == AppTab.LIBRARY) ||
                                (tab == AppTab.SETTINGS && (currentTab == AppTab.ACCOUNT || currentTab == AppTab.ADDONS)))
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
                                selectedTextColor = WuPlayCyan,
                                indicatorColor = WuPlayPurple,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = StreamBg,
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
                        AppTab.DISCOVER -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id, type ->
                                    viewModel.navigateToDetail(id, type)
                                }
                            )
                        }

                        AppTab.STREAMING -> {
                            StreamingServicesScreen(
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

                        AppTab.WATCHLIST, AppTab.LIBRARY -> {
                            WatchlistScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id, type ->
                                    viewModel.navigateToDetail(id, type)
                                }
                            )
                        }

                        AppTab.SETTINGS, AppTab.ACCOUNT, AppTab.ADDONS -> {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
