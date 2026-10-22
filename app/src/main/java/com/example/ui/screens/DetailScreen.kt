package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MetaDetail
import com.example.data.model.StreamItem
import com.example.data.model.VideoEpisode
import com.example.data.model.WatchStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

/**
 * Stremio MetaDetails Screen
 * Faithfully mirrors the MetaDetails route from official Stremio Web.
 * Features:
 * - Full-bleed cinematic backdrop with multi-stop gradient
 * - Stremio Logo & Title header with IMDb gold rating badge
 * - Primary "Play / Resume" action + "Add to Library" + "Share"
 * - Season & Episode Picker (for Series)
 * - Available Streams List (resolution, provider, direct stream launch)
 */
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
    val isLoadingMeta by viewModel.isLoadingDetail.collectAsState()
    val isDirectPlaying by viewModel.isDirectPlaying.collectAsState()
    val currentStreams by viewModel.currentStreams.collectAsState()
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

    LaunchedEffect(meta) {
        if (meta?.type == "series" && !meta?.videos.isNullOrEmpty()) {
            selectedEpisode = meta!!.videos!!.first()
        }
    }

    Scaffold(
        containerColor = StremioBg,
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
                    CircularProgressIndicator(color = StremioPurple, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading Stremio Details...",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = StremioFontFamily
                    )
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
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = StreamCoral,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Could not load title details",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontFamily = StremioFontFamily
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StremioPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Go Back", fontFamily = StremioFontFamily)
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // 1. Full-Bleed Cinematic Backdrop Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
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
                                    .background(StremioSurfaceVariant)
                            )
                        }

                        // Gradient Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.5f),
                                            0.5f to Color.Transparent,
                                            0.8f to StremioBg.copy(alpha = 0.75f),
                                            1.0f to StremioBg
                                        )
                                    )
                                )
                        )

                        // Top Navigation Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .testTag("detail_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Share Button
                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            setType("text/plain")
                                            putExtra(Intent.EXTRA_SUBJECT, item.name)
                                            putExtra(Intent.EXTRA_TEXT, "Watch ${item.name} on Stremio: https://strem.io")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share with"))
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .testTag("share_title_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Bookmark in Library Toggle
                                IconButton(
                                    onClick = { viewModel.toggleLibrary(item, WatchStatus.PLAN_TO_WATCH) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSaved) StremioPurple else Color.Black.copy(alpha = 0.6f)
                                        )
                                        .testTag("detail_library_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Library",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Center Play Button with Stremio Glow
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(StremioPurple)
                                .clickable {
                                    val targetEp = if (item.type == "series") selectedEpisode ?: item.videos?.firstOrNull() else null
                                    viewModel.playDirect(item, targetEp)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDirectPlaying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Title, Badges, Direct Play & Meta Information
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = StremioFontFamily
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stremio Metadata Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!item.imdbRating.isNullOrEmpty()) {
                                Surface(
                                    color = StremioIMDb,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = item.imdbRating,
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = StremioFontFamily
                                        )
                                    }
                                }
                            }

                            if (!item.releaseInfo.isNullOrEmpty()) {
                                Surface(
                                    color = StremioSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.releaseInfo,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = StremioFontFamily,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (!item.runtime.isNullOrEmpty()) {
                                Surface(
                                    color = StremioSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.runtime,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = StremioFontFamily,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Surface(
                                color = StremioPurple.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = item.type.uppercase(),
                                    color = StremioPurpleLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = StremioFontFamily,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Play Button
                        val playButtonText = when {
                            isDirectPlaying -> "Resolving Stremio Stream..."
                            item.type == "series" -> {
                                val ep = selectedEpisode ?: item.videos?.firstOrNull()
                                if (ep != null) "Watch S${ep.season}:E${ep.episode} • ${ep.title}" else "Watch Series"
                            }
                            resumePositionMs > 0 -> "Resume Movie"
                            else -> "Watch Now"
                        }

                        Button(
                            onClick = {
                                val targetEp = if (item.type == "series") selectedEpisode ?: item.videos?.firstOrNull() else null
                                viewModel.playDirect(item, targetEp)
                            },
                            enabled = !isDirectPlaying,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StremioPurple,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("direct_play_button")
                        ) {
                            if (isDirectPlaying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = playButtonText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = StremioFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Actions (Library & Mark Watched)
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
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (isMovieWatched) StreamGreen else StremioBorder
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
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
                                        fontSize = 12.sp,
                                        fontFamily = StremioFontFamily
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleLibrary(item, WatchStatus.PLAN_TO_WATCH) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSaved) StremioPurpleLight else TextSecondary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isSaved) StremioPurple else StremioBorder
                                    )
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("detail_watchlist_button")
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSaved) "In Library ✓" else "+ Library",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    fontFamily = StremioFontFamily
                                )
                            }
                        }

                        // Genres Pills
                        if (!item.genres.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item.genres.forEach { genre ->
                                    Surface(
                                        color = StremioSurfaceVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = StremioFontFamily,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Description / Synopsis
                        if (!item.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = item.description,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                maxLines = if (isDescriptionExpanded) 12 else 3,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = StremioFontFamily,
                                modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                            if (item.description.length > 120) {
                                Text(
                                    text = if (isDescriptionExpanded) "Show Less" else "Read More",
                                    color = StremioPurpleLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = StremioFontFamily,
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
                                    fontSize = 12.sp,
                                    fontFamily = StremioFontFamily
                                )
                            }
                            if (!item.cast.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Cast: ${item.cast.take(4).joinToString(", ")}",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = StremioFontFamily
                                )
                            }
                        }
                    }
                }

                // 3. For Series: Seasons & Episodes Picker
                if (item.type == "series" && !item.videos.isNullOrEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 22.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Episodes",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = StremioFontFamily
                                )
                                Text(
                                    text = "${seasonNumbers.size} Seasons",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = StremioFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

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
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            activeSeasonNum = sNum
                                            viewModel.selectSeason(sNum)
                                            val firstInSeason = episodesBySeason[sNum]?.firstOrNull()
                                            if (firstInSeason != null) {
                                                selectedEpisode = firstInSeason
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = "Season $sNum",
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
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Episodes List
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
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) StremioSurfaceVariant else StremioDark
                                        ),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                if (isCurrent) StremioPurple else StremioBorder
                                            )
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedEpisode = ep
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
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 100.dp, height = 64.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(StremioSurface)
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

                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(4.dp)
                                                ) {
                                                    Text(
                                                        text = "E${ep.episode}",
                                                        color = StremioPurpleLight,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = StremioFontFamily,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ep.title,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontFamily = StremioFontFamily
                                                )
                                                if (!ep.overview.isNullOrEmpty()) {
                                                    Text(
                                                        text = ep.overview,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        fontFamily = StremioFontFamily
                                                    )
                                                }
                                            }

                                            if (isEpWatched) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Watched",
                                                    tint = StreamGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Stremio Streams List (StreamsList from Stremio Web)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stream,
                                contentDescription = null,
                                tint = StremioPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Available Streams",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = StremioFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (currentStreams.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StremioSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = StremioPurpleLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Querying installed addons for streams... Tap 'Watch Now' to direct play.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = StremioFontFamily
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                currentStreams.forEach { streamItem ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = StremioDark),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(StremioBorder)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.navigateToPlayer(
                                                    stream = streamItem,
                                                    title = item.name,
                                                    subtitle = selectedEpisode?.let { "S${it.season}:E${it.episode} • ${it.title}" } ?: item.releaseInfo,
                                                    itemId = item.id,
                                                    startPositionMs = resumePositionMs,
                                                    meta = item,
                                                    episode = selectedEpisode
                                                )
                                            }
                                            .testTag("stream_item_${streamItem.addonName ?: "core"}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(StremioPurple.copy(alpha = 0.25f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = StremioPurpleLight,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = streamItem.title ?: streamItem.name ?: "Stremio Stream",
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        fontFamily = StremioFontFamily
                                                    )
                                                    Text(
                                                        text = "${streamItem.addonName ?: "Stremio Core"} • ${streamItem.name ?: "Direct Stream"}",
                                                        color = TextMuted,
                                                        fontSize = 11.sp,
                                                        fontFamily = StremioFontFamily
                                                    )
                                                }
                                            }

                                            // Quality badge
                                            Surface(
                                                color = StremioSurfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = streamItem.quality ?: "1080p",
                                                    color = StremioPurpleLight,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = StremioFontFamily,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
