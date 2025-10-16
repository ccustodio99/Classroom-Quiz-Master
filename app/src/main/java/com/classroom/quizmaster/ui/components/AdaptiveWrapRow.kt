package com.classroom.quizmaster.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom wrap row that keeps our chip-style content responsive without
 * depending on the experimental layout APIs. The implementation follows the
 * "clarity" and "consistency" UX principles by ensuring predictable spacing
 * across breakpoints.
 */
@Composable
fun AdaptiveWrapRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val horizontalSpacingPx = horizontalSpacing.roundToPx()
        val verticalSpacingPx = verticalSpacing.roundToPx()
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE

        data class RowInfo(
            val items: List<Placeable>,
            val width: Int,
            val height: Int
        )

        val rows = mutableListOf<RowInfo>()
        var currentRowItems = mutableListOf<Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            val spacing = if (currentRowItems.isEmpty()) 0 else horizontalSpacingPx
            val projectedWidth = currentRowWidth + spacing + placeable.width

            if (projectedWidth > maxWidth && currentRowItems.isNotEmpty()) {
                rows += RowInfo(
                    items = currentRowItems,
                    width = currentRowWidth,
                    height = currentRowHeight
                )
                currentRowItems = mutableListOf(placeable)
                currentRowWidth = placeable.width
                currentRowHeight = placeable.height
            } else {
                currentRowItems += placeable
                currentRowWidth = projectedWidth
                currentRowHeight = maxOf(currentRowHeight, placeable.height)
            }
        }

        if (currentRowItems.isNotEmpty()) {
            rows += RowInfo(
                items = currentRowItems,
                width = currentRowWidth,
                height = currentRowHeight
            )
        }

        val calculatedWidth = rows.maxOfOrNull { it.width } ?: constraints.minWidth
        val calculatedHeight = rows.sumOf { it.height }
            .let { totalItemHeights ->
                val gaps = (rows.size - 1).coerceAtLeast(0) * verticalSpacingPx
                totalItemHeights + gaps
            }

        val layoutWidth = when {
            constraints.hasBoundedWidth -> constraints.maxWidth
            else -> calculatedWidth
        }.coerceIn(constraints.minWidth, constraints.maxWidth)

        val layoutHeight = calculatedHeight
            .coerceAtLeast(constraints.minHeight)
            .coerceAtMost(constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            var yOffset = 0
            rows.forEachIndexed { rowIndex, row ->
                var xOffset = 0
                row.items.forEachIndexed { index, placeable ->
                    placeable.placeRelative(x = xOffset, y = yOffset)
                    xOffset += placeable.width
                    if (index != row.items.lastIndex) {
                        xOffset += horizontalSpacingPx
                    }
                }
                yOffset += row.height
                if (rowIndex != rows.lastIndex) {
                    yOffset += verticalSpacingPx
                }
            }
        }
    }
}

