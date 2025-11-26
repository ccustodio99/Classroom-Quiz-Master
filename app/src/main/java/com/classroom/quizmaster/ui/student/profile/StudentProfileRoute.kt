package com.classroom.quizmaster.ui.student.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.PrimaryButton

@Composable
fun StudentProfileRoute(
    onLoggedOut: () -> Unit,
    viewModel: StudentProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentProfileScreen(
        state = state,
        onNameChanged = viewModel::updateNameInput,
        onSaveName = viewModel::saveName,
        onRefresh = viewModel::refreshData,
        onResetPassword = viewModel::sendPasswordReset,
        onLogout = {
            viewModel.logout()
            onLoggedOut()
        }
    )
}

@Composable
fun StudentProfileScreen(
    state: StudentProfileUiState,
    onNameChanged: (String) -> Unit,
    onSaveName: () -> Unit,
    onRefresh: () -> Unit,
    onResetPassword: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineSmall)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Identity", style = MaterialTheme.typography.titleMedium)
                androidx.compose.material3.OutlinedTextField(
                    value = state.nameInput,
                    onValueChange = onNameChanged,
                    label = { Text("Full name") },
                    singleLine = true,
                    isError = state.errorMessage != null
                )
                Text(text = state.email.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                if (state.errorMessage != null) {
                    Text(text = state.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
                if (state.statusMessage != null) {
                    Text(text = state.statusMessage ?: "", color = MaterialTheme.colorScheme.primary)
                }
                PrimaryButton(
                    text = if (state.saving) "Saving..." else "Save name",
                    onClick = onSaveName,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = if (state.refreshing) "Refreshing..." else "Refresh data",
                    onClick = onRefresh,
                    enabled = !state.refreshing,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = "Reset password",
                    onClick = onResetPassword,
                    enabled = !state.email.isNullOrBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Progress", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                Text("Classrooms: ${state.classroomsCount}")
                Text("Assignments done: ${state.completedAssignments}")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = "Sign out",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
