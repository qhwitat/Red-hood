package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberColorScheme = darkColorScheme(
    primary = CyberNeonRed,
    onPrimary = CyberTextHigh,
    secondary = CyberDarkCrimson,
    onSecondary = CyberTextHigh,
    tertiary = CyberBrightAccent,
    background = CyberBlack,
    onBackground = CyberTextHigh,
    surface = CyberDarkSurface,
    onSurface = CyberTextHigh,
    surfaceVariant = CyberDarkCard,
    onSurfaceVariant = CyberTextDim,
    outline = CyberDarkCrimson
)

@Composable
fun CyberTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
