package com.classroom.quizmaster.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

data class QuizThemeConfig(
    val palette: QuizPalette = QuizPalette.Calm,
    val dynamicColor: Boolean = false,
    val highContrast: Boolean = false
)

@Composable
fun QuizMasterTheme(
    config: QuizThemeConfig = QuizThemeConfig(),
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = remember(config, useDarkTheme, context) {
        when {
            config.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (useDarkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }
            else -> paletteFor(config.palette).scheme(useDarkTheme)
        }
    }.let { scheme ->
        if (config.highContrast || config.palette == QuizPalette.HighContrast) {
            scheme.withHighContrast()
        } else {
            scheme
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides QuizSpacing(),
        LocalElevations provides QuizElevations(),
        LocalShapeTokens provides QuizShapeTokens()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuizTypography,
            shapes = QuizShapes,
            content = content
        )
    }
}

private class ThemePreviewProvider : PreviewParameterProvider<QuizThemeConfig> {
    override val values: Sequence<QuizThemeConfig> = sequenceOf(
        QuizThemeConfig(QuizPalette.Calm),
        QuizThemeConfig(QuizPalette.Vibrant),
        QuizThemeConfig(QuizPalette.HighContrast, highContrast = true)
    )
}

@Preview(name = "Theme variations", widthDp = 360, heightDp = 640)
@Composable
private fun ThemePreview(@PreviewParameter(ThemePreviewProvider::class) config: QuizThemeConfig) {
    QuizMasterTheme(config = config) {
        ThemeSwatch()
    }
}

@Composable
private fun ThemeSwatch() {
    Surface(
        modifier = Modifier,
        tonalElevation = QuizTheme.elevations.mDp
    ) {
        Text(
            text = "QuizMaster Theme",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
