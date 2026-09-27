package com.example.guardian.parent.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C4DF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7DEFF),
    onPrimaryContainer = Color(0xFF20005C),
    secondary = Color(0xFF00BCD4),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFFFF6D3B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCF),
    surfaceVariant = Color(0xFFE7E0F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC7B8FF),
    onPrimary = Color(0xFF33108A),
    primaryContainer = Color(0xFF4B2DD1),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF4DD0E1),
    secondaryContainer = Color(0xFF00505B),
    tertiary = Color(0xFFFFB59B),
    tertiaryContainer = Color(0xFFB33217),
)

@Composable
fun GuardianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep our own vibrant brand palette instead of the device's Material You wallpaper colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
