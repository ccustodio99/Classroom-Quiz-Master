package com.classroom.quizmaster.ui.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.ActivityTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityTabViewModel(
    container: AppContainer,
    private val userId: String
) : ViewModel() {

    private val catalogRepository = container.catalogRepository

    private val _uiState = MutableStateFlow(ActivityTabUiState(isLoading = true))
    val uiState: StateFlow<ActivityTabUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            catalogRepository.observeActivityTimeline(userId).collect { timeline ->
                _uiState.value = ActivityTabUiState(
                    timeline = timeline,
                    isLoading = false
                )
            }
        }
    }
}

data class ActivityTabUiState(
    val timeline: ActivityTimeline? = null,
    val isLoading: Boolean = false
)

