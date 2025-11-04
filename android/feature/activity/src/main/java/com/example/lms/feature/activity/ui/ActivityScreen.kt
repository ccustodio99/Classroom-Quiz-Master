package com.example.lms.feature.activity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ActivityRoute(
    modifier: Modifier = Modifier,
    onOpenCertificates: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    ActivityScreen(
        modifier = modifier,
        state = viewModel.uiState,
        onOpenCertificates = onOpenCertificates,
    )
}

@Composable
fun ActivityScreen(
    state: ActivityUiState,
    onOpenCertificates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Streak ${state.streak} days • Mastery gain ${(state.masteryGain * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.progress.forEach { progress ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(progress.title, style = MaterialTheme.typography.titleMedium)
                        Text("${progress.completionPercent}% complete", style = MaterialTheme.typography.bodyMedium)
                        Text("Mastery Δ ${progress.masteryDelta}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Text("Badges", style = MaterialTheme.typography.titleMedium)
        state.badges.forEach { badge ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(badge.name, style = MaterialTheme.typography.titleSmall)
                    Text(badge.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("Certificates", style = MaterialTheme.typography.titleMedium)
        state.certificates.forEach { certificate ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text(certificate.title, style = MaterialTheme.typography.bodyLarge); Text(certificate.issuedOn, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = onOpenCertificates) { Text("Open") }
            }
        }
    }
}
