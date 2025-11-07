package com.classroom.quizmaster.ui.teacher.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportsScreen(
    averages: List<Float>,
    commonlyMissed: List<Pair<String, Float>>
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Class average", style = MaterialTheme.typography.titleLarge)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    if (averages.isNotEmpty()) {
                        val barWidth = size.width / averages.size
                        averages.forEachIndexed { index, score ->
                            val barHeight = size.height * (score / 100f)
                            drawRect(
                                color = primaryColor,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    x = index * barWidth,
                                    y = size.height - barHeight
                                ),
                                size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, barHeight),
                            )
                        }
                    }
                }
            }
        }
        Card {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Commonly missed", style = MaterialTheme.typography.titleLarge)
                commonlyMissed.forEach { (question, percent) ->
                    Text("$question - ${percent.toInt()}% incorrect")
                }
            }
        }
    }
}
