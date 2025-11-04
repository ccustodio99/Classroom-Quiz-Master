package com.classroom.quizmaster.ui.feature.profile

import androidx.lifecycle.ViewModel
import com.classroom.quizmaster.domain.model.PersonaType
import com.classroom.quizmaster.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        // Mock user data for demonstration
        val mockUser = User(
            id = "123",
            name = "Alex Doe",
            role = PersonaType.Learner,
            email = "alex.doe@example.com"
        )
        _uiState.value = ProfileUiState(user = mockUser)
    }

    fun logout() {
        // TODO: Implement actual logout logic
        _uiState.value = ProfileUiState(user = null)
    }
}
