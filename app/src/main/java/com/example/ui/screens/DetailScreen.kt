package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MetaDetail
import com.example.data.model.StreamItem
import com.example.data.model.VideoEpisode
import com.example.data.model.WatchStatus
import com.example.ui.components.StreamSourceCard
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
    val context = LocalContext.current
    val meta by viewModel.currentMeta.collectAsState()
    val streams by viewModel.currentStreams.collectAsState()
    val isLoadingMeta by viewModel.isLoadingDetail.collectAsState()
    val isLoadingStreams by viewModel.isLoadingStreams.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val libraryItems by viewModel.libraryItems.collectAsState()
    val watchedEpisodes by viewModel.watchedEpisodes.collectAsState()

    val isSaved = remember(libraryItems, id) {
        libraryItems.any { it.id == id }
    }

    val isMovieWatched = remember(libraryItems, id) {
        viewModel.isMovieWatched(id)
    }

    var selectedEpisode by remember { mutableStateOf<VideoEpisode?>(null) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    // Stream options & filtering
    var selectedQualityFilter by remember { mutableStateOf("All") }
    var streamForOptionsDialog by remember { mutableStateOf<StreamItem?>(null) }
    var showCustomStreamDialog by remember { mutableStateOf(false) }

    // Update selectedEpisode when meta loads
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
                CircularProgressIndicator(color = StreamCyan)
            }
        } else if (meta == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Could not load title details", color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = StreamCyan, contentColor = Color.Black)
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

            // Filter streams by quality
            val filteredStreams = remember(streams, selectedQualityFilter) {
                if (selectedQualityFilter == "All") streams else {
                    streams.filter { s ->
                        when (selectedQualityFilter) {
                            "4K UHD" -> s.quality?.contains("4K", ignoreCase = true) == true
                            "1080p" -> s.quality?.contains("1080", ignoreCase = true) == true
                            "720p" -> s.quality?.contains("720", ignoreCase = true) == true
                            else -> true
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // 1. EXPANDED LARGE BACKDROP with Multi-Stop Gradient Scrim (480dp tall)
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
                                        0.72f to StreamBg.copy(alpha = 0.65f),
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
                                // Watched quick toggle on backdrop for movies
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

                                IconButton(
                                    onClick = { viewModel.toggleLibrary(item, WatchStatus.PLAN_TO_WATCH) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .testTag("detail_library_toggle")
                                    ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (isSaved) StreamCyan else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Title, Watched Status Actions & Metadata Chips
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

                        // Primary Action Buttons Row (Watch Now, Watched Toggle, Add to Library)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mark as Watched button for Movies
                            if (item.type != "series") {
                                Button(
                                    onClick = { viewModel.toggleMovieWatched(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isMovieWatched) StreamGreen.copy(alpha = 0.25f) else StreamSurfaceVariant,
                                        contentColor = if (isMovieWatched) StreamGreen else Color.White
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
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isMovieWatched) "Watched ✓" else "Mark Watched",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Library Button
                            OutlinedButton(
                                onClick = { viewModel.toggleLibrary(item, WatchStatus.PLAN_TO_WATCH) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSaved) StreamCyan else Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(
                                        if (isSaved) listOf(StreamCyan, StreamCyan) else listOf(StreamBorder, StreamBorder)
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSaved) "In Watchlist" else "+ Watchlist",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                color = StreamPurple.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = item.type.uppercase(),
                                    color = StreamPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Genres
                        if (!item.genres.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
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
                                    color = StreamCyan,
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

                // 3. SEASONS AND SEASONS TITLES SECTION (for series)
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
                                        tint = StreamCyan,
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
                                            val firstEpOfSeason = seasonEps.firstOrNull()
                                            if (firstEpOfSeason != null) {
                                                selectedEpisode = firstEpOfSeason
                                                viewModel.selectEpisode(firstEpOfSeason)
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
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Watched",
                                                        tint = if (isSelected) Color.Black else StreamGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
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
                                        modifier = Modifier.testTag("season_chip_$sNum")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // SEASON TITLE SHOWCASE BANNER
                            val explicitSeasonTitle = currentSeasonEpisodes.firstOrNull { !it.seasonTitle.isNullOrBlank() }?.seasonTitle
                            val seasonTitleDisplay = explicitSeasonTitle ?: when (activeSeasonNum) {
                                1 -> "Season 1: Patagonian Escapades"
                                2 -> "Season 2: Andean Heights"
                                3 -> "Season 3: The Highland Expeditions"
                                4 -> "Season 4: The Wild Frontiers"
                                else -> "Season $activeSeasonNum"
                            }

                            val watchedCountInSeason = currentSeasonEpisodes.count { watchedEpisodes.contains(it.id) }
                            val isEntireSeasonWatched = currentSeasonEpisodes.isNotEmpty() && watchedCountInSeason == currentSeasonEpisodes.size

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StreamCard),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(listOf(StreamCyan.copy(alpha = 0.5f), StreamPurple.copy(alpha = 0.3f)))
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .testTag("season_title_banner")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = StreamCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "SEASON $activeSeasonNum",
                                                color = StreamCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        // Mark Entire Season Watched / Unmark Button
                                        Surface(
                                            onClick = {
                                                val shouldMark = !isEntireSeasonWatched
                                                viewModel.markSeasonWatched(currentSeasonEpisodes, shouldMark, item)
                                                Toast.makeText(
                                                    context,
                                                    if (shouldMark) "Marked Season $activeSeasonNum as Watched" else "Unmarked Season $activeSeasonNum",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isEntireSeasonWatched) StreamGreen.copy(alpha = 0.2f) else StreamSurfaceVariant,
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = Brush.horizontalGradient(
                                                    if (isEntireSeasonWatched) listOf(StreamGreen, StreamGreen) else listOf(StreamBorder, StreamBorder)
                                                )
                                            ),
                                            modifier = Modifier.testTag("mark_season_watched_btn")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isEntireSeasonWatched) Icons.Default.CheckCircle else Icons.Default.DoneAll,
                                                    contentDescription = null,
                                                    tint = if (isEntireSeasonWatched) StreamGreen else TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isEntireSeasonWatched) "Season Watched" else "Mark Season Watched",
                                                    color = if (isEntireSeasonWatched) StreamGreen else TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Big Bold Season Title
                                    Text(
                                        text = seasonTitleDisplay,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${currentSeasonEpisodes.size} Episodes • $watchedCountInSeason Watched • Full HD 1080p Stream",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Horizontal Episodes List
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(currentSeasonEpisodes, key = { it.id }) { ep ->
                                    val isCurrent = selectedEpisode?.id == ep.id
                                    val isEpWatched = watchedEpisodes.contains(ep.id)

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) StreamSurfaceVariant else StreamCard
                                        ),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = Brush.horizontalGradient(
                                                if (isCurrent) listOf(StreamCyan, StreamCyan) else listOf(StreamBorder, StreamBorder)
                                            )
                                        ),
                                        modifier = Modifier
                                            .width(200.dp)
                                            .clickable {
                                                selectedEpisode = ep
                                                viewModel.selectEpisode(ep)
                                            }
                                            .testTag("episode_${ep.id}")
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(105.dp)
                                                    .background(StreamSurfaceVariant)
                                            ) {
                                                if (!ep.thumbnail.isNullOrEmpty()) {
                                                    AsyncImage(
                                                        model = ep.thumbnail,
                                                        contentDescription = ep.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                // Top Right: Episode Watched Interactive Toggle
                                                Surface(
                                                    onClick = {
                                                        viewModel.toggleEpisodeWatched(ep, item)
                                                    },
                                                    color = Color.Black.copy(alpha = 0.75f),
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(28.dp)
                                                        .testTag("episode_watch_toggle_${ep.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (isEpWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                                            contentDescription = "Toggle Watched",
                                                            tint = if (isEpWatched) StreamGreen else Color.White.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                // Bottom Left: Season / Episode badge
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(6.dp)
                                                ) {
                                                    Text(
                                                        text = "S${ep.season}E${ep.episode}",
                                                        color = StreamCyan,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                // Play icon overlay if active
                                                if (isCurrent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.Center)
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(StreamCyan.copy(alpha = 0.85f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.padding(10.dp)) {
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
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = ep.overview,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. STREAM SOURCES & STREAM OPTIONS SECTION
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        // Header with Title & Action to Add Custom Stream
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = StreamCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedEpisode != null) "Streams for ${selectedEpisode!!.title}" else "Available Streams",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Add Custom Stream button
                            OutlinedButton(
                                onClick = { showCustomStreamDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StreamCyan),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(StreamCyan, StreamPurple))
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("add_custom_stream_btn")
                            ) {
                                Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Stream", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stream Quality Filter Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filterOptions = listOf("All", "4K UHD", "1080p", "720p")
                            filterOptions.forEach { qOption ->
                                val isSelected = selectedQualityFilter == qOption
                                val count = if (qOption == "All") streams.size else {
                                    streams.count { it.quality?.contains(qOption.replace(" UHD", ""), ignoreCase = true) == true }
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedQualityFilter = qOption },
                                    label = {
                                        Text(
                                            text = "$qOption ($count)",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
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
                                    modifier = Modifier.testTag("quality_filter_$qOption")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isLoadingStreams && streams.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = StreamCyan, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Resolving streams from active addons...", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        } else if (filteredStreams.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StreamSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (streams.isEmpty()) "No streams found from current addons" else "No streams matching $selectedQualityFilter",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Try adding a custom stream link or resetting quality filters.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showCustomStreamDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = StreamCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Add Custom Stream URL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredStreams.forEach { stream ->
                                    StreamSourceCard(
                                        stream = stream,
                                        onClick = {
                                            val resumePos = viewModel.getContinueWatchingPosition(item.id)
                                            viewModel.navigateToPlayer(
                                                stream = stream,
                                                title = item.name,
                                                subtitle = selectedEpisode?.title ?: item.releaseInfo,
                                                itemId = item.id,
                                                startPositionMs = resumePos,
                                                meta = item,
                                                episode = selectedEpisode
                                            )
                                        },
                                        onOptionsClick = {
                                            streamForOptionsDialog = stream
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // Stream Options Dialog (Copy URL, External Player, Details)
            if (streamForOptionsDialog != null) {
                val stream = streamForOptionsDialog!!
                AlertDialog(
                    onDismissRequest = { streamForOptionsDialog = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = StreamCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stream Source Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = stream.title ?: "Stream Source",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Provider: ${stream.addonName} • Quality: ${stream.quality ?: "HD"}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Play in App button
                            Button(
                                onClick = {
                                    val resumePos = viewModel.getContinueWatchingPosition(item.id)
                                    viewModel.navigateToPlayer(
                                        stream = stream,
                                        title = item.name,
                                        subtitle = selectedEpisode?.title ?: item.releaseInfo,
                                        itemId = item.id,
                                        startPositionMs = resumePos,
                                        meta = item,
                                        episode = selectedEpisode
                                    )
                                    streamForOptionsDialog = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StreamCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play in App Player", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Copy Stream URL
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Stream URL", stream.url ?: stream.externalUrl ?: "")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Stream link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    streamForOptionsDialog = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Stream URL")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Open in External Player (VLC / MX Player)
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val streamUrl = stream.url ?: stream.externalUrl
                                        if (!streamUrl.isNullOrEmpty()) {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(Uri.parse(streamUrl), "video/*")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Play stream with"))
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No external player found: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    streamForOptionsDialog = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StreamPurple),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open in External Player (VLC)")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { streamForOptionsDialog = null }) {
                            Text("Close", color = TextSecondary)
                        }
                    },
                    containerColor = StreamSurface
                )
            }

            // Custom Stream URL Dialog
            if (showCustomStreamDialog) {
                var customUrl by remember { mutableStateOf("") }
                var customTitle by remember { mutableStateOf("Custom Direct Stream") }
                var customQuality by remember { mutableStateOf("1080p") }

                AlertDialog(
                    onDismissRequest = { showCustomStreamDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddLink, contentDescription = null, tint = StreamCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Stream URL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Enter any direct HTTP/HTTPS video URL (.mp4, .m3u8, webm) to play immediately with this title.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = customUrl,
                                onValueChange = { customUrl = it },
                                label = { Text("Stream URL") },
                                placeholder = { Text("https://example.com/video.mp4") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val itemClip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                            if (!itemClip.isNullOrEmpty()) {
                                                customUrl = itemClip
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = StreamCyan)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StreamCyan,
                                    unfocusedBorderColor = StreamBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_stream_url_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customTitle,
                                onValueChange = { customTitle = it },
                                label = { Text("Stream Label / Name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StreamCyan,
                                    unfocusedBorderColor = StreamBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("1080p", "4K UHD", "720p").forEach { q ->
                                    FilterChip(
                                        selected = customQuality == q,
                                        onClick = { customQuality = q },
                                        label = { Text(q, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StreamCyan,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customUrl.isNotBlank()) {
                                    val newStream = StreamItem(
                                        title = customTitle.ifBlank { "Custom Direct Stream" },
                                        name = "Direct URL Source",
                                        url = customUrl.trim(),
                                        quality = customQuality,
                                        addonName = "Custom URL"
                                    )
                                    viewModel.addCustomStream(newStream)
                                    Toast.makeText(context, "Added stream to available sources!", Toast.LENGTH_SHORT).show()
                                    showCustomStreamDialog = false
                                }
                            },
                            enabled = customUrl.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = StreamCyan, contentColor = Color.Black)
                        ) {
                            Text("Add & Play", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomStreamDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = StreamSurface
                )
            }
        }
    }
}
