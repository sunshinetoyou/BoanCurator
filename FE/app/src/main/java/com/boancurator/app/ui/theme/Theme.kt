package com.boancurator.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = DarkBackground,
    primaryContainer = CyanDark,
    secondary = NeonBlue,
    onSecondary = DarkBackground,
    secondaryContainer = NeonBlueDark,
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
