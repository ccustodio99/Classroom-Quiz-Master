package com.classroom.quizmaster.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class QuizPalette {
    Calm, Vibrant, HighContrast
}

internal data class QuizColorFamily(
    val light: ColorScheme,
    val dark: ColorScheme
) {
    fun scheme(useDarkTheme: Boolean) = if (useDarkTheme) dark else light
}

internal val CalmColors = QuizColorFamily(
    light = lightColorScheme(
        primary = Color(0xFF3862F8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDE2FF),
        onPrimaryContainer = Color(0xFF001455),
        secondary = Color(0xFF5161C5),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0E4FF),
        onSecondaryContainer = Color(0xFF0D1B5C),
        tertiary = Color(0xFF1B998B),
        onTertiary = Color.White,
        background = Color(0xFFF7F9FF),
        onBackground = Color(0xFF111322),
        surface = Color.White,
        onSurface = Color(0xFF15182B),
        surfaceVariant = Color(0xFFE1E6FB),
        onSurfaceVariant = Color(0xFF434A6B),
        outline = Color(0xFF6C7395),
        error = Color(0xFFB3261E),
        onError = Color.White
    ),
    dark = darkColorScheme(
        primary = Color(0xFF9BB3FF),
        onPrimary = Color(0xFF02133D),
        primaryContainer = Color(0xFF1F2C63),
        onPrimaryContainer = Color(0xFFDDE2FF),
        secondary = Color(0xFFC3CBFF),
        onSecondary = Color(0xFF09174F),
        secondaryContainer = Color(0xFF323E78),
        onSecondaryContainer = Color(0xFFE0E4FF),
        tertiary = Color(0xFF79DDCF),
        onTertiary = Color(0xFF00201A),
        background = Color(0xFF0C0F1D),
        onBackground = Color(0xFFE2E6FF),
        surface = Color(0xFF111425),
        onSurface = Color(0xFFE0E4FF),
        surfaceVariant = Color(0xFF303454),
        onSurfaceVariant = Color(0xFFC3CAEF),
        outline = Color(0xFF8B90B5),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410)
    )
)

internal val VibrantColors = QuizColorFamily(
    light = lightColorScheme(
        primary = Color(0xFFFB6A00),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD5B3),
        onPrimaryContainer = Color(0xFF2E0C00),
        secondary = Color(0xFF6B38FB),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE2D6FF),
        onSecondaryContainer = Color(0xFF1C0055),
        tertiary = Color(0xFF008F9C),
        onTertiary = Color.White,
        background = Color(0xFFFFFBF7),
        onBackground = Color(0xFF1F1B17),
        surface = Color.White,
        onSurface = Color(0xFF221B14),
        surfaceVariant = Color(0xFFF3E1D7),
        onSurfaceVariant = Color(0xFF4F453D),
        outline = Color(0xFF81746B),
        error = Color(0xFFBA1A1A),
        onError = Color.White
    ),
    dark = darkColorScheme(
        primary = Color(0xFFFFB784),
        onPrimary = Color(0xFF4D1900),
        primaryContainer = Color(0xFF702500),
        onPrimaryContainer = Color(0xFFFFDCC7),
        secondary = Color(0xFFCDB8FF),
        onSecondary = Color(0xFF2F0071),
        secondaryContainer = Color(0xFF4D00AA),
        onSecondaryContainer = Color(0xFFEBDDFF),
        tertiary = Color(0xFF74DEE9),
        onTertiary = Color(0xFF00363C),
        background = Color(0xFF1F140E),
        onBackground = Color(0xFFF0DFD5),
        surface = Color(0xFF271912),
        onSurface = Color(0xFFF4E1D5),
        surfaceVariant = Color(0xFF51433A),
        onSurfaceVariant = Color(0xFFD6C2B8),
        outline = Color(0xFF9F8F86),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410)
    )
)

internal val HighContrastColors = QuizColorFamily(
    light = lightColorScheme(
        primary = Color(0xFF0057FF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF002D7A),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFF004B50),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF00292C),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFF6300A6),
        onTertiary = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFE0E0E0),
        onSurfaceVariant = Color.Black,
        outline = Color.Black,
        error = Color(0xFFB3261E),
        onError = Color.White
    ),
    dark = darkColorScheme(
        primary = Color(0xFF91B6FF),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF0033A0),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFF80F8FF),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF00585E),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFFEFB7FF),
        onTertiary = Color.Black,
        background = Color(0xFF020202),
        onBackground = Color(0xFFF5F5F5),
        surface = Color(0xFF030303),
        onSurface = Color(0xFFF5F5F5),
        surfaceVariant = Color(0xFF1C1C1C),
        onSurfaceVariant = Color(0xFFDADADA),
        outline = Color(0xFFEEEEEE),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF2B0907)
    )
)

internal fun paletteFor(palette: QuizPalette): QuizColorFamily = when (palette) {
    QuizPalette.Calm -> CalmColors
    QuizPalette.Vibrant -> VibrantColors
    QuizPalette.HighContrast -> HighContrastColors
}

internal fun ColorScheme.withHighContrast(): ColorScheme {
    if (this === HighContrastColors.light || this === HighContrastColors.dark) return this
    val boost = if (Build.VERSION.SDK_INT >= 31) 0.05f else 0f
    return copy(
        primary = primary.copy(alpha = 1f - boost),
        onPrimary = if (useWhiteForeground(onPrimary)) Color.White else Color.Black,
        secondary = secondary.copy(alpha = 1f - boost),
        onSecondary = if (useWhiteForeground(onSecondary)) Color.White else Color.Black,
        background = background,
        onBackground = Color(0xFF050505),
        surface = surface,
        onSurface = Color(0xFF050505),
        outline = outline.copy(alpha = 1f)
    )
}

private fun useWhiteForeground(color: Color): Boolean {
    val luminance = (0.299 * color.red) + (0.587 * color.green) + (0.114 * color.blue)
    return luminance < 0.5
}
