package com.classroom.quizmaster.ui.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.ui.components.AdaptiveWrapRow
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.InfoPill
import com.classroom.quizmaster.ui.components.SectionCard
import com.classroom.quizmaster.ui.components.TopBarAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    viewModel: ModuleDetailViewModel,
    onStartDelivery: () -> Unit,
    onViewReports: () -> Unit,
    onOpenLiveSession: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val module = state.module
    val subtitle = remember(module) {
        module?.objectives?.take(3)?.joinToString(" • ") ?: "Module overview"
    }

    GenZScaffold(
        title = module?.topic ?: "Module",
        subtitle = subtitle,
        onBack = onBack,
        actions = listOf(
            TopBarAction(
                icon = Icons.Rounded.CloudSync,
                contentDescription = "Sync to cloud",
                onClick = viewModel::syncToCloud
            )
        )
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            module?.let {
                ModuleSnapshot(module = it)
                LiveActions(
                    onHostLiveSession = {
                        viewModel.createLiveSession()?.let(onOpenLiveSession)
                    },
                    onStartDelivery = onStartDelivery,
                    onAssignHomework = viewModel::assignHomework,
                    onViewReports = onViewReports
                )
            } ?: LoadingState()
        }
    }
}

@Composable
private fun ModuleSnapshot(module: Module) {
    val preCount = remember(module) { module.preTest.items.size }
    val postCount = remember(module) { module.postTest.items.size }
    val slideCount = remember(module) { module.lesson.slides.size }
    SectionCard(
        title = "Module snapshot",
        subtitle = module.subject,
        caption = "Pre-test → Talakayan → Post-test bundled for quick launch.",
        trailingContent = {
            InfoPill(text = "${preCount + postCount} assessment items")
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AdaptiveWrapRow(
                horizontalSpacing = 8.dp,
                verticalSpacing = 8.dp
            ) {
                InfoPill(text = "$preCount pre-test")
                InfoPill(text = "$postCount post-test", backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.secondary)
                InfoPill(text = "$slideCount slides", backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.tertiary)
                InfoPill(text = "${module.settings.timePerItemSeconds}s cadence")
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Learning objectives", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                AdaptiveWrapRow(
                    horizontalSpacing = 8.dp,
                    verticalSpacing = 8.dp
                ) {
                    module.objectives.forEach { objective ->
                        InfoPill(text = objective)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveActions(
    onHostLiveSession: () -> Unit,
    onStartDelivery: () -> Unit,
    onAssignHomework: () -> Unit,
    onViewReports: () -> Unit
) {
    SectionCard(
        title = "Launch & share",
        subtitle = "Pick the flow that matches your delivery mode",
        caption = "Open the live lobby for synchronous sessions or push as homework."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalButton(
                onClick = onHostLiveSession,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Rounded.Group, contentDescription = null)
                Text("Host live session", modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = onStartDelivery,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                Text("Simulan: Pre → Aralin → Post", modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(
                onClick = onAssignHomework,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Rounded.School, contentDescription = null)
                Text("Assign as homework", modifier = Modifier.padding(start = 8.dp))
            }
            FilledTonalButton(
                onClick = onViewReports,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View reports")
            }
        }
    }
}

@Composable
private fun LoadingState() {
    SectionCard(
        title = "Loading module",
        subtitle = "Fetching the latest details",
        caption = "Please wait while we retrieve objectives and assessments."
    ) {
        Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
