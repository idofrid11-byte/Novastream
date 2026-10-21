package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MetaItem
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.HeroBanner
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (id: String, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val catalogSections by viewModel.catalogSections.collectAsState()
    val heroItem by viewModel.heroItem.collectAsState()
    val isLoading by viewModel.isLoadingHome.collectAsState()
    val selectedType by viewModel.selectedMediaType.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val libraryItems by viewModel.libraryItems.collectAsState()
    val installedAddons by viewModel.installedAddons.collectAsState()

    val continueWatchingItems = remember(libraryItems) {
        libraryItems.filter { it.watchPositionMs > 0 && it.progressFraction < 0.95f }
            .sortedByDescending { it.lastWatchedTimestamp }
    }

    val activeAddonsCount = remember(installedAddons) {
        installedAddons.count { it.isEnabled }
    }

    val genresList = listOf("All", "Animation", "Sci-Fi", "Comedy", "Classic", "Drama", "Horror", "Space")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StreamSurface)
                    .statusBarsPadding()
            ) {
                // Main Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_brand")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StreamCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Nova",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Stream",
                                    color = StreamCyan,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Addon Media Aggregator",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Addons Active Pill & Search Shortcut
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.switchTab(AppTab.ADDONS) },
                            shape = RoundedCornerShape(20.dp),
                            color = StreamSurfaceVariant,
                            modifier = Modifier.testTag("active_addons_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(StreamGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$activeAddonsCount Addons",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.switchTab(AppTab.SEARCH) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StreamSurfaceVariant)
                                .testTag("home_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Media Type Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        "all" to "Discover All",
                        "movie" to "Movies",
                        "series" to "Series",
                        "channel" to "Live & Space"
                    )
                    types.forEach { (key, label) ->
                        val isSelected = selectedType == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setMediaTypeFilter(key) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StreamCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = StreamSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = StreamBorder,
                                selectedBorderColor = StreamCyan
                            ),
                            modifier = Modifier.testTag("type_filter_$key")
                        )
                    }
                }

                // Genre Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    genresList.forEach { g ->
                        val isAll = g == "All"
                        val isSelected = if (isAll) selectedGenre == null else selectedGenre == g
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) StreamPurple.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    viewModel.setGenreFilter(if (isAll) null else g)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = g,
                                color = if (isSelected) StreamPurple else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        containerColor = StreamBg,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_catalog_list")
        ) {
            // Hero Banner
            if (heroItem != null) {
                item {
                    val hero = heroItem!!
                    val isSaved = viewModel.isSavedInLibrary(hero.id)
                    HeroBanner(
                        item = hero,
                        onPlayClick = {
                            onNavigateToDetail(hero.id, hero.type)
                        },
                        onDetailClick = {
                            onNavigateToDetail(hero.id, hero.type)
                        },
                        onToggleLibrary = {
                            // Fetch or toggle
                            onNavigateToDetail(hero.id, hero.type)
                        },
                        isSaved = isSaved
                    )
                }
            }

            // Continue Watching Shelf
            if (continueWatchingItems.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = StreamCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Continue Watching",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${continueWatchingItems.size} items",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(continueWatchingItems, key = { it.id }) { libItem ->
                                ContinueWatchingCard(
                                    item = libItem,
                                    onClick = {
                                        onNavigateToDetail(libItem.id, libItem.type)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Loading Indicator
            if (isLoading && catalogSections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StreamCyan)
                    }
                }
            }

            // Catalog Rows
            catalogSections.forEach { (sectionTitle, items) ->
                if (items.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 18.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sectionTitle,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${items.size} titles",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items, key = { it.id }) { item ->
                                    MediaPosterCard(
                                        item = item,
                                        onClick = {
                                            onNavigateToDetail(item.id, item.type)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
