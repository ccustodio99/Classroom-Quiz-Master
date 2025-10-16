package com.classroom.quizmaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    secondary = SecondaryMagenta,
    onSecondary = Color.White,
    tertiary = TertiaryCyan,
    onTertiary = Color.Black,
    background = MistBackground,
    onBackground = InkDark,
    surface = Color.White,
    onSurface = InkDark,
    surfaceVariant = Color.White.copy(alpha = 0.92f),
    onSurfaceVariant = InkDark.copy(alpha = 0.72f),
    outline = PrimaryIndigo.copy(alpha = 0.4f)
)

private val DarkColors = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    secondary = SecondaryMagenta,
    onSecondary = Color.Black,
    tertiary = TertiaryCyan,
    onTertiary = Color.Black,
    background = InkDark,
    onBackground = Color.White,
    surface = MidnightSurface,
    onSurface = Color.White,
    surfaceVariant = MidnightSurface.copy(alpha = 0.9f),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    outline = TertiaryCyan.copy(alpha = 0.4f)
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
