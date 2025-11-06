package com.classroom.quizmaster.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.classroom.quizmaster.data.model.UserRole
import com.classroom.quizmaster.ui.viewmodel.ProfileViewModel

@Suppress("UnusedParameter")
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearFeedback() }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.user == null) {
                Text(
                    "No account is currently signed in.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("Full name") },
                enabled = uiState.user != null && !uiState.isSaving && !uiState.isDeleting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.org,
                onValueChange = viewModel::updateOrg,
                label = { Text("Organization") },
                enabled = uiState.user != null && !uiState.isSaving && !uiState.isDeleting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Email: ${uiState.user?.email ?: "Not available"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Role: ${uiState.role.displayName()}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (uiState.message != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.message ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::saveProfile,
                enabled = uiState.user != null && !uiState.isSaving && !uiState.isDeleting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp)
                    )
                }
                Text("Save changes")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = viewModel::signOut,
                enabled = !uiState.isSaving && !uiState.isDeleting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign out")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showDeleteDialog = true },
                enabled = uiState.user != null && !uiState.isDeleting && !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isDeleting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp)
                    )
                }
                Text("Delete account")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) showDeleteDialog = false },
            title = { Text("Delete account") },
            text = { Text("This will permanently remove your account data. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun UserRole.displayName(): String = when (this) {
    UserRole.LEARNER -> "Student"
    UserRole.TEACHER -> "Teacher"
    UserRole.ADMIN -> "Admin"
}
