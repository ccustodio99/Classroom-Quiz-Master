package com.classroom.quizmaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Teal90,
    onPrimary = Neutral10,
    primaryContainer = Teal40,
    onPrimaryContainer = Neutral99,
    secondary = Plum90,
    onSecondary = Neutral10,
    background = Neutral10,
    onBackground = Neutral99
)

private val LightColors = lightColorScheme(
    primary = Plum40,
    onPrimary = Neutral99,
    primaryContainer = Plum90,
    onPrimaryContainer = Neutral10,
    secondary = Teal40,
    onSecondary = Neutral99,
    background = Neutral99,
    onBackground = Neutral10
)

@Composable
fun ClassroomQuizMasterTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
