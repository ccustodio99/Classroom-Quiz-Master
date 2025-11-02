package com.classroom.quizmaster.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.AccountStatus
import com.classroom.quizmaster.domain.model.UserAccount
import com.classroom.quizmaster.domain.model.UserRole
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    container: AppContainer
) : ViewModel() {

    private val authAgent = container.authAgent

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authAgent.observeCurrentAccount().collectLatest { account ->
                _uiState.update { state ->
                    state.copy(
                        currentUser = account,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
        viewModelScope.launch {
            authAgent.observePendingAccounts().collectLatest { pending ->
                _uiState.update { state ->
                    state.copy(pendingAccounts = pending.sortedBy { it.createdAt })
                }
            }
        }
    }

    fun login(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authAgent.login(email, password)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun register(displayName: String, email: String, password: String, role: UserRole) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authAgent.register(email, password, displayName, role)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    registrationSuccess = result.isSuccess && role != UserRole.Admin,
                    error = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun approveAccount(accountId: String) {
        val admin = _uiState.value.currentUser ?: return
        if (admin.role != UserRole.Admin || admin.status != AccountStatus.Active) return
        viewModelScope.launch {
            val result = authAgent.approve(admin.id, accountId)
            _uiState.update { state ->
                state.copy(error = result.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authAgent.logout()
            _uiState.update { it.copy(registrationSuccess = false) }
        }
    }

    fun clearRegistrationFlag() {
        _uiState.update { it.copy(registrationSuccess = false) }
    }
}

data class SessionUiState(
    val currentUser: UserAccount? = null,
    val pendingAccounts: List<UserAccount> = emptyList(),
    val isLoading: Boolean = false,
    val registrationSuccess: Boolean = false,
    val error: String? = null
) {
    val roleLabel: String
        get() = currentUser?.role?.name?.lowercase(Locale.ROOT)?.replaceFirstChar { it.titlecase(Locale.ROOT) }
            ?: "Guest"
}
