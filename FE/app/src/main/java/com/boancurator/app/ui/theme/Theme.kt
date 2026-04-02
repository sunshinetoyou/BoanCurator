package com.boancurator.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DarkBackground,
    primaryContainer = NeonGreenDim,
    secondary = Emerald,
    onSecondary = DarkBackground,
    secondaryContainer = Emerald,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = Error,
    onError = DarkBackground
)

@Composable
fun BoanCuratorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = BoanTypography,
        content = content
    )
}
