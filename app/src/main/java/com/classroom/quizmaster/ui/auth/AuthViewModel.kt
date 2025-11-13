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

data class SignupFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false
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
        when {
            !signup.email.contains("@") -> emitError("Use a school email.")
            signup.password.length < 8 -> emitError("Password must be at least 8 characters.")
            signup.password != signup.confirmPassword -> emitError("Passwords do not match.")
            !signup.acceptedTerms -> emitError("Accept the terms to continue.")
            else -> {
                delay(600)
                _effects.emit(AuthEffect.TeacherAuthenticated)
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
