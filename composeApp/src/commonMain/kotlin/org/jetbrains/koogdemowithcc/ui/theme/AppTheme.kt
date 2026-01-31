package org.jetbrains.koogdemowithcc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Modern dark color scheme inspired by ChatGPT/Claude
 */
private val DarkColorScheme = darkColorScheme(
    // Main background - dark gray like ChatGPT
    background = Color(0xFF212121),
    surface = Color(0xFF212121),
    surfaceVariant = Color(0xFF2F2F2F),

    // Cards and containers
    surfaceContainer = Color(0xFF2F2F2F),
    surfaceContainerHigh = Color(0xFF3A3A3A),
    surfaceContainerLow = Color(0xFF1A1A1A),

    // Primary - teal/green accent
    primary = Color(0xFF10A37F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF10A37F),
    onPrimaryContainer = Color.White,

    // Secondary - muted
    secondary = Color(0xFF8E8E8E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFE5E5E5),

    // Tertiary
    tertiary = Color(0xFF10A37F),
    onTertiary = Color.White,

    // Text colors
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFFB4B4B4),

    // Outline
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF3A3A3A),

    // Error
    error = Color(0xFFEF4444),
    onError = Color.White,

    // Inverse
    inverseSurface = Color(0xFFE5E5E5),
    inverseOnSurface = Color(0xFF212121),
    inversePrimary = Color(0xFF0D8A6A)
)

/**
 * Light color scheme (optional, for future use)
 */
private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF7F7F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFF7F7F8),

    primary = Color(0xFF10A37F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF10A37F),
    onPrimaryContainer = Color.White,

    secondary = Color(0xFF6B6B6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEFEF),
    onSecondaryContainer = Color(0xFF2D2D2D),

    onBackground = Color(0xFF2D2D2D),
    onSurface = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFF6B6B6B),

    outline = Color(0xFFD9D9D9),
    outlineVariant = Color(0xFFEFEFEF)
)

/**
 * App theme with modern ChatGPT-like colors
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = true, // Default to dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
