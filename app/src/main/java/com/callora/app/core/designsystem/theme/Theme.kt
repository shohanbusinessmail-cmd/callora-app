package com.callora.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.callora.app.core.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Navy700,
    onPrimaryContainer = Teal300,
    secondary = DarkSecondary,
    onSecondary = Navy950,
    secondaryContainer = Navy800,
    background = Navy900,
    onBackground = Color(0xFFE6EEF8),
    surface = Navy850,
    onSurface = Color(0xFFE6EEF8),
    surfaceVariant = Navy800,
    onSurfaceVariant = Color(0xFFA9B8CC),
    surfaceTint = Teal400,
    outline = Navy600,
    error = StatusError,
    outlineVariant = Navy700,
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF03332E),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    background = Neutral50,
    onBackground = Neutral900,
    surface = Color.White,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral700,
    surfaceTint = LightPrimary,
    outline = Neutral200,
    error = StatusError,
    outlineVariant = Neutral200,
)

@Composable
fun CalloraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                window.statusBarColor = colors.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = CalloraTypography,
        shapes = CalloraShapes,
        content = content,
    )
}
