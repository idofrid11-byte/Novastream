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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MetaItem
import com.example.data.model.StreamingProviders
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

    val continueWatchingItems = remember(libraryItems) {
        libraryItems.filter { it.watchPositionMs > 0 && it.progressFraction < 0.95f }
            .sortedByDescending { it.lastWatchedTimestamp }
    }

    val streamingProviders = StreamingProviders.ALL_PROVIDERS
    val genresList = listOf("All", "Animation", "Sci-Fi", "Comedy", "Classic", "Drama", "Horror", "Action")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StreamSurface)
                    .statusBarsPadding()
            ) {
                // Main Header Row with WuPlay Branding
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
                        // Futuristic WuPlay W Emblem with glowing gradient
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(WuPlayPurple, WuPlayCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Wu",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Play",
                                    color = WuPlayCyan,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "Cinematic Media Hub",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Search & Settings Shortcuts
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.switchTab(AppTab.SEARCH) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StreamSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = WuPlayCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.switchTab(AppTab.SETTINGS) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StreamSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Media Type Filter (All, Movies, Series)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("all" to "Featured", "movie" to "Movies", "series" to "TV Series")
                    types.forEach { (typeKey, label) ->
                        val isSelected = selectedType == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setMediaTypeFilter(typeKey) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                            modifier = Modifier.testTag("filter_type_$typeKey")
                        )
                    }
                }

                // Genre Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    genresList.forEach { g ->
                        val isAll = g == "All"
                        val isSelected = (isAll && selectedGenre == null) || (selectedGenre == g)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setGenreFilter(if (isAll) null else g) },
                            label = { Text(g, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StreamSurfaceVariant,
                                selectedLabelColor = WuPlayCyan,
                                containerColor = StreamBg,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = StreamBorder,
                                selectedBorderColor = WuPlayCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        containerColor = StreamBg,
        modifier = modifier.testTag("home_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. HERO BANNER
            if (heroItem != null) {
                item {
                    val isHeroSaved = libraryItems.any { it.id == heroItem!!.id }
                    HeroBanner(
                        item = heroItem!!,
                        onPlayClick = {
                            onNavigateToDetail(heroItem!!.id, heroItem!!.type)
                        },
                        onDetailClick = {
                            onNavigateToDetail(heroItem!!.id, heroItem!!.type)
                        },
                        onToggleLibrary = {
                            // Quick toggle watchlist for hero
                            viewModel.navigateToDetail(heroItem!!.id, heroItem!!.type)
                        },
                        isSaved = isHeroSaved
                    )
                }
            }

            // 2. STREAMING SERVICES CATALOG ROW (Netflix, Apple TV+, Disney+, Max, Prime, Paramount+, Hulu)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = WuPlayCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Streaming Services",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { viewModel.switchTab(AppTab.STREAMING) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("View All", color = WuPlayCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WuPlayCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(streamingProviders) { provider ->
                            val brandColor = Color(provider.colorHex)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StreamCard),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(
                                        listOf(brandColor.copy(alpha = 0.8f), Color(provider.secondaryColorHex).copy(alpha = 0.6f))
                                    )
                                ),
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(72.dp)
                                    .clickable {
                                        viewModel.selectStreamingProvider(provider.id)
                                        viewModel.switchTab(AppTab.STREAMING)
                                    }
                                    .testTag("home_service_${provider.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            color = brandColor,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = provider.logoText,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = provider.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. CONTINUE WATCHING CAROUSEL
            if (continueWatchingItems.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = WuPlayCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Continue Watching",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${continueWatchingItems.size} in progress",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(continueWatchingItems) { item ->
                                ContinueWatchingCard(
                                    item = item,
                                    onClick = { onNavigateToDetail(item.id, item.type) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. LOADING STATE OR CATALOG SECTIONS
            if (isLoading && catalogSections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = WuPlayPurple)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Loading WuPlay catalogs...", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Render each catalog section horizontally
                catalogSections.forEach { (sectionTitle, items) ->
                    if (items.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sectionTitle,
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${items.size} titles",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(items) { mediaItem ->
                                        MediaPosterCard(
                                            item = mediaItem,
                                            onClick = { onNavigateToDetail(mediaItem.id, mediaItem.type) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
