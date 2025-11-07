package com.classroom.quizmaster.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.util.NicknamePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val nickname: String = "Student",
    val loading: Boolean = false,
    val error: String? = null,
    val nicknameError: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState())

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun onDisplayNameChange(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value)
    }

    fun onNicknameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            nickname = value,
            nicknameError = NicknamePolicy.validationError(value)
        )
    }

    fun signInTeacher() = launchWithProgress {
        authRepository.signInWithEmail(_uiState.value.email, _uiState.value.password)
    }

    fun createTeacher() = launchWithProgress {
        authRepository.signUpWithEmail(
            email = _uiState.value.email,
            password = _uiState.value.password,
            displayName = _uiState.value.displayName.ifBlank { _uiState.value.email.substringBefore("@") }
        )
    }

    fun continueAsStudent(onSuccess: () -> Unit) {
        val current = _uiState.value
        val violation = NicknamePolicy.validationError(current.nickname)
        if (violation != null) {
            _uiState.value = current.copy(nicknameError = violation)
            return
        }
        launchWithProgress {
            val sanitized = NicknamePolicy.sanitize(current.nickname, current.email)
            authRepository.signInAnonymously(sanitized)
            onSuccess()
        }
    }

    fun handleGoogleToken(idToken: String) = launchWithProgress {
        authRepository.signInWithGoogle(idToken)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun launchWithProgress(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching { block() }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = throwable.message ?: "Authentication failed",
                        loading = false
                    )
                }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(loading = false)
                }
        }
    }
}
