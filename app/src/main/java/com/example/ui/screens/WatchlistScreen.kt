package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LibraryItem
import com.example.data.model.WatchStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

/**
 * Stremio Library Screen
 * Faithfully mirrors the Library route from official Stremio Web.
 * Features:
 * - Stremio Logo & Library Header with title counter
 * - Filter pills: All, Watching, Plan to Watch, Watched
 * - Cloud synchronization with Firebase Firestore
 * - Poster cards with watch progress and IMDb rating badges
 */
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
            "Watching" -> libraryItems.filter { it.status == WatchStatus.WATCHING || (it.watchPositionMs > 0 && it.progressFraction < 0.95f) }
            "Plan to Watch" -> libraryItems.filter { it.status == WatchStatus.PLAN_TO_WATCH }
            "Watched" -> libraryItems.filter { it.status == WatchStatus.COMPLETED || it.progressFraction >= 0.95f }
            else -> libraryItems
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StremioDark)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.stremio_symbol),
                            contentDescription = "Stremio",
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Library",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = StremioFontFamily
                            )
                            Text(
                                text = "${libraryItems.size} saved titles • ${if (currentUser != null) "Cloud Synced" else "On Device"}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = StremioFontFamily
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentUser != null) {
                            IconButton(
                                onClick = { viewModel.syncLibraryWithCloud() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StremioSurfaceVariant)
                                    .testTag("sync_cloud_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        color = StremioPurple,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Sync Cloud",
                                        tint = StremioPurpleLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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

                Spacer(modifier = Modifier.height(8.dp))

                // Library Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterCategories = listOf("All", "Watching", "Plan to Watch", "Watched")
                    filterCategories.forEach { category ->
                        val isSelected = selectedTabFilter == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTabFilter = category },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 11.sp,
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
                            modifier = Modifier.testTag("library_filter_$category")
                        )
                    }
                }
            }
        },
        containerColor = StremioBg,
        modifier = modifier.testTag("library_screen")
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
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(StremioSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = StremioPurpleLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedTabFilter == "All") "Your Stremio Library is empty" else "No $selectedTabFilter titles",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = StremioFontFamily
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browse Board or Discover to bookmark movies and series to your Library.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = StremioFontFamily
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.switchTab(AppTab.BOARD) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StremioPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Browse Board",
                            fontWeight = FontWeight.Bold,
                            fontFamily = StremioFontFamily
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("library_grid")
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    StremioLibraryPosterCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id, item.type) }
                    )
                }
            }
        }
    }
}

/**
 * Poster card for Stremio Library items with progress bar and watched indicator.
 */
@Composable
fun StremioLibraryPosterCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("library_item_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
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

            // Top-left IMDb rating badge
            if (!item.imdbRating.isNullOrEmpty()) {
                Surface(
                    color = StremioIMDb,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomEnd = 4.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = item.imdbRating,
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = StremioFontFamily,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Watched checkmark badge if completed
            if (item.status == WatchStatus.COMPLETED || item.progressFraction >= 0.95f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(StreamGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Watched",
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Bottom Progress bar if watching
            if (item.progressFraction in 0.01f..0.94f) {
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = StremioPurple,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
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
