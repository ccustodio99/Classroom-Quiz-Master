package com.classroom.quizmaster.ui.teacher.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TeacherProfileRoute(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: TeacherProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherProfileScreen(
        state = state,
        onNameChanged = viewModel::updateDisplayName,
        onSave = { viewModel.save(onBack) },
        onRefresh = viewModel::refreshData,
        onLogout = { viewModel.logout(onLoggedOut) },
        onBack = onBack
    )
}

@Composable
fun TeacherProfileScreen(
    state: TeacherProfileUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Update your teacher profile or sign out.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = state.displayNameInput,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Display name") },
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text
            )
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            enabled = false,
            readOnly = true
        )
        state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        state.savedMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSaving) "Saving..." else "Save")
        }
        Button(
            onClick = onRefresh,
            enabled = !state.isRefreshing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isRefreshing) "Refreshing..." else "Refresh data")
        }
        Button(
            onClick = onLogout,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign out")
        }
        Button(
            onClick = onBack,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
