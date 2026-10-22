package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (id: String, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    WatchlistScreen(
        viewModel = viewModel,
        onNavigateToDetail = onNavigateToDetail,
        modifier = modifier
    )
}
