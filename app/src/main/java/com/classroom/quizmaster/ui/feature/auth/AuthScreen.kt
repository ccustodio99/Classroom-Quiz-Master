package com.classroom.quizmaster.ui.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.UserAccount
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.ui.SessionViewModel

private enum class AuthMode { Login, Register }

@Composable
fun AuthScreen(
    sessionViewModel: SessionViewModel,
    onAuthenticated: (UserAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionState by sessionViewModel.uiState.collectAsState()
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.Teacher) }

    LaunchedEffect(sessionState.currentUser) {
        sessionState.currentUser?.let(onAuthenticated)
    }

    LaunchedEffect(sessionState.registrationSuccess) {
        if (sessionState.registrationSuccess) {
            password = ""
            sessionViewModel.clearRegistrationFlag()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (mode == AuthMode.Login) "Welcome back" else "Create an account",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
                if (mode == AuthMode.Register) {
                    RoleSelector(role = role, onRoleSelected = { role = it })
                }
                val errorMessage = sessionState.error
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (sessionState.registrationSuccess && mode == AuthMode.Register) {
                    Text(
                        text = "Registration submitted. Please wait for admin approval.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = {
                        if (mode == AuthMode.Login) {
                            sessionViewModel.login(email, password)
                        } else {
                            sessionViewModel.register(displayName, email, password, role)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sessionState.isLoading
                ) {
                    Text(if (mode == AuthMode.Login) "Log in" else "Sign up")
                }
                TextButton(onClick = {
                    mode = if (mode == AuthMode.Login) AuthMode.Register else AuthMode.Login
                    sessionViewModel.clearRegistrationFlag()
                }) {
                    Text(
                        if (mode == AuthMode.Login) "Need an account? Sign up"
                        else "Already registered? Log in"
                    )
                }
            }
        }
        if (sessionState.isLoading) {
            Spacer(modifier = Modifier.size(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun RoleSelector(
    role: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Register as")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = role == UserRole.Teacher,
                onClick = { onRoleSelected(UserRole.Teacher) },
                label = { Text("Teacher") }
            )
            FilterChip(
                selected = role == UserRole.Student,
                onClick = { onRoleSelected(UserRole.Student) },
                label = { Text("Student") }
            )
        }
    }
}
