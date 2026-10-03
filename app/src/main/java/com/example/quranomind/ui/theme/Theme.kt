package com.example.quranomind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = NavyDark,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color.White,
    secondary = GoldMuted,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = GoldAccent,
    background = DarkBackground,
    onBackground = Color(0xFFE2E8F0),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2EDF8),
    onPrimaryContainer = NavyPrimary,
    secondary = GoldMuted,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = Color(0xFF533F05),
    background = SoftGray,
    onBackground = Color(0xFF1A202C),
    surface = Color.White,
    onSurface = Color(0xFF1A202C),
    surfaceVariant = OffWhite,
    onSurfaceVariant = Color(0xFF4A5568)
)

@Composable
fun QuranoMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
