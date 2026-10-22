package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.LibraryItem
import com.example.data.model.WatchStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WatchlistScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (id: String, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryItems by viewModel.libraryItems.collectAsState()
    val isSyncing by viewModel.isSyncingCloud.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTabFilter by remember { mutableStateOf("All") }

    val filteredItems = remember(libraryItems, selectedTabFilter) {
        when (selectedTabFilter) {
            "Movies" -> libraryItems.filter { it.type == "movie" }
            "Series" -> libraryItems.filter { it.type == "series" }
            "Watching" -> libraryItems.filter { it.status == WatchStatus.WATCHING }
            "Plan to Watch" -> libraryItems.filter { it.status == WatchStatus.PLAN_TO_WATCH }
            "Completed" -> libraryItems.filter { it.status == WatchStatus.COMPLETED }
            else -> libraryItems
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StreamSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "WuPlay",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Watchlist",
                                color = WuPlayCyan,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${libraryItems.size} saved titles • ${if (currentUser != null) "Cloud synced" else "Local device"}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentUser != null) {
                            IconButton(
                                onClick = { viewModel.syncLibraryWithCloud() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StreamSurfaceVariant)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(color = WuPlayCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = WuPlayCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.switchTab(AppTab.DISCOVER) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WuPlayPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Titles",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Watchlist Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("All", "Movies", "Series", "Watching", "Plan to Watch", "Completed")
                    tabs.forEach { tab ->
                        val isSelected = selectedTabFilter == tab
                        val count = when (tab) {
                            "Movies" -> libraryItems.count { it.type == "movie" }
                            "Series" -> libraryItems.count { it.type == "series" }
                            "Watching" -> libraryItems.count { it.status == WatchStatus.WATCHING }
                            "Plan to Watch" -> libraryItems.count { it.status == WatchStatus.PLAN_TO_WATCH }
                            "Completed" -> libraryItems.count { it.status == WatchStatus.COMPLETED }
                            else -> libraryItems.size
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTabFilter = tab },
                            label = {
                                Text(
                                    text = "$tab ($count)",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WuPlayPurple,
                                selectedLabelColor = Color.White,
                                containerColor = StreamSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = StreamBorder,
                                selectedBorderColor = WuPlayPurple
                            ),
                            modifier = Modifier.testTag("watchlist_tab_$tab")
                        )
                    }
                }
            }
        },
        containerColor = StreamBg,
        modifier = modifier.testTag("watchlist_screen")
    ) { innerPadding ->
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StreamSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = WuPlayCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (libraryItems.isEmpty()) "Your Watchlist is Empty" else "No $selectedTabFilter Found",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browse Movies, TV Shows, and Streaming Services to add titles to your personal WuPlay collection.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { viewModel.switchTab(AppTab.DISCOVER) },
                        colors = ButtonDefaults.buttonColors(containerColor = WuPlayPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explore Titles")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(filteredItems) { item ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = StreamCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(StreamBorder, StreamBorder))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(item.id, item.type) }
                            .testTag("watchlist_item_${item.id}")
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(155.dp)
                            ) {
                                if (!item.poster.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = item.poster,
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(StreamSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = TextMuted)
                                    }
                                }

                                // Quick Remove Bookmark
                                IconButton(
                                    onClick = { viewModel.removeFromLibrary(item.id) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Type Badge
                                Surface(
                                    color = WuPlayPurple.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = item.type.uppercase(),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                // Progress bar for in-progress items
                                if (item.watchPositionMs > 0 && item.progressFraction > 0f) {
                                    LinearProgressIndicator(
                                        progress = { item.progressFraction },
                                        color = WuPlayCyan,
                                        trackColor = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .align(Alignment.BottomCenter)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.status.displayName,
                                    color = WuPlayCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
