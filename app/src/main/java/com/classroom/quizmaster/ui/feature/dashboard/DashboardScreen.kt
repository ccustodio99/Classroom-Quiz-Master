package com.classroom.quizmaster.ui.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.TopBarAction
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCreateModule: () -> Unit,
    onOpenModule: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val modules = state.modules
    val objectiveCount = remember(modules) { modules.flatMap { it.objectives }.toSet().size }
    val trendingTopics = remember(modules) { modules.take(3).joinToString(" • ") { it.topic } }

    GenZScaffold(
        title = "QuizMaster Control",
        subtitle = "Gen Z-ready classroom flow",
        actions = listOf(
            TopBarAction(
                icon = Icons.Rounded.AutoAwesome,
                contentDescription = "Drop sample modules",
                onClick = viewModel::createQuickModule
            )
        )
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                DashboardHeader(
                    moduleCount = modules.size,
                    objectiveCount = objectiveCount,
                    trendingTopics = trendingTopics.takeIf { it.isNotBlank() },
                    onCreateModule = onCreateModule,
                    onQuickModule = viewModel::createQuickModule
                )
            }
            item {
                HighlightRow(
                    moduleCount = modules.size,
                    objectiveCount = objectiveCount
                )
            }
            if (modules.isEmpty()) {
                item {
                    EmptyState(onCreateModule = onCreateModule)
                }
            } else {
                item {
                    Text(
                        text = "Modules Ready to Roll",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(modules, key = { it.id }) { module ->
                    ModuleCard(
                        module = module,
                        onOpen = { onOpenModule(module.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    moduleCount: Int,
    objectiveCount: Int,
    trendingTopics: String?,
    onCreateModule: () -> Unit,
    onQuickModule: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary
    ) {
        Brush.linearGradient(
            listOf(
                colorScheme.primary,
                colorScheme.secondary,
                colorScheme.tertiary
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HeaderPills(moduleCount = moduleCount, objectiveCount = objectiveCount)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Kamusta, Guro!",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Craft immersive quiz flows for your class",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Modern visuals, quick pacing, mastery-focused diagnostics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            if (!trendingTopics.isNullOrBlank()) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Hot topics: $trendingTopics",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onCreateModule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Design new flow")
                }
                OutlinedButton(
                    onClick = onQuickModule,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Drop sample")
                }
            }
        }
    }
}

@Composable
private fun HeaderPills(moduleCount: Int, objectiveCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HeaderPill(text = "${moduleCount.coerceAtLeast(0)} modules synced")
        HeaderPill(text = "${objectiveCount.coerceAtLeast(0)} objectives mapped")
    }
}

@Composable
private fun HeaderPill(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun HighlightRow(
    moduleCount: Int,
    objectiveCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HighlightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.RocketLaunch,
            title = "Active modules",
            value = if (moduleCount > 0) moduleCount.toString() else "Start",
            caption = if (moduleCount > 0) "Ready to launch sessions" else "Drop your first pack"
        )
        HighlightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Explore,
            title = "Objectives mapped",
            value = if (objectiveCount > 0) objectiveCount.toString() else "—",
            caption = if (objectiveCount > 0) "Learning goals covered" else "Add mastery targets"
        )
    }
}

@Composable
private fun HighlightCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    caption: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 0.dp
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleCard(
    module: ModuleSummary,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = module.topic,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Learning goals",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (module.objectives.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    module.objectives.forEach { objective ->
                        ObjectivePill(objective = objective)
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ModuleMetaRow(module = module)
            FilledTonalButton(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buksan ang Flow")
            }
        }
    }
}

@Composable
private fun ObjectivePill(objective: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        tonalElevation = 0.dp
    ) {
        Text(
            text = objective,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModuleMetaRow(module: ModuleSummary) {
    val status = when {
        (module.attempts ?: 0) > 0 && module.postAverage != null && module.preAverage != null && module.postAverage > module.preAverage -> "📈 Gains"
        (module.attempts ?: 0) > 0 -> "Live in class"
        else -> "Ready to launch"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetaItem(
            modifier = Modifier.weight(1f),
            label = "Attempts",
            value = module.attempts?.takeIf { it > 0 }?.toString() ?: "Fresh drop"
        )
        MetaItem(
            modifier = Modifier.weight(1f),
            label = "Pre → Post",
            value = "${formatPercent(module.preAverage)} → ${formatPercent(module.postAverage)}"
        )
        MetaItem(
            modifier = Modifier.weight(1f),
            label = "Status",
            value = status
        )
    }
}

@Composable
private fun MetaItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyState(onCreateModule: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 0.dp
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Launch your first Gen Z flow",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Create a module with pre/post diagnostics, interactive slides, and exports—everything in one smooth flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onCreateModule,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start designing")
            }
        }
    }
}

private fun formatPercent(value: Double?): String =
    value?.let { "${it.roundToInt()}%" } ?: "—"
