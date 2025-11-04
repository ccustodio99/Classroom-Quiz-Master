package com.classroom.quizmaster.ui.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.SyncStatus

@Composable
fun ProfileTabScreen(
    viewModel: ProfileTabViewModel,
    snackbarHostState: SnackbarHostState,
    accountName: String?,
    email: String?,
    role: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = accountName ?: "Guest",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    email?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    role?.let {
                        Text("Role: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Text(
                text = "Sync & Privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Offline-first mode keeps everything cached. Push or pull updates when online.")
                    Button(onClick = viewModel::pushAllModules) {
                        Text("Push modules to Firebase")
                    }
                    OutlinedButton(onClick = viewModel::pullUpdates) {
                        Text("Pull updates from Firebase")
                    }
                    SyncStatusLabel(status = state.syncStatus)
                }
            }
        }
        item {
            OutlinedButton(onClick = onLogout) {
                Text("Log out")
            }
        }
    }
}

@Composable
private fun SyncStatusLabel(status: SyncStatus) {
    val text = when (status) {
        SyncStatus.Idle -> "Idle"
        SyncStatus.InProgress -> "Sync in progress…"
        SyncStatus.Success -> "Synced successfully"
        SyncStatus.Error -> "Sync encountered issues"
    }
    Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
}

