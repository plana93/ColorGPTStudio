package com.example.colorgptstudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Schema Dark: "Studio Digitale" ──────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary           = AccentBlue,
    onPrimary         = DarkBackground,
    primaryContainer  = AccentBlueDark,
    onPrimaryContainer = AccentBlueLight,
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = DarkSurfaceVar,
    onSurfaceVariant  = DarkOnSurfaceVar,
    outline           = DarkOutline,
    error             = ColorError,
)

// ─── Schema Light ─────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary           = AccentBlue,
    onPrimary         = LightBackground,
    primaryContainer  = AccentBlueLight,
    onPrimaryContainer = AccentBlueDark,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = LightSurfaceVar,
    onSurfaceVariant  = LightOnSurfaceVar,
    outline           = LightOutline,
    error             = ColorError,
)

@Composable
fun ColorGPTStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
