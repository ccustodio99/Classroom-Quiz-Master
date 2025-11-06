package com.classroom.quizmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.data.model.User
import com.classroom.quizmaster.data.repo.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class AuthStateViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state

    init {
        viewModelScope.launch {
            authRepo.observeAuthState()
                .catch { _state.value = AuthState.SignedOut }
                .collect { user ->
                    _state.value = if (user == null) {
                        AuthState.SignedOut
                    } else {
                        AuthState.SignedIn(user)
                    }
                }
        }
    }

    fun signOut() {
        authRepo.signOut()
    }
}

sealed interface AuthState {
    object Loading : AuthState
    object SignedOut : AuthState
    data class SignedIn(val user: User) : AuthState
}
