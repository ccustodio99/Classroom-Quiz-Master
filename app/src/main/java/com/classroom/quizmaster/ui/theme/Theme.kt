package com.classroom.quizmaster.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColors = lightColorScheme(
    primary = PurplePrimary,
    secondary = OrangeAccent,
    primaryContainer = PurplePrimary.copy(alpha = 0.2f),
    background = Color(0xFFFDFDFC),
    surface = Color(0xFFFDFDFC)
)

private val darkColors = darkColorScheme(
    primary = OrangeAccent,
    secondary = PurplePrimary,
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E)
)

@Composable
fun QuizMasterTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = if (useDarkTheme) darkColors else lightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuizTypography,
        content = content
    )
}
