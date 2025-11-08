package com.classroom.quizmaster.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val QuizShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

data class QuizShapeTokens(
    val card: RoundedCornerShape = RoundedCornerShape(24.dp),
    val button: RoundedCornerShape = RoundedCornerShape(20.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    val chip: RoundedCornerShape = RoundedCornerShape(50)
)

internal val LocalShapeTokens = staticCompositionLocalOf { QuizShapeTokens() }

object QuizThemeTokens {
    val shapes: QuizShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalShapeTokens.current
}
