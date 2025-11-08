package com.classroom.quizmaster.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.model.DistributionBar
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun DistributionBarChart(
    data: List<DistributionBar>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val correct = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Canvas(
            modifier = Modifier
                .height(200.dp)
                .padding(16.dp)
        ) {
            if (data.isEmpty()) return@Canvas
            val barWidth = size.width / (data.size * 1.5f)
            data.forEachIndexed { index, bar ->
                val barHeight = size.height * bar.value.coerceIn(0f, 1f)
                val x = index * barWidth * 1.5f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (bar.isCorrect) {
                            listOf(correct, correct.copy(alpha = 0.6f))
                        } else {
                            listOf(primary, primary.copy(alpha = 0.4f))
                        }
                    ),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawContext.canvas.nativeCanvas.apply {
                    val labelPaint = Paint().apply {
                        textAlign = Paint.Align.CENTER
                        color = onSurfaceVariant.toArgb()
                        textSize = 28f
                        isAntiAlias = true
                    }
                    drawText(
                        bar.label,
                        x + barWidth / 2f,
                        size.height + 32f,
                        labelPaint
                    )
                }
            }
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.4f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }
    }
}

@QuizPreviews
@Composable
private fun DistributionBarChartPreview() {
    QuizMasterTheme {
        DistributionBarChart(
            data = listOf(
                DistributionBar("A", 0.65f, true),
                DistributionBar("B", 0.2f),
                DistributionBar("C", 0.1f),
                DistributionBar("D", 0.05f)
            )
        )
    }
}
