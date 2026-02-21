package de.tswebula.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dunkles Bahn-Farbschema
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),          // Hellblau (DB-ähnlich)
    onPrimary = Color(0xFF00213A),
    primaryContainer = Color(0xFF00456E),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF00391A),
    background = Color(0xFF12121F),       // Sehr dunkel
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1B1B2F),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E2A3A),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF37474F),
    error = Color(0xFFEF5350),
    onError = Color(0xFF4A0000)
)

@Composable
fun TSWEBulaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
