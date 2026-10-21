package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AddonManifest
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AddonsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val installedAddons by viewModel.installedAddons.collectAsState()
    val installUrl by viewModel.installUrl.collectAsState()
    val isInstalling by viewModel.isInstallingAddon.collectAsState()
    val addonMessage by viewModel.addonMessage.collectAsState()

    var showInstallDialog by remember { mutableStateOf(false) }
    var selectedAddonDetail by remember { mutableStateOf<AddonManifest?>(null) }

    val sampleUrls = listOf(
        "Official Cinemeta" to "https://v3-cinemeta.strem.io/manifest.json",
        "OpenSubtitles v3" to "https://opensubtitles-v3.strem.io/manifest.json",
        "WatchHub Catalog" to "https://watchhub.strem.io/manifest.json"
    )

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
                        Text(
                            text = "Addon Manager",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${installedAddons.count { it.isEnabled }} active of ${installedAddons.size} total",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { showInstallDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("install_addon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Addon", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                .padding(horizontal = 16.dp)
                .testTag("addons_list")
        ) {
            // Notification / Status Banner
            if (addonMessage != null) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StreamSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = StreamCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = addonMessage!!,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearAddonMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Addon Info Hero
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StreamCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(StreamCyan.copy(alpha = 0.4f), StreamPurple.copy(alpha = 0.4f))))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = StreamCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Stremio Protocol v3",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Addons supply catalogs, metadata, and streams. NovaStream is fully compatible with standard community manifest.json endpoints and stremio:// links.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Installed Addons",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.resetAddonsToDefaults() }) {
                        Text("Reset Defaults", color = TextMuted, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Installed Addons List
            items(installedAddons, key = { it.id }) { addon ->
                AddonItemCard(
                    addon = addon,
                    onToggle = { enabled -> viewModel.toggleAddon(addon.id, enabled) },
                    onUninstall = { viewModel.uninstallAddon(addon.id) },
                    onClick = { selectedAddonDetail = addon }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Install Custom Addon Dialog
    if (showInstallDialog) {
        var inputUrl by remember { mutableStateOf(installUrl) }

        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = {
                Text(
                    text = "Install Addon from URL",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter manifest URL (supports https:// or stremio://):",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("addon_url_input"),
                        placeholder = { Text("https://example.com/manifest.json", fontSize = 12.sp, color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = StreamSurfaceVariant,
                            unfocusedContainerColor = StreamSurfaceVariant,
                            focusedBorderColor = StreamCyan,
                            unfocusedBorderColor = StreamBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quick Sample Addons:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    sampleUrls.forEach { (name, url) ->
                        Surface(
                            onClick = { inputUrl = url },
                            shape = RoundedCornerShape(8.dp),
                            color = StreamSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, color = StreamCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    if (isInstalling) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = StreamCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Fetching & validating manifest...", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.installAddon(inputUrl)
                        showInstallDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StreamCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    enabled = inputUrl.isNotBlank() && !isInstalling,
                    modifier = Modifier.testTag("dialog_install_button")
                ) {
                    Text("Install", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = StreamSurface
        )
    }

    // Addon Detail BottomSheet/Dialog
    if (selectedAddonDetail != null) {
        val addon = selectedAddonDetail!!
        AlertDialog(
            onDismissRequest = { selectedAddonDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Extension, contentDescription = null, tint = StreamCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(addon.name, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Version: ${addon.version}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Transport: ${addon.transportUrl}", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!addon.description.isNullOrEmpty()) {
                        Text(addon.description, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text("Supported Types: ${addon.types.joinToString(", ")}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Catalogs: ${addon.catalogs.size}", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAddonDetail = null }) {
                    Text("Close", color = StreamCyan)
                }
            },
            containerColor = StreamSurface
        )
    }
}

@Composable
fun AddonItemCard(
    addon: AddonManifest,
    onToggle: (Boolean) -> Unit,
    onUninstall: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StreamCard),
        modifier = modifier
            .fillMaxWidth()
            .testTag("addon_card_${addon.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Addon Logo / Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StreamSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!addon.logo.isNullOrEmpty()) {
                        AsyncImage(
                            model = addon.logo,
                            contentDescription = addon.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = StreamCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = addon.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "v${addon.version}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (addon.isBuiltin) "Official Built-in Addon" else "Community Custom Addon",
                        color = if (addon.isBuiltin) StreamCyanDim else StreamPurple,
                        fontSize = 11.sp
                    )
                }

                // Active Switch
                Switch(
                    checked = addon.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = StreamCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = StreamSurfaceVariant
                    ),
                    modifier = Modifier.testTag("addon_toggle_${addon.id}")
                )
            }

            if (!addon.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = addon.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Resource badges & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (addon.supportsResource("catalog")) {
                        ResourceBadge(label = "CATALOG", color = StreamCyan)
                    }
                    if (addon.supportsResource("meta")) {
                        ResourceBadge(label = "META", color = StreamPurple)
                    }
                    if (addon.supportsResource("stream")) {
                        ResourceBadge(label = "STREAM", color = StreamGreen)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!addon.isBuiltin) {
                        IconButton(
                            onClick = onUninstall,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Uninstall",
                                tint = StreamCoral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
