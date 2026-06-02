package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBD00FF),      // Cyber purple
    secondary = Color(0xFF00F0FF),    // Neon cyan
    tertiary = Color(0xFF10B981),     // Bright safety green
    background = Color(0xFF0F0B1E),   // Deep cosmic background
    surface = Color(0xFF1C1635),      // Nebula layered surface
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFE2E0EC),
    onSurface = Color(0xFFE2E0EC)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFBD00FF),
    secondary = Color(0xFF00F0FF),
    tertiary = Color(0xFF10B981),
    background = Color(0xFF0F0B1E),   // Standardizing dark space background for arcade feel
    surface = Color(0xFF1C1635),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFE2E0EC),
    onSurface = Color(0xFFE2E0EC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
