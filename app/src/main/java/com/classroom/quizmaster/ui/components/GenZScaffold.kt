package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack

/**
 * Shared screen chrome that keeps the minimalist crimson identity consistent across
 * the app. It wraps a [Scaffold] and renders a solid-toned top bar with gentle
 * curvature to ground each screen.
 */
@Composable
fun GenZScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: List<TopBarAction> = emptyList(),
    floatingActionButton: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        floatingActionButton = {
            floatingActionButton?.invoke()
        },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onBack != null) {
                            Surface(
                                onClick = onBack,
                                shape = CircleShape,
                                color = colorScheme.onPrimary.copy(alpha = 0.12f)
                            ) {
                                Icon(
                                    imageVector = TopBarIcons.Back,
                                    contentDescription = "Go back",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium.copy(color = colorScheme.onPrimary.copy(alpha = 0.72f)),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        actions.forEach { action ->
                            Surface(
                                onClick = action.onClick,
                                shape = CircleShape,
                                color = colorScheme.onPrimary.copy(alpha = 0.12f)
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.contentDescription,
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        content(padding)
    }
}

data class TopBarAction(
    val icon: ImageVector,
    val contentDescription: String?,
    val onClick: () -> Unit
)

/**
 * Decorative icon set used by [GenZScaffold] so screens can obtain a matching back button
 * even when they only provide custom top actions.
 */
object TopBarIcons {
    val Back: ImageVector
        @Composable get() = Icons.AutoMirrored.Rounded.ArrowBack
}

/**
 * Reusable card used across screens to group information, respecting hierarchy and
 * giving generous breathing room.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    caption: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                trailingContent?.invoke()
            }
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/**
 * Minimal pill for inline metadata.
 */
@Composable
fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
