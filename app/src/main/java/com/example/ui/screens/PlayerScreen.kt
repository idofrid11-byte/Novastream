package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.AudioSettings
import com.example.data.model.MetaDetail
import com.example.data.model.StreamItem
import com.example.data.model.SubtitleSettings
import com.example.data.model.VideoEpisode
import com.example.data.subtitles.SubtitleProvider
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    stream: StreamItem,
    title: String,
    subtitle: String?,
    itemId: String,
    startPositionMs: Long = 0L,
    meta: MetaDetail? = null,
    episode: VideoEpisode? = null,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Setup ExoPlayer
    val exoPlayer = remember(stream.url) {
        ExoPlayer.Builder(context).build().apply {
            val mediaUrl = stream.url ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            val mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl))
            setMediaItem(mediaItem)
            prepare()
            if (startPositionMs > 0) {
                seekTo(startPositionMs)
            }
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(startPositionMs) }
    var durationMs by remember { mutableStateOf(0L) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Subtitle & Audio Settings State
    var subtitleSettings by remember {
        mutableStateOf(
            SubtitleSettings(
                isEnabled = true,
                selectedLanguage = "eng",
                fontSizeSp = 18,
                textColor = "white",
                hasBackgroundBox = true,
                delayOffsetMs = 0L
            )
        )
    }

    var audioSettings by remember {
        mutableStateOf(
            AudioSettings(
                selectedTrack = "default",
                volumeBoostPercent = 100,
                audioDelayMs = 0L
            )
        )
    }

    var showSettingsModal by remember { mutableStateOf(false) }
    var selectedModalTab by remember { mutableStateOf(0) } // 0: Subtitles, 1: Audio

    // Apply Audio Volume Boost
    LaunchedEffect(audioSettings.volumeBoostPercent) {
        exoPlayer.volume = (audioSettings.volumeBoostPercent / 100f).coerceIn(0f, 2.0f)
    }

    // Intercept back button to save progress and exit
    BackHandler {
        viewModel.updateProgress(
            id = itemId,
            positionMs = exoPlayer.currentPosition,
            durationMs = exoPlayer.duration.coerceAtLeast(0L),
            episodeId = episode?.id,
            episodeTitle = episode?.title
        )
        exoPlayer.release()
        onBack()
    }

    // Player state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            viewModel.updateProgress(
                id = itemId,
                positionMs = exoPlayer.currentPosition,
                durationMs = exoPlayer.duration.coerceAtLeast(0L),
                episodeId = episode?.id,
                episodeTitle = episode?.title
            )
            exoPlayer.release()
        }
    }

    // Periodic time polling & progress save
    LaunchedEffect(isPlaying) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(0L)

                viewModel.updateProgress(
                    id = itemId,
                    positionMs = currentPositionMs,
                    durationMs = durationMs,
                    episodeId = episode?.id,
                    episodeTitle = episode?.title
                )
            }
            delay(1000)
        }
    }

    // Auto-hide controls after 4 seconds (pause timer if settings modal is open)
    LaunchedEffect(areControlsVisible, isPlaying, showSettingsModal) {
        if (areControlsVisible && isPlaying && !showSettingsModal) {
            delay(4000)
            areControlsVisible = false
        }
    }

    fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d:%02d", minutes, seconds)
        }
    }

    // Subtitle Cues resolution
    val currentCues = remember(itemId, subtitleSettings.selectedLanguage) {
        SubtitleProvider.getCues(itemId, subtitleSettings.selectedLanguage)
    }

    val effectiveSubtitleTime = currentPositionMs + subtitleSettings.delayOffsetMs
    val activeCue = remember(effectiveSubtitleTime, subtitleSettings.isEnabled, currentCues) {
        if (subtitleSettings.isEnabled) {
            currentCues.find { effectiveSubtitleTime in it.startMs..it.endMs }
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
            .testTag("video_player_container")
    ) {
        // Media3 ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.resizeMode = resizeMode
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // ON-SCREEN LIVE SUBTITLES OVERLAY
        if (activeCue != null) {
            val textColor = when (subtitleSettings.textColor) {
                "yellow" -> Color(0xFFFFEB3B)
                "cyan" -> StreamCyan
                "green" -> Color(0xFF00E676)
                else -> Color.White
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (areControlsVisible) 110.dp else 45.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = if (subtitleSettings.hasBackgroundBox) Color.Black.copy(alpha = 0.82f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("subtitle_display_box")
                ) {
                    Text(
                        text = activeCue.text,
                        color = textColor,
                        fontSize = subtitleSettings.fontSizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Custom Overlay Controls
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.8f),
                            0.22f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.9f)
                        )
                    )
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.updateProgress(
                                    id = itemId,
                                    positionMs = exoPlayer.currentPosition,
                                    durationMs = exoPlayer.duration.coerceAtLeast(0L),
                                    episodeId = episode?.id,
                                    episodeTitle = episode?.title
                                )
                                exoPlayer.release()
                                onBack()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("player_back_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (!subtitle.isNullOrEmpty()) {
                                Text(
                                    text = subtitle,
                                    color = StreamCyan,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Top Action Icons: Subtitles, Audio & External Player
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Subtitles Settings Button
                        IconButton(
                            onClick = {
                                selectedModalTab = 0
                                showSettingsModal = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (subtitleSettings.isEnabled) StreamCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f))
                                .testTag("player_subtitles_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (subtitleSettings.isEnabled) StreamCyan else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Audio Settings Button
                        IconButton(
                            onClick = {
                                selectedModalTab = 1
                                showSettingsModal = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (audioSettings.volumeBoostPercent > 100) StreamPurple.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f))
                                .testTag("player_audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Audio Settings",
                                tint = if (audioSettings.volumeBoostPercent > 100) StreamPurple else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // External Player
                        IconButton(
                            onClick = {
                                val mediaUrl = stream.url ?: stream.externalUrl
                                if (!mediaUrl.isNullOrEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(mediaUrl), "video/*")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(intent, "Open with player"))
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("open_external_player")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "External Player",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // CENTER CONTROLS (Rewind, Play/Pause, Fast Forward)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(StreamCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(32.dp))
                        } else {
                            IconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier.size(68.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            val dur = exoPlayer.duration
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000L).coerceAtMost(if (dur > 0) dur else Long.MAX_VALUE))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // BOTTOM CONTROLS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = formatTime(durationMs),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Scrubbing Slider
                    val sliderProgress = if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = sliderProgress,
                        onValueChange = { frac ->
                            if (durationMs > 0) {
                                val target = (frac * durationMs).toLong()
                                currentPositionMs = target
                                exoPlayer.seekTo(target)
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = StreamCyan,
                            activeTrackColor = StreamCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("player_seek_slider")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Secondary Tools Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Subtitles & Audio Chips
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    selectedModalTab = 0
                                    showSettingsModal = true
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (subtitleSettings.isEnabled) StreamCyan.copy(alpha = 0.2f) else StreamSurfaceVariant,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(if (subtitleSettings.isEnabled) StreamCyan else StreamBorder)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = null,
                                        tint = if (subtitleSettings.isEnabled) StreamCyan else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (subtitleSettings.isEnabled) subtitleSettings.selectedLanguage.uppercase() else "CC Off",
                                        color = if (subtitleSettings.isEnabled) StreamCyan else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    selectedModalTab = 1
                                    showSettingsModal = true
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (audioSettings.volumeBoostPercent > 100) StreamPurple.copy(alpha = 0.2f) else StreamSurfaceVariant
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = if (audioSettings.volumeBoostPercent > 100) StreamPurple else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${audioSettings.volumeBoostPercent}%",
                                        color = if (audioSettings.volumeBoostPercent > 100) StreamPurple else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Speed & Aspect Ratio
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    currentSpeed = when (currentSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        2.0f -> 0.75f
                                        else -> 1.0f
                                    }
                                    exoPlayer.playbackParameters = PlaybackParameters(currentSpeed)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Text("${currentSpeed}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SUBTITLES & AUDIO SETTINGS MODAL
        if (showSettingsModal) {
            AlertDialog(
                onDismissRequest = { showSettingsModal = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playback Settings",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showSettingsModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Settings Tab Switcher (Subtitles vs Audio)
                        TabRow(
                            selectedTabIndex = selectedModalTab,
                            containerColor = StreamSurfaceVariant,
                            contentColor = StreamCyan,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Tab(
                                selected = selectedModalTab == 0,
                                onClick = { selectedModalTab = 0 },
                                text = { Text("Subtitles (CC)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                            Tab(
                                selected = selectedModalTab == 1,
                                onClick = { selectedModalTab = 1 },
                                text = { Text("Audio", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedModalTab == 0) {
                            // SUBTITLE CONTROLS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Enable Subtitles", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Display synchronized text cues", color = TextMuted, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = subtitleSettings.isEnabled,
                                    onCheckedChange = { subtitleSettings = subtitleSettings.copy(isEnabled = it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = StreamCyan)
                                )
                            }

                            if (subtitleSettings.isEnabled) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Language Track", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))

                                SubtitleProvider.AVAILABLE_LANGUAGES.forEach { (code, label) ->
                                    val isSelected = subtitleSettings.selectedLanguage == code
                                    Surface(
                                        onClick = { subtitleSettings = subtitleSettings.copy(selectedLanguage = code) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) StreamCyan.copy(alpha = 0.2f) else StreamSurfaceVariant,
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) StreamCyan else Color.Transparent)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, color = if (isSelected) StreamCyan else TextPrimary, fontSize = 12.sp)
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = StreamCyan, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Text Size", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val sizes = listOf("Small" to 14, "Normal" to 18, "Large" to 22, "XL" to 26)
                                    sizes.forEach { (name, size) ->
                                        val isSelected = subtitleSettings.fontSizeSp == size
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { subtitleSettings = subtitleSettings.copy(fontSizeSp = size) },
                                            label = { Text(name, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = StreamCyan,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Subtitle Color", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val colors = listOf("white" to Color.White, "yellow" to Color(0xFFFFEB3B), "cyan" to StreamCyan, "green" to Color(0xFF00E676))
                                    colors.forEach { (key, clr) ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(clr)
                                                .border(
                                                    2.dp,
                                                    if (subtitleSettings.textColor == key) Color.White else Color.Transparent,
                                                    CircleShape
                                                )
                                                .clickable { subtitleSettings = subtitleSettings.copy(textColor = key) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Timing Sync Offset", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val offsets = listOf(-500L to "-0.5s", -250L to "-0.25s", 0L to "0.0s", 250L to "+0.25s", 500L to "+0.5s")
                                    offsets.forEach { (offset, text) ->
                                        val isSelected = subtitleSettings.delayOffsetMs == offset
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { subtitleSettings = subtitleSettings.copy(delayOffsetMs = offset) },
                                            label = { Text(text, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = StreamPurple,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // AUDIO SETTINGS
                            Text("Audio Track", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))

                            val tracks = listOf(
                                "default" to "Default Audio (Direct Stream)",
                                "stereo" to "Stereo AAC (Clear Dialogue)",
                                "surround_51" to "5.1 Surround Sound",
                                "night_mode" to "Night Mode (Dynamic Compression)"
                            )

                            tracks.forEach { (code, name) ->
                                val isSelected = audioSettings.selectedTrack == code
                                Surface(
                                    onClick = { audioSettings = audioSettings.copy(selectedTrack = code) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) StreamPurple.copy(alpha = 0.2f) else StreamSurfaceVariant,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) StreamPurple else Color.Transparent)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, color = if (isSelected) StreamPurple else TextPrimary, fontSize = 12.sp)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = StreamPurple, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Text("Volume Boost (Night / Dialogue Gain)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Amplifies quiet audio dialogues", color = TextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val boosts = listOf(100 to "100%", 125 to "125%", 150 to "150%", 200 to "200%")
                                boosts.forEach { (pct, label) ->
                                    val isSelected = audioSettings.volumeBoostPercent == pct
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { audioSettings = audioSettings.copy(volumeBoostPercent = pct) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StreamCyan,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Text("Audio Sync Offset", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val audioOffsets = listOf(-500L to "-0.5s", -250L to "-0.25s", 0L to "0.0s", 250L to "+0.25s", 500L to "+0.5s")
                                audioOffsets.forEach { (offset, text) ->
                                    val isSelected = audioSettings.audioDelayMs == offset
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { audioSettings = audioSettings.copy(audioDelayMs = offset) },
                                        label = { Text(text, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StreamPurple,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSettingsModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = StreamCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply & Close", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = StreamSurface
            )
        }
    }
}
