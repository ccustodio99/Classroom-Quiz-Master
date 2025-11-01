package com.classroom.quizmaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryCrimson,
    onPrimary = Color.White,
    secondary = SecondaryMaroon,
    onSecondary = Color.White,
    tertiary = AccentAmber,
    onTertiary = Color.White,
    background = BlushBackground,
    onBackground = Onyx,
    surface = BlushSurface,
    onSurface = Onyx,
    surfaceVariant = SurfaceVariantClay,
    onSurfaceVariant = OnSurfaceVariantClay,
    outline = OutlineClay
)

private val DarkColors = darkColorScheme(
    primary = PrimaryCrimsonDark,
    onPrimary = OnPrimaryCrimsonDark,
    secondary = SecondaryMaroonDark,
    onSecondary = OnSecondaryMaroonDark,
    tertiary = AccentAmberDark,
    onTertiary = OnAccentAmberDark,
    background = NightBackground,
    onBackground = NightOnyx,
    surface = NightSurface,
    onSurface = NightOnyx,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline
)

@Composable
fun QuizMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
