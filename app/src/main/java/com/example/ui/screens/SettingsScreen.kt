package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackSettings by viewModel.playbackSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isSyncing by viewModel.isSyncingCloud.collectAsState()
    val installedAddons by viewModel.installedAddons.collectAsState()
    val installUrl by viewModel.installUrl.collectAsState()
    val isInstallingAddon by viewModel.isInstallingAddon.collectAsState()
    val addonMessage by viewModel.addonMessage.collectAsState()

    var showSignOutDialog by remember { mutableStateOf(false) }

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
                                text = "Settings",
                                color = WuPlayCyan,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Playback preferences, account & addons",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    // Reset button
                    TextButton(
                        onClick = { viewModel.resetPlaybackSettings() },
                        colors = ButtonDefaults.textButtonColors(contentColor = WuPlayCyan)
                    ) {
                        Text("Reset Defaults", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = StreamBg,
        modifier = modifier.testTag("settings_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // 1. PLAYBACK SETTINGS SECTION HEADER
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = WuPlayPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Playback Engine Preferences", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Card 1: Video Playback & Behavior
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StreamCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(StreamBorder, StreamBorderGlow))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Playback Speed
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Default Playback Speed", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("${playbackSettings.playbackSpeed}x", color = WuPlayCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                speeds.forEach { sp ->
                                    val isSelected = playbackSettings.playbackSpeed == sp
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updatePlaybackSpeed(sp) },
                                        label = { Text("${sp}x", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = StreamBorder)

                        // Aspect Ratio Mode
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Default Aspect Ratio", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                val modeLabel = when (playbackSettings.resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill Screen"
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop / Zoom"
                                    else -> "Fit (Default)"
                                }
                                Text(modeLabel, color = WuPlayCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val modes = listOf(
                                    Triple(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit", Icons.Default.AspectRatio),
                                    Triple(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Stretch", Icons.Default.Fullscreen),
                                    Triple(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom", Icons.Default.Crop)
                                )
                                modes.forEach { (mode, label, icon) ->
                                    val isSelected = playbackSettings.resizeMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setResizeMode(mode) },
                                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WuPlayPurple,
                                            selectedLabelColor = Color.White,
                                            containerColor = StreamSurfaceVariant,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = StreamBorder)

                        // Auto-Play Next Episode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Play Next Episode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Automatically starts the next episode when current finishes", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = playbackSettings.autoPlayNextEpisode,
                                onCheckedChange = { viewModel.setAutoPlayNextEpisode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WuPlayPurple)
                            )
                        }

                        HorizontalDivider(color = StreamBorder)

                        // Auto-Skip Intro Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Skip Intro & Recaps", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Seamlessly bypass TV intros for binge watching", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = playbackSettings.autoSkipIntro,
                                onCheckedChange = { viewModel.setAutoSkipIntro(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WuPlayPurple)
                            )
                        }

                        HorizontalDivider(color = StreamBorder)

                        // Hardware Acceleration Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hardware Video Acceleration", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Uses GPU direct decoding for ultra-smooth 4K playback", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = playbackSettings.hardwareAcceleration,
                                onCheckedChange = { viewModel.setHardwareAcceleration(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WuPlayCyan)
                            )
                        }

                        HorizontalDivider(color = StreamBorder)

                        // Audio Passthrough Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Audio Dialogue Enhancement", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Boosts vocal frequencies and speech intelligibility", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = playbackSettings.audioPassthrough,
                                onCheckedChange = { viewModel.setAudioPassthrough(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WuPlayPurple)
                            )
                        }

                        HorizontalDivider(color = StreamBorder)

                        // Stream Buffer Cache
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Stream Buffer Cache", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("${playbackSettings.bufferCacheSeconds} Seconds", color = WuPlayCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(30 to "30s (Low)", 60 to "60s (Recommended)", 120 to "120s (High)").forEach { (sec, label) ->
                                    val isSelected = playbackSettings.bufferCacheSeconds == sec
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setBufferCacheSeconds(sec) },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WuPlayCyan,
                                            selectedLabelColor = Color.Black,
                                            containerColor = StreamSurfaceVariant,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Subtitle Settings
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StreamCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(StreamBorder, StreamBorderGlow))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = WuPlayCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Subtitles Preferences", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        // Default Subtitle Language
                        Column {
                            Text("Default Language", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val languages = listOf("eng" to "English", "spa" to "Spanish", "fre" to "French", "ger" to "German", "jpn" to "Japanese", "off" to "Off")
                                languages.forEach { (code, label) ->
                                    val isSelected = playbackSettings.defaultSubtitleLang == code
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setDefaultSubtitleLang(code) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WuPlayPurple,
                                            selectedLabelColor = Color.White,
                                            containerColor = StreamSurfaceVariant,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        // Subtitle Size
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtitle Font Size", color = TextSecondary, fontSize = 12.sp)
                                Text("${playbackSettings.subtitleFontSize} sp", color = WuPlayCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(14 to "Small", 18 to "Normal", 22 to "Large", 26 to "Extra Large").forEach { (sz, label) ->
                                    val isSelected = playbackSettings.subtitleFontSize == sz
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setSubtitleFontSize(sz) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WuPlayPurple,
                                            selectedLabelColor = Color.White,
                                            containerColor = StreamSurfaceVariant,
                                            labelColor = TextSecondary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. WUPLAY CLOUD ACCOUNT & WATCHLIST SYNC
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = WuPlayCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WuPlay Cloud & Watchlist Sync", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StreamCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(WuPlayPurple, WuPlayCyan))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (currentUser != null) {
                            // Signed in State
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(WuPlayPurple.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = WuPlayCyan, modifier = Modifier.size(36.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentUser?.displayName ?: "WuPlay Member",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentUser?.email ?: "Synced with Firestore",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncLibraryWithCloud() },
                                    enabled = !isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = WuPlayPurple, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isSyncing) "Syncing..." else "Sync Watchlist", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showSignOutDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StreamCoral),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sign Out", fontSize = 12.sp)
                                }
                            }
                        } else {
                            // Signed out state
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Backup Watchlist Across Devices",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sign in with Google to sync your saved titles, watch progress, and custom addons.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        if (context is Activity) {
                                            viewModel.signInWithGoogle(context)
                                        }
                                    },
                                    enabled = !isAuthLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = WuPlayCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. ADDON ENGINE SUMMARY & MANAGEMENT
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Extension, contentDescription = null, tint = WuPlayPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WuPlay Addon Engine", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StreamCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(StreamBorder, StreamBorderGlow))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Community Addons", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Surface(color = StreamSurfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "${installedAddons.count { it.isEnabled }} Installed",
                                    color = WuPlayCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Addon list chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            installedAddons.forEach { addon ->
                                Surface(
                                    color = if (addon.isEnabled) WuPlayPurple.copy(alpha = 0.25f) else StreamSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(
                                            if (addon.isEnabled) listOf(WuPlayPurple, WuPlayCyan) else listOf(StreamBorder, StreamBorder)
                                        )
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (addon.isEnabled) StreamGreen else TextMuted)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = addon.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Restore Defaults Button
                        OutlinedButton(
                            onClick = { viewModel.resetAddonsToDefaults() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Addons to Defaults", fontSize = 12.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out of WuPlay?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Your local saved items and progress will remain intact on this device.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showSignOutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StreamCoral)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = StreamSurface
        )
    }
}
