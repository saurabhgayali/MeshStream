package com.meshstream.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),      // Light blue — primary action
    secondary = Color(0xFF81C784),    // Green — active/recording indicator
    tertiary = Color(0xFFFFB74D),     // Amber — warning / low storage
    background = Color(0xFF121212),   // Dark background for low-light use
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFCF6679),
)

/**
 * MeshStream uses a dark theme by default — the app is designed for low-light
 * field environments and to minimise screen brightness (battery saving).
 */
@Composable
fun MeshStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
