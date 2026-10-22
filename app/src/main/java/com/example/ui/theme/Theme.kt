package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WuPlayPurple,
    onPrimary = Color.White,
    primaryContainer = StreamSurfaceVariant,
    onPrimaryContainer = WuPlayCyan,
    secondary = WuPlayCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF281E48),
    onSecondaryContainer = WuPlayPurple,
    tertiary = StreamCoral,
    onTertiary = Color.White,
    background = StreamBg,
    onBackground = TextPrimary,
    surface = StreamSurface,
    onSurface = TextPrimary,
    surfaceVariant = StreamSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StreamBorder,
    outlineVariant = StreamBorderGlow
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
