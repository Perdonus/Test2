package com.perdonus.r34viewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = RoseDust,
    onPrimary = InkBlack,
    secondary = Clay,
    onSecondary = InkBlack,
    tertiary = Sand,
    background = InkBlack,
    onBackground = Sand,
    surface = Panel,
    onSurface = Sand,
    surfaceVariant = PanelAlt,
)

private val LightColors = lightColorScheme(
    primary = Wine,
    secondary = Clay,
    tertiary = RoseDust,
    background = Sand,
    onBackground = InkBlack,
    surface = Color(0xFFFFF7EE),
    onSurface = InkBlack,
)

@Composable
fun R34Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
