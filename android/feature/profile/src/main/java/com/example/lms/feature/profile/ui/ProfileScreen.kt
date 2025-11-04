package com.example.lms.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
    onManageDownloads: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    ProfileScreen(
        modifier = modifier,
        state = viewModel.uiState,
        onSignOut = onSignOut,
        onManageDownloads = onManageDownloads,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onSignOut: () -> Unit,
    onManageDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${state.displayName} • ${state.role}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(state.email, style = MaterialTheme.typography.bodyMedium)
        Text("Organization: ${state.org}", style = MaterialTheme.typography.bodyMedium)
        Text("Offline downloads", style = MaterialTheme.typography.titleMedium)
        state.downloads.forEach { download ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(download.title, style = MaterialTheme.typography.bodyLarge); Text("${download.sizeMb} MB", style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = onManageDownloads) { Text("Manage") }
                }
            }
        }
        Text("Preferences", style = MaterialTheme.typography.titleMedium)
        state.preferences.forEach { pref ->
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pref.label, style = MaterialTheme.typography.bodyLarge)
                Text(pref.value, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = onSignOut) { Text("Sign out") }
    }
}
