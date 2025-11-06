package com.classroom.quizmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.data.model.UserRole
import com.classroom.quizmaster.data.repo.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {

    data class UiState(
        val name: String = "",
        val email: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val selectedRole: UserRole = UserRole.LEARNER,
        val org: String = "",
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    val roles: List<UserRole> = listOf(UserRole.LEARNER, UserRole.TEACHER)

    fun updateName(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun updateConfirmPassword(value: String) = _uiState.update { it.copy(confirmPassword = value, error = null) }
    fun updateOrg(value: String) = _uiState.update { it.copy(org = value, error = null) }
    fun updateRole(role: UserRole) = _uiState.update { it.copy(selectedRole = role, error = null) }

    fun submit() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password
        val confirmPassword = state.confirmPassword

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(error = "Email and password are required.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters.") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepo.signUp(
                name = state.name.trim(),
                email = email,
                password = password,
                role = state.selectedRole,
                org = state.org.trim()
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Unable to create account."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
