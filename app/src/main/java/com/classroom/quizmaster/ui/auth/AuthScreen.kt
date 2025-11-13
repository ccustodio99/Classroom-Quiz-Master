package com.classroom.quizmaster.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.R
import com.classroom.quizmaster.ui.components.GhostButton
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.QuizSnackbar
import com.classroom.quizmaster.ui.components.SegmentOption
import com.classroom.quizmaster.ui.components.SegmentedControl
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AuthRoute(
    onTeacherAuthenticated: () -> Unit,
    onStudentEntry: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                AuthEffect.TeacherAuthenticated -> onTeacherAuthenticated()
                AuthEffect.DemoMode -> snackbarHost.showSnackbar("Offline demo enabled")
                is AuthEffect.Error -> snackbarHost.showSnackbar(effect.message)
            }
        }
    }

    AuthScreen(
        state = state,
        snackbarHostState = snackbarHost,
        onLoginEmail = viewModel::updateLoginEmail,
        onLoginPassword = viewModel::updateLoginPassword,
        onSignupEmail = viewModel::updateSignupEmail,
        onSignupPassword = viewModel::updateSignupPassword,
        onSignupConfirm = viewModel::updateSignupConfirm,
        onTermsToggle = viewModel::toggleTerms,
        onProfileNameChange = viewModel::updateProfileName,
        onProfileRoleSelected = viewModel::updateProfileRole,
        onProfileSchoolChange = viewModel::updateProfileSchool,
        onProfileSubjectChange = viewModel::updateProfileSubject,
        onProfileNicknameChange = viewModel::updateProfileNickname,
        onSignupBack = viewModel::backToSignupCredentials,
        onLogin = viewModel::signInTeacher,
        onSignup = viewModel::signUpTeacher,
        onDemo = viewModel::continueOfflineDemo,
        onGoogle = {
            snackbarScope.launch {
                snackbarHost.showSnackbar(context.getString(R.string.google_placeholder))
            }
        },
        onStudentEntry = onStudentEntry
    )
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    snackbarHostState: SnackbarHostState,
    onLoginEmail: (String) -> Unit,
    onLoginPassword: (String) -> Unit,
    onSignupEmail: (String) -> Unit,
    onSignupPassword: (String) -> Unit,
    onSignupConfirm: (String) -> Unit,
    onTermsToggle: (Boolean) -> Unit,
    onProfileNameChange: (String) -> Unit,
    onProfileRoleSelected: (SignupRole) -> Unit,
    onProfileSchoolChange: (String) -> Unit,
    onProfileSubjectChange: (String) -> Unit,
    onProfileNicknameChange: (String) -> Unit,
    onSignupBack: () -> Unit,
    onLogin: () -> Unit,
    onSignup: () -> Unit,
    onDemo: () -> Unit,
    onGoogle: () -> Unit,
    onStudentEntry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = "Manage quizzes, launch live sessions, and sync reports across your classrooms.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SnackbarHost(hostState = snackbarHostState) { data ->
            QuizSnackbar(message = data.visuals.message)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.auth_teacher_login),
                style = MaterialTheme.typography.titleLarge
            )
            AuthTextField(
                value = state.login.email,
                label = stringResource(R.string.auth_email),
                onValueChange = onLoginEmail
            )
            AuthTextField(
                value = state.login.password,
                label = stringResource(R.string.auth_password),
                onValueChange = onLoginPassword,
                isPassword = true
            )
            PrimaryButton(
                text = stringResource(R.string.auth_sign_in),
                onClick = onLogin,
                enabled = !state.loading
            )
            SecondaryButton(
                text = stringResource(R.string.auth_google),
                onClick = onGoogle,
                leadingIcon = { androidx.compose.material3.Icon(Icons.Default.GTranslate, contentDescription = null) }
            )
            GhostButton(
                text = stringResource(R.string.auth_demo),
                onClick = onDemo
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.auth_signup_header),
                style = MaterialTheme.typography.titleLarge
            )
            when (state.signupStep) {
                SignupStep.Credentials -> SignupCredentialsForm(
                    state = state,
                    onSignupEmail = onSignupEmail,
                    onSignupPassword = onSignupPassword,
                    onSignupConfirm = onSignupConfirm,
                    onTermsToggle = onTermsToggle,
                    onContinue = onSignup
                )

                SignupStep.Profile -> SignupProfileForm(
                    state = state,
                    onProfileNameChange = onProfileNameChange,
                    onProfileRoleSelected = onProfileRoleSelected,
                    onProfileSchoolChange = onProfileSchoolChange,
                    onProfileSubjectChange = onProfileSubjectChange,
                    onProfileNicknameChange = onProfileNicknameChange,
                    onBack = onSignupBack,
                    onComplete = onSignup
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onStudentEntry,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Need to join a game instead? Continue as a student")
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SignupCredentialsForm(
    state: AuthUiState,
    onSignupEmail: (String) -> Unit,
    onSignupPassword: (String) -> Unit,
    onSignupConfirm: (String) -> Unit,
    onTermsToggle: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    Text(
        text = stringResource(R.string.auth_signup_step_one),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    AuthTextField(
        value = state.signup.email,
        label = stringResource(R.string.auth_email),
        onValueChange = onSignupEmail
    )
    AuthTextField(
        value = state.signup.password,
        label = stringResource(R.string.auth_password),
        onValueChange = onSignupPassword,
        isPassword = true
    )
    AuthTextField(
        value = state.signup.confirmPassword,
        label = stringResource(R.string.auth_confirm_password),
        onValueChange = onSignupConfirm,
        isPassword = true
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = state.signup.acceptedTerms,
            onCheckedChange = onTermsToggle
        )
        Text(text = stringResource(R.string.auth_terms))
    }
    PrimaryButton(
        text = stringResource(R.string.auth_create_account),
        onClick = onContinue,
        enabled = state.signup.acceptedTerms && !state.loading
    )
}

@Composable
private fun SignupProfileForm(
    state: AuthUiState,
    onProfileNameChange: (String) -> Unit,
    onProfileRoleSelected: (SignupRole) -> Unit,
    onProfileSchoolChange: (String) -> Unit,
    onProfileSubjectChange: (String) -> Unit,
    onProfileNicknameChange: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Text(
        text = stringResource(R.string.auth_signup_step_two),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = stringResource(R.string.auth_signup_step_two_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    SegmentedControl(
        options = listOf(
            SegmentOption(
                id = SignupRole.Teacher.name,
                label = stringResource(R.string.auth_signup_role_teacher)
            ),
            SegmentOption(
                id = SignupRole.Student.name,
                label = stringResource(R.string.auth_signup_role_student)
            )
        ),
        selectedId = if (state.profile.role == SignupRole.None) "" else state.profile.role.name,
        onSelected = { id ->
            val role = SignupRole.values().firstOrNull { it.name == id } ?: SignupRole.None
            onProfileRoleSelected(role)
        },
        modifier = Modifier.fillMaxWidth()
    )
    when (state.profile.role) {
        SignupRole.Teacher -> {
            Text(
                text = stringResource(R.string.auth_signup_teacher_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AuthTextField(
                value = state.profile.fullName,
                label = stringResource(R.string.auth_signup_teacher_name),
                onValueChange = onProfileNameChange
            )
            AuthTextField(
                value = state.profile.school,
                label = stringResource(R.string.auth_signup_teacher_school),
                onValueChange = onProfileSchoolChange
            )
            AuthTextField(
                value = state.profile.subject,
                label = stringResource(R.string.auth_signup_teacher_subject),
                onValueChange = onProfileSubjectChange
            )
        }

        SignupRole.Student -> {
            Text(
                text = stringResource(R.string.auth_signup_student_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AuthTextField(
                value = state.profile.nickname,
                label = stringResource(R.string.auth_signup_student_nickname),
                onValueChange = onProfileNicknameChange
            )
        }

        SignupRole.None -> {
            Text(
                text = stringResource(R.string.auth_signup_role_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SecondaryButton(
            text = stringResource(R.string.auth_signup_back),
            onClick = onBack,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text = stringResource(R.string.auth_signup_complete),
            onClick = onComplete,
            enabled = !state.loading,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
    )
}

@QuizPreviews
@Composable
private fun AuthScreenPreview() {
    QuizMasterTheme {
        AuthScreen(
            state = AuthUiState(),
            snackbarHostState = SnackbarHostState(),
            onLoginEmail = {},
            onLoginPassword = {},
            onSignupEmail = {},
            onSignupPassword = {},
            onSignupConfirm = {},
            onTermsToggle = {},
            onProfileNameChange = {},
            onProfileRoleSelected = {},
            onProfileSchoolChange = {},
            onProfileSubjectChange = {},
            onProfileNicknameChange = {},
            onSignupBack = {},
            onLogin = {},
            onSignup = {},
            onDemo = {},
            onGoogle = {},
            onStudentEntry = {}
        )
    }
}
