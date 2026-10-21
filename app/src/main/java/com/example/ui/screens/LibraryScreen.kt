package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.LibraryItem
import com.example.data.model.WatchStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (id: String, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryItems by viewModel.libraryItems.collectAsState()
    var selectedStatusFilter by remember { mutableStateOf<WatchStatus?>(null) }

    val filteredItems = remember(libraryItems, selectedStatusFilter) {
        if (selectedStatusFilter == null) libraryItems
        else libraryItems.filter { it.status == selectedStatusFilter }
    }

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
                            text = "My Library",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${libraryItems.size} titles saved",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.switchTab(AppTab.DISCOVER) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StreamSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add titles",
                            tint = StreamCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allSelected = selectedStatusFilter == null
                    FilterChip(
                        selected = allSelected,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All (${libraryItems.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StreamCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = StreamSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = allSelected,
                            borderColor = StreamBorder,
                            selectedBorderColor = StreamCyan
                        )
                    )

                    WatchStatus.values().forEach { status ->
                        val count = libraryItems.count { it.status == status }
                        val isSelected = selectedStatusFilter == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStatusFilter = status },
                            label = { Text("${status.displayName} ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StreamPurple,
                                selectedLabelColor = Color.White,
                                containerColor = StreamSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = StreamBorder,
                                selectedBorderColor = StreamPurple
                            )
                        )
                    }
                }
            }
        },
        containerColor = StreamBg,
        modifier = modifier
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
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No titles in this section",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browse discover or search to save movies, series, and documentaries to your library.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.switchTab(AppTab.DISCOVER) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Browse Discover", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("library_grid")
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    LibraryCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id, item.type) },
                        onStatusChange = { newStatus -> viewModel.setLibraryStatus(item.id, newStatus) },
                        onRemove = { viewModel.removeFromLibrary(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryCard(
    item: LibraryItem,
    onClick: () -> Unit,
    onStatusChange: (WatchStatus) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StreamCard),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("library_item_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(StreamSurfaceVariant)
            ) {
                if (!item.poster.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.poster,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Status chip top left
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = item.status.displayName,
                        color = when (item.status) {
                            WatchStatus.WATCHING -> StreamCyan
                            WatchStatus.FAVORITE -> StreamGold
                            WatchStatus.COMPLETED -> StreamGreen
                            WatchStatus.PLAN_TO_WATCH -> StreamPurple
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // More menu button top right
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .padding(4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(StreamSurfaceVariant)
                    ) {
                        WatchStatus.values().forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.displayName, color = Color.White) },
                                onClick = {
                                    onStatusChange(st)
                                    showMenu = false
                                }
                            )
                        }
                        Divider(color = StreamBorder)
                        DropdownMenuItem(
                            text = { Text("Remove from Library", color = StreamCoral) },
                            onClick = {
                                onRemove()
                                showMenu = false
                            }
                        )
                    }
                }

                // Progress Bar at bottom if watched
                if (item.watchPositionMs > 0) {
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = StreamCyan,
                        trackColor = Color.Black.copy(alpha = 0.5f)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.lastEpisodeTitle ?: item.releaseInfo ?: item.type.replaceFirstChar { it.uppercase() },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
