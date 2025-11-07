package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun TimerRing(
    progress: Float,
    modifier: Modifier = Modifier.size(96.dp)
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = scheme.primary.copy(alpha = 0.3f)
    val progressColor = scheme.primary
    Canvas(modifier = modifier) {
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
    }
}
