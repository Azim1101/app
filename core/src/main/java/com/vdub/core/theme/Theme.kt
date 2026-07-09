package com.vdub.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VDubPrimary,
    onPrimary = VDubOnPrimary,
    primaryContainer = VDubPrimaryContainer,
    onPrimaryContainer = VDubOnPrimaryContainer,
    secondary = VDubSecondary,
    onSecondary = VDubOnSecondary,
    secondaryContainer = VDubSecondaryContainer,
    onSecondaryContainer = VDubOnSecondaryContainer,
    tertiary = VDubTertiary,
    onTertiary = VDubOnTertiary,
    tertiaryContainer = VDubTertiaryContainer,
    onTertiaryContainer = VDubOnTertiaryContainer,
    error = VDubError,
    onError = VDubOnError,
    errorContainer = VDubErrorContainer,
    onErrorContainer = VDubOnErrorContainer,
    background = VDubBackground,
    onBackground = VDubOnBackground,
    surface = VDubSurface,
    onSurface = VDubOnSurface,
    surfaceVariant = VDubSurfaceVariant,
    onSurfaceVariant = VDubOnSurfaceVariant,
    outline = VDubOutline,
    outlineVariant = VDubOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = VDubDarkPrimary,
    onPrimary = VDubDarkOnPrimary,
    primaryContainer = VDubDarkPrimaryContainer,
    onPrimaryContainer = VDubDarkOnPrimaryContainer,
    secondary = VDubDarkSecondary,
    onSecondary = VDubDarkOnSecondary,
    secondaryContainer = VDubDarkSecondaryContainer,
    onSecondaryContainer = VDubDarkOnSecondaryContainer,
    tertiary = VDubDarkTertiary,
    onTertiary = VDubDarkOnTertiary,
    tertiaryContainer = VDubDarkTertiaryContainer,
    onTertiaryContainer = VDubDarkOnTertiaryContainer,
    error = VDubDarkError,
    onError = VDubDarkOnError,
    errorContainer = VDubDarkErrorContainer,
    onErrorContainer = VDubDarkOnErrorContainer,
    background = VDubDarkBackground,
    onBackground = VDubDarkOnBackground,
    surface = VDubDarkSurface,
    onSurface = VDubDarkOnSurface,
    surfaceVariant = VDubDarkSurfaceVariant,
    onSurfaceVariant = VDubDarkOnSurfaceVariant,
    outline = VDubDarkOutline,
    outlineVariant = VDubDarkOutlineVariant
)

@Composable
fun VDubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VDubTypography,
        content = content
    )
}
