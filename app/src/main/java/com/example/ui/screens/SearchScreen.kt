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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (id: String, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.searchFilterType.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val installedAddons by viewModel.installedAddons.collectAsState()

    val popularSuggestions = listOf(
        "Stranger Things",
        "Severance",
        "The Mandalorian",
        "House of the Dragon",
        "The Boys",
        "Ted Lasso",
        "The Bear",
        "Big Buck Bunny",
        "Sintel",
        "Yellowstone"
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
                // Search Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field"),
                    placeholder = {
                        Text(
                            "Search movies, series, addons...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (query.isNotEmpty()) StreamCyan else TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StreamSurfaceVariant,
                        unfocusedContainerColor = StreamSurfaceVariant,
                        focusedBorderColor = StreamCyan,
                        unfocusedBorderColor = StreamBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Type filter pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        "all" to "All Types",
                        "movie" to "Movies",
                        "series" to "Series",
                        "channel" to "Channels"
                    )
                    types.forEach { (key, label) ->
                        val isSelected = filterType == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSearchFilterType(key) },
                            label = { Text(label, fontSize = 11.sp) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = StreamCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Querying ${installedAddons.count { it.isEnabled }} active addons...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (query.isBlank()) {
                // Initial state: Suggestions & Tips
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Suggested Searches",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display suggestions chips
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        popularSuggestions.chunked(3).forEach { chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                chunk.forEach { s ->
                                    Surface(
                                        onClick = { viewModel.setSearchQuery(s) },
                                        shape = RoundedCornerShape(20.dp),
                                        color = StreamSurfaceVariant,
                                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StreamBorder))
                                    ) {
                                        Text(
                                            text = s,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Connected Addons Tip Box
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = StreamSurfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = StreamCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search queries all installed addons simultaneously. You can install more catalogs in the Addons tab.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else if (searchResults.isEmpty()) {
                // No Results
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results for \"$query\"",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Check the spelling or try searching another title. You can also add more catalog addons.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Results Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("search_results_grid")
                ) {
                    items(searchResults, key = { it.id }) { item ->
                        MediaPosterCard(
                            item = item,
                            onClick = {
                                onNavigateToDetail(item.id, item.type)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
