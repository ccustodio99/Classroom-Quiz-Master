package com.classroom.quizmaster.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classroom.quizmaster.R
import com.classroom.quizmaster.domain.model.UserRole
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthRoute(
    onTeacherAuthenticated: () -> Unit,
    onStudentContinue: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val clientId = stringResource(id = R.string.google_web_client_id)
    val googleClient = remember(clientId, context) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build()
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token != null) {
                viewModel.handleGoogleToken(token)
            } else {
                viewModel.clearError()
            }
        } catch (ex: Exception) {
            viewModel.clearError()
        }
    }

    LaunchedEffect(authState.isAuthenticated, authState.role) {
        if (authState.isAuthenticated && authState.role == UserRole.TEACHER) {
            onTeacherAuthenticated()
        }
    }

    AuthScreen(
        state = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onNicknameChange = viewModel::onNicknameChange,
        onSignIn = viewModel::signInTeacher,
        onSignUp = viewModel::createTeacher,
        onGoogle = { launcher.launch(googleClient.signInIntent) },
        onStudentContinue = { viewModel.continueAsStudent(onStudentContinue) }
    )
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onGoogle: () -> Unit,
    onStudentContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Classroom Quiz Master", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Teacher sign in", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") }
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") }
                )
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = onDisplayNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") }
                )
                Button(onClick = onSignIn, enabled = !state.loading) {
                    Text("Sign in")
                }
                Button(onClick = onSignUp, enabled = !state.loading) {
                    Text("Create account")
                }
                Button(onClick = onGoogle, enabled = !state.loading) {
                    Text("Continue with Google")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Student entry", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nickname") }
                )
                Button(onClick = onStudentContinue, enabled = !state.loading) {
                    Text("Continue as Student")
                }
            }
        }
        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
