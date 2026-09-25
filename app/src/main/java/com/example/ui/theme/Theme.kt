package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyanPrimary,
    onPrimary = CyberBackground,
    primaryContainer = CyberSurfaceCard,
    onPrimaryContainer = CyberCyanPrimary,
    secondary = CyberBlueSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = CyberAccentPurple,
    onTertiary = TextPrimary,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = AlertRed,
    onError = TextPrimary
)

private val LightColorScheme = DarkColorScheme // Network security tools look best in cyber dark mode

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep high-contrast cyber theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CyberBackground.toArgb()
                window.navigationBarColor = CyberBackground.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
