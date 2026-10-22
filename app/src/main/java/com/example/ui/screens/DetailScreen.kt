package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.MetaDetail
import com.example.data.model.VideoEpisode
import com.example.data.model.WatchStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DetailScreen(
    id: String,
    type: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta by viewModel.currentMeta.collectAsState()
    val isLoadingMeta by viewModel.isLoadingDetail.collectAsState()
    val isDirectPlaying by viewModel.isDirectPlaying.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val libraryItems by viewModel.libraryItems.collectAsState()
    val watchedEpisodes by viewModel.watchedEpisodes.collectAsState()

    val isSaved = remember(libraryItems, id) {
        libraryItems.any { it.id == id }
    }

    val isMovieWatched = remember(libraryItems, id) {
        viewModel.isMovieWatched(id)
    }

    val resumePositionMs = remember(libraryItems, id) {
        viewModel.getContinueWatchingPosition(id)
    }

    var selectedEpisode by remember { mutableStateOf<VideoEpisode?>(null) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    // Auto-select initial episode for series
    LaunchedEffect(meta) {
        if (meta?.type == "series" && !meta?.videos.isNullOrEmpty()) {
            selectedEpisode = meta!!.videos!!.first()
        }
    }

    Scaffold(
        containerColor = StreamBg,
        modifier = modifier.testTag("detail_screen")
    ) { innerPadding ->
        if (isLoadingMeta && meta == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = WuPlayPurple, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading WuPlay Title...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (meta == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StreamCoral, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Could not load title details", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = WuPlayPurple, contentColor = Color.White)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            val item = meta!!

            // Group episodes by season for series
            val episodesBySeason = remember(item.videos) {
                item.videos?.groupBy { it.season }?.toSortedMap() ?: sortedMapOf()
            }
            val seasonNumbers = remember(episodesBySeason) {
                episodesBySeason.keys.toList()
            }
            var activeSeasonNum by remember(item.id, selectedSeason, seasonNumbers) {
                mutableIntStateOf(if (selectedSeason in seasonNumbers) selectedSeason else (seasonNumbers.firstOrNull() ?: 1))
            }
            val currentSeasonEpisodes = episodesBySeason[activeSeasonNum] ?: emptyList()
            val currentSeasonTitle = currentSeasonEpisodes.firstOrNull()?.seasonTitle ?: "Season $activeSeasonNum"

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // 1. EXPANDED LARGE BACKDROP with Multi-Stop Cinematic Gradient
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                            .testTag("detail_backdrop_box")
                    ) {
                        val backdropUrl = item.background ?: item.poster

                        if (!backdropUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = backdropUrl,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(StreamSurfaceVariant)
                            )
                        }

                        // Cinematic multi-stop gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.70f),
                                        0.20f to Color.Transparent,
                                        0.50f to Color.Transparent,
                                        0.70f to StreamBg.copy(alpha = 0.75f),
                                        0.88f to StreamBg.copy(alpha = 0.95f),
                                        1.0f to StreamBg
                                    )
                                )
                        )

                        // Top Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .testTag("detail_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Watched toggle for movies
                                if (item.type != "series") {
                                    IconButton(
                                        onClick = { viewModel.toggleMovieWatched(item) },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isMovieWatched) StreamGreen.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.65f))
                                            .border(1.dp, if (isMovieWatched) StreamGreen else Color.Transparent, CircleShape)
                                            .testTag("detail_movie_watched_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (isMovieWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                            contentDescription = if (isMovieWatched) "Watched" else "Mark as Watched",
                                            tint = if (isMovieWatched) StreamGreen else Color.White
                                        )
                                    }
                                }

                                // Watchlist toggle button
                                IconButton(
                                    onClick = { viewModel.toggleLibrary(item, WatchStatus.PLAN_TO_WATCH) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSaved) WuPlayPurple.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.65f))
                                        .border(1.dp, if (isSaved) WuPlayPurple else Color.Transparent, CircleShape)
                                        .testTag("detail_library_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (isSaved) WuPlayCyan else Color.White
                                    )
                                }
                            }
                        }

                        // Big Central Play Trigger Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(WuPlayPurple, WuPlayCyan)
                                    )
                                )
                                .clickable {
                                    val targetEp = if (item.type == "series") selectedEpisode ?: item.videos?.firstOrNull() else null
                                    viewModel.playDirect(item, targetEp)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDirectPlaying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Title, Badges, Direct Play Action & Metadata
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metadata Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!item.imdbRating.isNullOrEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(StreamSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = StreamGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.imdbRating,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!item.releaseInfo.isNullOrEmpty()) {
                                Surface(
                                    color = StreamSurfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.releaseInfo,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (!item.runtime.isNullOrEmpty()) {
                                Surface(
                                    color = StreamSurfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.runtime,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Surface(
                                color = WuPlayPurple.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = item.type.uppercase(),
                                    color = WuPlayCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PRIMARY DIRECT PLAY ACTION (WuPlay One-Click Instant Play)
                        val playButtonText = when {
                            isDirectPlaying -> "Resolving WuPlay Stream..."
                            item.type == "series" -> {
                                val ep = selectedEpisode ?: item.videos?.firstOrNull()
                                if (ep != null) "Watch S${ep.season}:E${ep.episode} • ${ep.title}" else "Watch Series"
                            }
                            resumePositionMs > 0 -> "Resume Movie"
                            else -> "Watch Movie Now"
                        }

                        Button(
                            onClick = {
                                val targetEp = if (item.type == "series") selectedEpisode ?: item.videos?.firstOrNull() else null
                                viewModel.playDirect(item, targetEp)
                            },
                            enabled = !isDirectPlaying,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WuPlayPurple,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("direct_play_button")
                        ) {
                            if (isDirectPlaying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = playButtonText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Action Buttons Row (Mark Watched, Add to Watchlist)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.type != "series") {
                                OutlinedButton(
                                    onClick = { viewModel.toggleMovieWatched(item) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isMovieWatched) StreamGreen else TextSecondary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(
                                            if (isMovieWatched) listOf(StreamGreen, StreamGreen) else listOf(StreamBorder, StreamBorder)
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("mark_movie_watched_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isMovieWatched) Icons.Default.CheckCircle else Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isMovieWatched) "Watched ✓" else "Mark Watched",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Watchlist Button
                            OutlinedButton(
                                onClick = { viewModel.toggleLibrary(item, WatchStatus.PLAN_TO_WATCH) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSaved) WuPlayCyan else TextSecondary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(
                                        if (isSaved) listOf(WuPlayPurple, WuPlayCyan) else listOf(StreamBorder, StreamBorder)
                                    )
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("detail_library_button")
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSaved) "In Watchlist" else "+ Watchlist",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Genres
                        if (!item.genres.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item.genres.forEach { g ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(StreamSurfaceVariant)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = g, color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Overview / Synopsis
                        if (!item.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = item.description,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                maxLines = if (isDescriptionExpanded) 12 else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                            if (item.description.length > 120) {
                                Text(
                                    text = if (isDescriptionExpanded) "Show Less" else "Read More",
                                    color = WuPlayCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                )
                            }
                        }

                        // Cast & Directors
                        if (!item.director.isNullOrEmpty() || !item.cast.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (!item.director.isNullOrEmpty()) {
                                Text(
                                    text = "Director: ${item.director.joinToString(", ")}",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            if (!item.cast.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Starring: ${item.cast.take(4).joinToString(", ")}",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 3. SEASONS, SEASON TITLES & EPISODES (Direct Play on Episode Tap)
                if (item.type == "series" && !item.videos.isNullOrEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                        ) {
                            // Section Header with Total Seasons / Episodes Count
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Seasons & Episodes",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = StreamSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${seasonNumbers.size} Seasons • ${item.videos.size} Episodes",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Horizontal Season Selector Tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                seasonNumbers.forEach { sNum ->
                                    val isSelected = activeSeasonNum == sNum
                                    val seasonEps = episodesBySeason[sNum] ?: emptyList()
                                    val allSeasonWatched = seasonEps.isNotEmpty() && seasonEps.all { watchedEpisodes.contains(it.id) }

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            activeSeasonNum = sNum
                                            viewModel.selectSeason(sNum)
                                            val firstInSeason = seasonEps.firstOrNull()
                                            if (firstInSeason != null) {
                                                selectedEpisode = firstInSeason
                                            }
                                        },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Season $sNum",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                                if (allSeasonWatched) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Season Watched",
                                                        tint = if (isSelected) Color.White else StreamGreen,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
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
                                        modifier = Modifier.testTag("season_chip_$sNum")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // SEASON TITLE & MARK SEASON AS WATCHED ACTION
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StreamCard),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(listOf(StreamBorder, StreamBorderGlow))
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentSeasonTitle,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${currentSeasonEpisodes.size} episodes available",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    val allWatched = currentSeasonEpisodes.isNotEmpty() && currentSeasonEpisodes.all { watchedEpisodes.contains(it.id) }
                                    TextButton(
                                        onClick = {
                                            viewModel.markSeasonWatched(currentSeasonEpisodes, !allWatched, item)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (allWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = if (allWatched) StreamGreen else WuPlayCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (allWatched) "Season Watched" else "Mark Season",
                                            color = if (allWatched) StreamGreen else WuPlayCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // EPISODES LIST (Clicking ANY episode initiates DIRECT PLAYBACK!)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                currentSeasonEpisodes.forEach { ep ->
                                    val isEpWatched = watchedEpisodes.contains(ep.id)
                                    val isCurrent = selectedEpisode?.id == ep.id

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) StreamSurfaceVariant else StreamCard
                                        ),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = Brush.horizontalGradient(
                                                if (isCurrent) listOf(WuPlayPurple, WuPlayCyan) else listOf(StreamBorder, StreamBorder)
                                            )
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedEpisode = ep
                                                // DIRECT PLAY: Tapping episode immediately starts playback!
                                                viewModel.playDirect(item, ep)
                                            }
                                            .testTag("episode_card_${ep.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Thumbnail Box with Play overlay & S:E badge
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 110.dp, height = 70.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(StreamSurface)
                                            ) {
                                                val thumb = ep.thumbnail ?: item.poster
                                                if (!thumb.isNullOrEmpty()) {
                                                    AsyncImage(
                                                        model = thumb,
                                                        contentDescription = ep.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                // Top-right watched toggle
                                                IconButton(
                                                    onClick = { viewModel.toggleEpisodeWatched(ep, item) },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp)
                                                        .size(24.dp)
                                                        .testTag("episode_watch_toggle_${ep.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isEpWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                                        contentDescription = "Toggle Watched",
                                                        tint = if (isEpWatched) StreamGreen else Color.White.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Badge S1E1
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(4.dp)
                                                ) {
                                                    Text(
                                                        text = "E${ep.episode}",
                                                        color = WuPlayCyan,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }

                                                // Play glyph
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(WuPlayPurple.copy(alpha = 0.85f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = ep.title,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (isEpWatched) {
                                                        Text(
                                                            text = "Watched",
                                                            color = StreamGreen,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                if (!ep.overview.isNullOrEmpty()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = ep.overview,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Tap to play directly",
                                                    color = WuPlayCyan,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
