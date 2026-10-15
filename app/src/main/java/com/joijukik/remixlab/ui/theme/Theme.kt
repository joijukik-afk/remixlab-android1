package com.joijukik.remixlab.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = TextPrimaryDark,
    primaryContainer = PurpleDark,
    onPrimaryContainer = TextPrimaryDark,

    secondary = NeonBlue,
    onSecondary = DarkBackground,
    secondaryContainer = NeonBlue.copy(alpha = 0.2f),
    onSecondaryContainer = NeonBlue,

    tertiary = SoftViolet,
    onTertiary = DarkBackground,
    tertiaryContainer = SoftViolet.copy(alpha = 0.2f),
    onTertiaryContainer = SoftViolet,

    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleDark,
    onPrimary = TextPrimaryLight,
    primaryContainer = PurpleLight,
    onPrimaryContainer = TextPrimaryLight,

    secondary = NeonBlue,
    onSecondary = LightBackground,
    secondaryContainer = NeonBlue.copy(alpha = 0.15f),
    onSecondaryContainer = TextPrimaryLight,

    tertiary = SoftViolet,
    onTertiary = LightBackground,
    tertiaryContainer = SoftViolet.copy(alpha = 0.15f),
    onTertiaryContainer = TextPrimaryLight,

    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,

    error = ErrorRed,
    onError = LightBackground
)

@Composable
fun RemixLabTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
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
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RemixLabTypography,
        content = content
    )
}
