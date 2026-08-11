package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = JarvisBackground,
    primaryContainer = JarvisSurfaceVariant,
    onPrimaryContainer = CyanGlow,
    secondary = AmberGlow,
    onSecondary = JarvisBackground,
    tertiary = PurpleGlow,
    background = JarvisBackground,
    onBackground = TextPrimary,
    surface = JarvisSurface,
    onSurface = TextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = JarvisCardBorder
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = JarvisBackground.toArgb()
            window.navigationBarColor = JarvisBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
