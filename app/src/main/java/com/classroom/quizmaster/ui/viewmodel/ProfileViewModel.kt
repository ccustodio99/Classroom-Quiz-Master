package com.classroom.quizmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.data.model.User
import com.classroom.quizmaster.data.model.UserRole
import com.classroom.quizmaster.data.repo.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {

    data class UiState(
        val user: User? = null,
        val name: String = "",
        val org: String = "",
        val role: UserRole = UserRole.LEARNER,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val isDeleting: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch {
            authRepo.observeAuthState()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            user = null,
                            name = "",
                            org = "",
                            role = UserRole.LEARNER,
                            isLoading = false,
                            error = throwable.message ?: "Unable to load profile."
                        )
                    }
                }
                .collect { profile ->
                    _uiState.update { state ->
                        if (profile == null) {
                            state.copy(
                                user = null,
                                name = "",
                                org = "",
                                role = UserRole.LEARNER,
                                isLoading = false
                            )
                        } else {
                            state.copy(
                                user = profile,
                                name = profile.name,
                                org = profile.org,
                                role = profile.role,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, error = null, message = null) }
    }

    fun updateOrg(value: String) {
        _uiState.update { it.copy(org = value, error = null, message = null) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, message = null) }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.user == null) {
            _uiState.update { it.copy(error = "You need to be signed in to update your profile.") }
            return
        }
        if (state.name.trim().isEmpty()) {
            _uiState.update { it.copy(error = "Name cannot be empty.") }
            return
        }
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }
            authRepo.updateProfile(
                name = state.name.trim(),
                org = state.org.trim(),
                role = state.role
            ).onSuccess { updated ->
                _uiState.update {
                    it.copy(
                        user = updated,
                        name = updated.name,
                        org = updated.org,
                        role = updated.role,
                        isSaving = false,
                        message = "Profile updated."
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = throwable.message ?: "Unable to update profile."
                    )
                }
            }
        }
    }

    fun deleteAccount() {
        val state = _uiState.value
        if (state.user == null) {
            _uiState.update { it.copy(error = "You need to be signed in to delete your account.") }
            return
        }
        if (state.isDeleting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null, message = null) }
            authRepo.deleteAccount()
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, message = "Account deleted.") }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = throwable.message ?: "Unable to delete account."
                        )
                    }
                }
        }
    }

    fun signOut() {
        authRepo.signOut()
    }
}
