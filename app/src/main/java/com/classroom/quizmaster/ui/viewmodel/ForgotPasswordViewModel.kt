package com.classroom.quizmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.data.repo.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val isLoading: Boolean = false,
        val success: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun submit() {
        val email = _uiState.value.email.trim()
        if (email.isEmpty()) {
            _uiState.update { it.copy(error = "Email is required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            authRepo.sendPasswordReset(email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, success = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to send reset email."
                        )
                    }
                }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, success = false) }
    }
}
