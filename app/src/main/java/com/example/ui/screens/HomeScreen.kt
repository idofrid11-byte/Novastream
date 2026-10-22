package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LibraryItem
import com.example.data.model.MetaItem
import com.example.ui.components.HeroBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

/**
 * Stremio Board (Home Screen)
 * Faithfully mirrors the Board route from official Stremio Web.
 * Features:
 * - Stremio Header Bar with official logo and quick search trigger
 * - Continue Watching row with progress indicator and instant play
 * - Popular & Top Cinemeta movie/series catalog rows
 * - Trending Hero backdrop banner
 */
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
    val libraryItems by viewModel.libraryItems.collectAsState()

    val continueWatchingItems = remember(libraryItems) {
        libraryItems.filter { it.watchPositionMs > 0 && it.progressFraction < 0.95f }
            .sortedByDescending { it.lastWatchedTimestamp }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StremioDark)
                    .statusBarsPadding()
            ) {
                // Stremio Horizontal Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stremio Logo & Symbol
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.switchTab(AppTab.BOARD) }
                            .testTag("stremio_brand")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.stremio_symbol),
                            contentDescription = "Stremio",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Stremio",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = StremioFontFamily
                        )
                    }

                    // Search Pill & Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.switchTab(AppTab.SEARCH) },
                            shape = RoundedCornerShape(20.dp),
                            color = StremioSurfaceVariant,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(StremioBorder)
                            ),
                            modifier = Modifier.testTag("top_bar_search_trigger")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = StremioPurpleLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Search movies, series, addons...",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = StremioFontFamily
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.switchTab(AppTab.SETTINGS) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StremioSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Media Type Filter Row (All, Movies, Series)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        "all" to "All",
                        "movie" to "Movies",
                        "series" to "Series"
                    )
                    types.forEach { (typeKey, label) ->
                        val isSelected = selectedType == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setMediaTypeFilter(typeKey) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = StremioFontFamily
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StremioPurple,
                                selectedLabelColor = Color.White,
                                containerColor = StremioSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = StremioBorder,
                                selectedBorderColor = StremioPurple
                            ),
                            modifier = Modifier.testTag("type_filter_$typeKey")
                        )
                    }
                }
            }
        },
        containerColor = StremioBg,
        modifier = modifier
    ) { innerPadding ->
        if (isLoading && catalogSections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = StremioPurple)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading Stremio Board...",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = StremioFontFamily
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Featured Cinematic Hero Banner
                if (heroItem != null) {
                    item {
                        HeroBanner(
                            item = heroItem!!,
                            onPlayClick = {
                                onNavigateToDetail(heroItem!!.id, heroItem!!.type)
                            },
                            onDetailClick = {
                                onNavigateToDetail(heroItem!!.id, heroItem!!.type)
                            },
                            onToggleLibrary = {
                                viewModel.toggleLibrary(heroItem!!.toMetaDetail())
                            },
                            isSaved = libraryItems.any { it.id == heroItem!!.id },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                }

                // 2. Continue Watching Row (Stremio Board item)
                if (continueWatchingItems.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = StremioPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Continue Watching",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = StremioFontFamily
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.switchTab(AppTab.LIBRARY) },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "See All",
                                        color = StremioPurpleLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = StremioFontFamily
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(continueWatchingItems, key = { it.id }) { item ->
                                    StremioContinueWatchingCard(
                                        item = item,
                                        onClick = { onNavigateToDetail(item.id, item.type) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Stremio Board Catalog Rows
                catalogSections.forEach { (sectionTitle, items) ->
                    if (items.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
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
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = StremioFontFamily
                                    )

                                    IconButton(
                                        onClick = { viewModel.switchTab(AppTab.DISCOVER) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Browse $sectionTitle",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(items, key = { "${it.id}_${it.type}" }) { mediaItem ->
                                        StremioPosterCard(
                                            item = mediaItem,
                                            onClick = { onNavigateToDetail(mediaItem.id, mediaItem.type) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

/**
 * Authentic Stremio Poster Card with 1.464 ratio, rounded corners,
 * and Gold IMDb badge.
 */
@Composable
fun StremioPosterCard(
    item: MetaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(116.dp)
            .clickable(onClick = onClick)
            .testTag("stremio_poster_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StremioSurfaceVariant)
        ) {
            val posterUrl = item.poster ?: item.background
            if (!posterUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StremioSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Stremio Gold IMDb Badge
            if (!item.imdbRating.isNullOrEmpty()) {
                Surface(
                    color = StremioIMDb,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomEnd = 4.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.imdbRating,
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = StremioFontFamily
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = StremioFontFamily
        )

        Text(
            text = item.releaseInfo ?: item.type.replaceFirstChar { it.uppercase() },
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            fontFamily = StremioFontFamily
        )
    }
}

/**
 * Authentic Stremio Continue Watching Card with landscape preview,
 * episode subtitle, and purple watched progress bar.
 */
@Composable
fun StremioContinueWatchingCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = StremioDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(StremioBorder)
        ),
        modifier = modifier
            .width(220.dp)
            .clickable(onClick = onClick)
            .testTag("continue_watching_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(StremioSurfaceVariant)
            ) {
                val img = item.background ?: item.poster
                if (!img.isNullOrEmpty()) {
                    AsyncImage(
                        model = img,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Scrim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                // Play Button overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StremioPurple.copy(alpha = 0.85f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Episode indicator if series
                if (!item.lastEpisodeTitle.isNullOrEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = item.lastEpisodeTitle,
                            color = StremioPurpleLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = StremioFontFamily,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Linear Progress Bar (Stremio Purple)
            LinearProgressIndicator(
                progress = { item.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = StremioPurple,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = StremioFontFamily
                )
                Text(
                    text = item.releaseInfo ?: item.type.replaceFirstChar { it.uppercase() },
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    fontFamily = StremioFontFamily
                )
            }
        }
    }
}
