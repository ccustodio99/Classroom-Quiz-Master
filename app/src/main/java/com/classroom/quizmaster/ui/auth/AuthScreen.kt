package com.classroom.quizmaster.ui.auth

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.R
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
                AuthEffect.DemoMode -> {
                    snackbarHost.showSnackbar("Offline demo enabled")
                    onTeacherAuthenticated()
                }
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
    var teacherTab by rememberSaveable { mutableStateOf(TeacherAuthTab.SignIn) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        WelcomeBanner()
        HeroSection()
        if (state.isOffline) {
            StatusMessageCard(
                message = stringResource(id = R.string.auth_offline_message),
                isError = false
            )
        }
        state.bannerMessage?.takeIf { it.isNotBlank() }?.let {
            StatusMessageCard(message = it, isError = false)
        }
        state.errorMessage?.takeIf { it.isNotBlank() }?.let {
            StatusMessageCard(message = it, isError = true)
        }
        SnackbarHost(hostState = snackbarHostState) { data ->
            QuizSnackbar(message = data.visuals.message)
        }
        TeacherAuthCard(
            state = state,
            selectedTab = teacherTab,
            onTabSelected = { teacherTab = it },
            onLoginEmail = onLoginEmail,
            onLoginPassword = onLoginPassword,
            onLogin = onLogin,
            onGoogle = onGoogle,
            onDemo = onDemo,
            onSignupEmail = onSignupEmail,
            onSignupPassword = onSignupPassword,
            onSignupConfirm = onSignupConfirm,
            onTermsToggle = onTermsToggle,
            onSignup = onSignup,
            onProfileNameChange = onProfileNameChange,
            onProfileRoleSelected = onProfileRoleSelected,
            onProfileSchoolChange = onProfileSchoolChange,
            onProfileSubjectChange = onProfileSubjectChange,
            onProfileNicknameChange = onProfileNicknameChange,
            onSignupBack = onSignupBack
        )
        StudentEntryCard(onStudentEntry = onStudentEntry)
    }
}

@Composable
private fun WelcomeBanner() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_banner_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.auth_banner_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HeroSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = stringResource(R.string.auth_hero_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TeacherAuthCard(
    state: AuthUiState,
    selectedTab: TeacherAuthTab,
    onTabSelected: (TeacherAuthTab) -> Unit,
    onLoginEmail: (String) -> Unit,
    onLoginPassword: (String) -> Unit,
    onLogin: () -> Unit,
    onGoogle: () -> Unit,
    onDemo: () -> Unit,
    onSignupEmail: (String) -> Unit,
    onSignupPassword: (String) -> Unit,
    onSignupConfirm: (String) -> Unit,
    onTermsToggle: (Boolean) -> Unit,
    onSignup: () -> Unit,
    onProfileNameChange: (String) -> Unit,
    onProfileRoleSelected: (SignupRole) -> Unit,
    onProfileSchoolChange: (String) -> Unit,
    onProfileSubjectChange: (String) -> Unit,
    onProfileNicknameChange: (String) -> Unit,
    onSignupBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.auth_teacher_section_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.auth_teacher_section_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SegmentedControl(
                options = listOf(
                    SegmentOption(
                        id = TeacherAuthTab.SignIn.name,
                        label = stringResource(R.string.auth_teacher_tab_sign_in)
                    ),
                    SegmentOption(
                        id = TeacherAuthTab.CreateAccount.name,
                        label = stringResource(R.string.auth_teacher_tab_create)
                    )
                ),
                selectedId = selectedTab.name,
                onSelected = { option ->
                    onTabSelected(TeacherAuthTab.valueOf(option))
                },
                modifier = Modifier.fillMaxWidth()
            )

            when (selectedTab) {
                TeacherAuthTab.SignIn -> TeacherSignInForm(
                    state = state,
                    onLoginEmail = onLoginEmail,
                    onLoginPassword = onLoginPassword,
                    onLogin = onLogin,
                    onGoogle = onGoogle,
                    onDemo = onDemo
                )

                TeacherAuthTab.CreateAccount -> when (state.signupStep) {
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
        }
    }
}

@Composable
private fun StatusMessageCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Text(
            text = message,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun TeacherSignInForm(
    state: AuthUiState,
    onLoginEmail: (String) -> Unit,
    onLoginPassword: (String) -> Unit,
    onLogin: () -> Unit,
    onGoogle: () -> Unit,
    onDemo: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val emailError = state.login.email.isNotBlank() && !state.login.email.contains("@")
    val passwordError = state.login.password.isNotEmpty() && state.login.password.length < 6

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AuthTextField(
            value = state.login.email,
            label = stringResource(R.string.auth_email),
            onValueChange = onLoginEmail,
            supportingText = stringResource(id = R.string.auth_email_helper),
            isError = emailError,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            keyboardType = KeyboardType.Email
        )
        AuthTextField(
            value = state.login.password,
            label = stringResource(R.string.auth_password),
            onValueChange = onLoginPassword,
            isPassword = true,
            supportingText = if (passwordError) {
                stringResource(R.string.auth_password_error_sign_in)
            } else {
                stringResource(R.string.auth_password_helper_sign_in)
            },
            isError = passwordError,
            imeAction = ImeAction.Done,
            onImeAction = {
                focusManager.clearFocus()
                onLogin()
            }
        )
        PrimaryButton(
            text = stringResource(R.string.auth_sign_in),
            onClick = onLogin,
            enabled = state.login.email.isNotBlank() &&
                state.login.password.length >= 6 &&
                !emailError &&
                !passwordError &&
                !state.loading,
            isLoading = state.loading
        )
        SecondaryButton(
            text = stringResource(R.string.auth_google),
            onClick = onGoogle,
            leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.GTranslate, contentDescription = null) }
        )
        TextButton(onClick = onDemo) {
            Text(text = stringResource(R.string.auth_demo))
        }
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
    val focusManager = LocalFocusManager.current
    val emailError = state.signup.email.isNotBlank() && !state.signup.email.contains("@")
    val passwordShort = state.signup.password.isNotEmpty() && state.signup.password.length < 8
    val confirmMismatch = state.signup.confirmPassword.isNotEmpty() &&
        state.signup.confirmPassword != state.signup.password

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.auth_signup_step_one),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.auth_signup_title_step_one),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.auth_signup_step_one_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AuthTextField(
            value = state.signup.email,
            label = stringResource(R.string.auth_email),
            onValueChange = onSignupEmail,
            supportingText = stringResource(id = R.string.auth_email_helper),
            isError = emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )
        AuthTextField(
            value = state.signup.password,
            label = stringResource(R.string.auth_password),
            onValueChange = onSignupPassword,
            isPassword = true,
            supportingText = if (passwordShort) {
                stringResource(id = R.string.auth_password_error_sign_up)
            } else {
                stringResource(id = R.string.auth_password_helper_sign_up)
            },
            isError = passwordShort,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )
        AuthTextField(
            value = state.signup.confirmPassword,
            label = stringResource(R.string.auth_confirm_password),
            onValueChange = onSignupConfirm,
            isPassword = true,
            supportingText = if (confirmMismatch) {
                stringResource(R.string.auth_confirm_password_error)
            } else {
                stringResource(R.string.auth_confirm_password_helper)
            },
            isError = confirmMismatch,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() }
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
            text = stringResource(R.string.common_continue),
            onClick = onContinue,
            enabled = state.signup.acceptedTerms &&
                state.signup.email.isNotBlank() &&
                state.signup.password.length >= 8 &&
                state.signup.confirmPassword.isNotBlank() &&
                !emailError &&
                !passwordShort &&
                !confirmMismatch &&
                !state.loading,
            isLoading = state.loading
        )
    }
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
    val focusManager = LocalFocusManager.current
    val canComplete = when (state.profile.role) {
        SignupRole.Teacher -> state.profile.fullName.isNotBlank() && state.profile.school.isNotBlank()
        SignupRole.Student -> state.profile.nickname.isNotBlank()
        SignupRole.None -> false
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.auth_signup_step_two),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.auth_signup_title_step_two),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.auth_signup_step_two_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        Text(
            text = stringResource(R.string.auth_signup_role_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (state.profile.role) {
            SignupRole.Teacher -> TeacherProfileFields(
                state = state,
                onProfileNameChange = onProfileNameChange,
                onProfileSchoolChange = onProfileSchoolChange,
                onProfileSubjectChange = onProfileSubjectChange
            )

            SignupRole.Student -> {
                Text(
                    text = stringResource(R.string.auth_signup_student_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AuthTextField(
                    value = state.profile.nickname,
                    label = stringResource(R.string.auth_signup_student_nickname),
                    onValueChange = onProfileNicknameChange,
                    supportingText = stringResource(R.string.auth_signup_student_helper),
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() }
                )
            }

            SignupRole.None -> Unit
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
                enabled = canComplete && !state.loading,
                isLoading = state.loading,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TeacherProfileFields(
    state: AuthUiState,
    onProfileNameChange: (String) -> Unit,
    onProfileSchoolChange: (String) -> Unit,
    onProfileSubjectChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.auth_signup_teacher_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AuthTextField(
            value = state.profile.fullName,
            label = stringResource(R.string.auth_signup_teacher_name),
            onValueChange = onProfileNameChange,
            supportingText = stringResource(R.string.auth_signup_teacher_name_helper),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )
        AuthTextField(
            value = state.profile.school,
            label = stringResource(R.string.auth_signup_teacher_school),
            onValueChange = onProfileSchoolChange,
            supportingText = stringResource(R.string.auth_signup_teacher_school_helper),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AuthTextField(
                value = state.profile.subject,
                label = stringResource(R.string.auth_signup_teacher_subject),
                onValueChange = onProfileSubjectChange,
                supportingText = stringResource(R.string.auth_signup_teacher_subject_helper),
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() }
            )
            Text(
                text = stringResource(R.string.auth_signup_teacher_subject_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val suggestions = listOf(
                stringResource(R.string.auth_subject_chip_math),
                stringResource(R.string.auth_subject_chip_science),
                stringResource(R.string.auth_subject_chip_english),
                stringResource(R.string.auth_subject_chip_other)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    val selected = state.profile.subject.equals(suggestion, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onProfileSubjectChange(suggestion) },
                        label = { Text(suggestion) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    supportingText: String? = null,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    keyboardType: KeyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
    onImeAction: (() -> Unit)? = null
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val effectiveTransformation = when {
        isPassword && passwordVisible -> VisualTransformation.None
        isPassword -> PasswordVisualTransformation()
        else -> VisualTransformation.None
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { helper ->
            { Text(text = helper, style = MaterialTheme.typography.bodySmall) }
        },
        visualTransformation = effectiveTransformation,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    val description = if (passwordVisible) {
                        stringResource(R.string.auth_password_hide)
                    } else {
                        stringResource(R.string.auth_password_show)
                    }
                    Icon(imageVector = icon, contentDescription = description)
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() },
            onDone = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() }
        )
    )
}

@Composable
private fun StudentEntryCard(onStudentEntry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_student_heading),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.auth_student_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(
                text = stringResource(R.string.auth_continue_student),
                onClick = onStudentEntry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private enum class TeacherAuthTab { SignIn, CreateAccount }

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
