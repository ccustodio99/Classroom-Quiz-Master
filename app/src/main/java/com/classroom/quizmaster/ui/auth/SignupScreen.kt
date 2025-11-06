package com.classroom.quizmaster.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.classroom.quizmaster.data.model.UserRole
import com.classroom.quizmaster.ui.viewmodel.SignupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    navController: NavController,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var roleMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearError() }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineSmall)
        Text("Sign up as a teacher or student to get started.", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::updateName,
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.org,
            onValueChange = viewModel::updateOrg,
            label = { Text("Organization (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = roleMenuExpanded,
            onExpandedChange = { roleMenuExpanded = !roleMenuExpanded }
        ) {
            OutlinedTextField(
                value = uiState.selectedRole.displayName(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            DropdownMenu(
                expanded = roleMenuExpanded,
                onDismissRequest = { roleMenuExpanded = false }
            ) {
                viewModel.roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.displayName()) },
                        onClick = {
                            viewModel.updateRole(role)
                            roleMenuExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::updateConfirmPassword,
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = viewModel::submit,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 8.dp)
                )
            }
            Text("Create account")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { navController.popBackStack(AuthDestinations.Login, inclusive = false) }) {
            Text("Already have an account? Sign in")
        }
    }
}

private fun UserRole.displayName(): String = when (this) {
    UserRole.LEARNER -> "Student"
    UserRole.TEACHER -> "Teacher"
    UserRole.ADMIN -> "Admin"
}
