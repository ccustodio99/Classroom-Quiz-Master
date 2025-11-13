package com.classroom.quizmaster.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginFormState(
    val email: String = "",
    val password: String = ""
)

enum class SignupStage { Credentials, Profile }

enum class SignupRole { Teacher, Student }

data class SignupFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false,
    val stage: SignupStage = SignupStage.Credentials,
    val role: SignupRole? = null,
    val fullName: String = "",
    val school: String = "",
    val subject: String = "",
    val nickname: String = "",
    val gradeLevel: String = "",
    val avatarId: String? = null
)

data class AuthUiState(
    val login: LoginFormState = LoginFormState(),
    val signup: SignupFormState = SignupFormState(),
    val loading: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
    val demoModeEnabled: Boolean = false
)

sealed interface AuthEffect {
    data object TeacherAuthenticated : AuthEffect
    data object DemoMode : AuthEffect
    data class Error(val message: String) : AuthEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _effects = MutableSharedFlow<AuthEffect>()
    val effects: SharedFlow<AuthEffect> = _effects

    fun updateLoginEmail(value: String) {
        _uiState.value = _uiState.value.copy(login = _uiState.value.login.copy(email = value))
    }

    fun updateLoginPassword(value: String) {
        _uiState.value = _uiState.value.copy(login = _uiState.value.login.copy(password = value))
    }

    fun updateSignupEmail(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(email = value))
    }

    fun updateSignupPassword(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(password = value))
    }

    fun updateSignupConfirm(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(confirmPassword = value))
    }

    fun toggleTerms(value: Boolean) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(acceptedTerms = value))
    }

    fun updateSignupRole(role: SignupRole) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(role = role))
    }

    fun updateSignupFullName(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(fullName = value))
    }

    fun updateSignupSchool(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(school = value))
    }

    fun updateSignupSubject(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(subject = value))
    }

    fun updateSignupNickname(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(nickname = value))
    }

    fun updateSignupGradeLevel(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(gradeLevel = value))
    }

    fun updateSignupAvatar(avatarId: String?) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(avatarId = avatarId))
    }

    fun backToSignupCredentials() {
        _uiState.value = _uiState.value.copy(
            signup = _uiState.value.signup.copy(stage = SignupStage.Credentials)
        )
    }

    fun signInTeacher() = launchWithProgress {
        val login = _uiState.value.login
        if (!login.email.contains("@") || login.password.length < 6) {
            emitError("Enter a valid email and 6+ char password.")
            return@launchWithProgress
        }
        delay(500)
        _effects.emit(AuthEffect.TeacherAuthenticated)
    }

    fun signUpTeacher() = launchWithProgress {
        val signup = _uiState.value.signup
        when (signup.stage) {
            SignupStage.Credentials -> when {
                !signup.email.contains("@") -> emitError("Use a school email.")
                signup.password.length < 8 -> emitError("Password must be at least 8 characters.")
                signup.password != signup.confirmPassword -> emitError("Passwords do not match.")
                !signup.acceptedTerms -> emitError("Accept the terms to continue.")
                else -> {
                    _uiState.value = _uiState.value.copy(
                        signup = signup.copy(stage = SignupStage.Profile)
                    )
                }
            }

            SignupStage.Profile -> when {
                signup.role == null -> emitError("Select a role to continue.")
                signup.role == SignupRole.Teacher && signup.fullName.isBlank() -> emitError("Enter your name to personalize things.")
                signup.role == SignupRole.Teacher && signup.school.isBlank() -> emitError("Share your school or class to finish setup.")
                signup.role == SignupRole.Student && signup.nickname.isBlank() -> emitError("Pick a nickname so classmates know you.")
                else -> {
                    delay(600)
                    _uiState.value = _uiState.value.copy(signup = SignupFormState())
                    _effects.emit(AuthEffect.TeacherAuthenticated)
                }
            }
        }
    }

    fun continueOfflineDemo() = launchWithProgress {
        delay(300)
        _uiState.value = _uiState.value.copy(
            demoModeEnabled = true,
            bannerMessage = "Demo lessons unlocked offline"
        )
        _effects.emit(AuthEffect.DemoMode)
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = message, loading = false)
            _effects.emit(AuthEffect.Error(message))
        }
    }

    private fun launchWithProgress(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            runCatching { block() }
                .onFailure {
                    emitError(it.message ?: "Something went wrong")
                }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }
}
