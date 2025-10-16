package com.classroom.quizmaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryGold,
    background = BackgroundGray,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryGold
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
