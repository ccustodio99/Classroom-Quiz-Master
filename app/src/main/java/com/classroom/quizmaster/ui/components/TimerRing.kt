package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import com.classroom.quizmaster.ui.preview.QuizPreviews

@Composable
fun TimerRing(
    progress: Float,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 12.dp,
    ringDiameter: Dp = 112.dp
) {
    val scheme = MaterialTheme.colorScheme
    val normalized = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .size(ringDiameter)
            .semantics {
                contentDescription = "Timer $remainingSeconds seconds remaining"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = ringWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = scheme.surfaceVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = size
            )
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(scheme.primary, scheme.tertiary)
                ),
                startAngle = -90f,
                sweepAngle = 360f * normalized,
                useCenter = false,
                style = stroke,
                size = size
            )
        }
        Text(
            text = remainingSeconds.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.onSurface
        )
    }
}

@QuizPreviews
@Composable
private fun TimerRingPreview() {
    QuizMasterTheme {
        TimerRing(progress = 0.65f, remainingSeconds = 18)
    }
}
