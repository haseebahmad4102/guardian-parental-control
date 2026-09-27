package com.example.guardian.child.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00BFA5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F1E4),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7DEFF),
    tertiary = Color(0xFFFF7043),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCF),
    surfaceVariant = Color(0xFFD9E6E2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF52DEC4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFFB4F1E4),
    secondary = Color(0xFFCBBEFF),
    secondaryContainer = Color(0xFF4A2FBF),
    tertiary = Color(0xFFFFB59B),
    tertiaryContainer = Color(0xFFB3441E),
)

@Composable
fun GuardianChildTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
