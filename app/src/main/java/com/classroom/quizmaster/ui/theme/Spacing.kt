package com.classroom.quizmaster.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

data class QuizSpacing(
    val xs: Float = 4f,
    val s: Float = 8f,
    val m: Float = 12f,
    val l: Float = 16f,
    val xl: Float = 24f,
    val xxl: Float = 32f
) {
    val xsDp get() = xs.dp
    val sDp get() = s.dp
    val mDp get() = m.dp
    val lDp get() = l.dp
    val xlDp get() = xl.dp
    val xxlDp get() = xxl.dp
}

internal val LocalSpacing = staticCompositionLocalOf { QuizSpacing() }

data class QuizElevations(
    val none: Float = 0f,
    val s: Float = 1f,
    val m: Float = 3f,
    val l: Float = 6f
) {
    val noneDp get() = none.dp
    val sDp get() = s.dp
    val mDp get() = m.dp
    val lDp get() = l.dp
}

internal val LocalElevations = staticCompositionLocalOf { QuizElevations() }

object QuizTheme {
    val spacing: QuizSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
    val elevations: QuizElevations
        @Composable
        @ReadOnlyComposable
        get() = LocalElevations.current
}
