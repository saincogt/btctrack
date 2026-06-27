package com.zeal.btctrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DesignCoral,
    onPrimary = Color.White,
    primaryContainer = DesignCoralDisabled,
    onPrimaryContainer = DesignCoralActive,
    secondary = DesignAccentTeal,
    onSecondary = Color.White,
    background = DesignCanvas,
    onBackground = DesignInk,
    surface = DesignCanvas,
    onSurface = DesignInk,
    surfaceVariant = DesignSurfaceCard,
    onSurfaceVariant = DesignMuted,
    outline = DesignHairline,
    outlineVariant = DesignHairlineSoft,
    error = DesignError,
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = DesignError,
)

private val DarkColors = darkColorScheme(
    primary = DesignCoral,
    onPrimary = Color.White,
    primaryContainer = DesignCoralActive,
    onPrimaryContainer = DesignCoralDisabled,
    secondary = DesignAccentTeal,
    onSecondary = DesignSurfaceDark,
    background = DesignSurfaceDark,
    onBackground = DesignOnDark,
    surface = DesignSurfaceDarkSoft,
    onSurface = DesignOnDark,
    surfaceVariant = DesignSurfaceDarkElevated,
    onSurfaceVariant = DesignOnDarkSoft,
    outline = DesignHairlineDark,
    outlineVariant = DesignHairlineDarkSoft,
    error = DesignError,
    onError = Color.White,
    errorContainer = Color(0xFF4A1010),
    onErrorContainer = Color(0xFFFFB4AB),
)

@Composable
fun BtcTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
