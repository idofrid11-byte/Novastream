package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StreamCyan,
    onPrimary = Color.Black,
    primaryContainer = StreamSurfaceVariant,
    onPrimaryContainer = StreamCyan,
    secondary = StreamPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A1F45),
    onSecondaryContainer = StreamPurple,
    tertiary = StreamCoral,
    onTertiary = Color.White,
    background = StreamBg,
    onBackground = TextPrimary,
    surface = StreamSurface,
    onSurface = TextPrimary,
    surfaceVariant = StreamSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StreamBorder,
    outlineVariant = Color(0xFF222538)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Forced custom cinematic streaming theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
