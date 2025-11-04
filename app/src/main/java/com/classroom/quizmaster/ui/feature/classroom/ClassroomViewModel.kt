package com.classroom.quizmaster.ui.feature.classroom

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClassroomUiState(
    val currentTab: ClassroomTab = ClassroomTab.Stream,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ClassroomViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ClassroomUiState())
    val uiState = _uiState.asStateFlow()

    fun selectTab(tab: ClassroomTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }
}
