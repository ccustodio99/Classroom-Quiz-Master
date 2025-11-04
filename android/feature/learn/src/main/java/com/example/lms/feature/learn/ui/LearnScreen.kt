package com.example.lms.feature.learn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LearnRoute(
    modifier: Modifier = Modifier,
    onSelectClass: () -> Unit,
    onStartSearch: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    LearnScreen(
        modifier = modifier,
        state = viewModel.uiState,
        onSelectClass = onSelectClass,
        onStartSearch = onStartSearch,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearnScreen(
    state: LearnUiState,
    onSelectClass: () -> Unit,
    onStartSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Catalog", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.filters.forEach { filter ->
                OutlinedButton(onClick = onStartSearch) { Text(filter) }
            }
        }
        Text("You're enrolled", style = MaterialTheme.typography.titleMedium)
        state.enrolled.forEach { path ->
            LearningCard(path = path, actionLabel = "Open", onClick = onSelectClass)
        }
        Text("Recommended for you", style = MaterialTheme.typography.titleMedium)
        state.recommendations.forEach { path ->
            LearningCard(path = path, actionLabel = "Preview", onClick = onStartSearch)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearningCard(path: LearningPath, actionLabel: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(path.title, style = MaterialTheme.typography.titleMedium)
            Text(path.description, style = MaterialTheme.typography.bodyMedium)
            Text("${path.durationMinutes} min • ${path.level}", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                path.tags.forEach { tag ->
                    Text(tag, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClick) { Text(actionLabel) }
        }
    }
}
