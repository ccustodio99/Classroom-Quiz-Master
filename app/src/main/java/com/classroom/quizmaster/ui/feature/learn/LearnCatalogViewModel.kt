package com.classroom.quizmaster.ui.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.CourseSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LearnCatalogViewModel(
    container: AppContainer
) : ViewModel() {

    private val catalogRepository = container.catalogRepository

    private val _uiState = MutableStateFlow(LearnCatalogUiState(isLoading = true))
    val uiState: StateFlow<LearnCatalogUiState> = _uiState.asStateFlow()
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(
                catalogRepository.observeCourses(),
                query
            ) { courses, search ->
                val filtered = if (search.isBlank()) {
                    courses
                } else {
                    courses.filter { course ->
                        course.title.contains(search, ignoreCase = true) ||
                                course.objectives().any { it.contains(search, ignoreCase = true) }
                    }
                }
                LearnCatalogUiState(
                    courses = filtered,
                    query = search,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateQuery(input: String) {
        query.value = input
    }
}

data class LearnCatalogUiState(
    val courses: List<CourseSummary> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false
)

private fun CourseSummary.objectives(): List<String> =
    units.flatMap { it.objectiveTags }.distinct()
