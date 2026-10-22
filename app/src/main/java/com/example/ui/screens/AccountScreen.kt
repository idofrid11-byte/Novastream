package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AccountScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    SettingsScreen(viewModel = viewModel, modifier = modifier)
}
