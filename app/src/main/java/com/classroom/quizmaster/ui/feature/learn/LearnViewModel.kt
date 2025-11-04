package com.classroom.quizmaster.ui.feature.learn

import androidx.lifecycle.ViewModel
import com.classroom.quizmaster.domain.model.Class
import com.classroom.quizmaster.domain.model.ClassJoinPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LearnUiState(
    val classes: List<Class> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredClasses: List<Class>
        get() = if (searchQuery.isBlank()) {
            classes
        } else {
            classes.filter { it.subject.contains(searchQuery, ignoreCase = true) }
        }
}

class LearnViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Mock data for demonstration
        val mockClasses = listOf(
            Class("1", "ALG101", "Section A", "Algebra 101", "teacher1", joinPolicy = ClassJoinPolicy.OPEN),
            Class("2", "BIO202", "Section B", "Biology Basics", "teacher2", joinPolicy = ClassJoinPolicy.INVITE_ONLY),
            Class("3", "HIS301", "Section C", "World History", "teacher3", joinPolicy = ClassJoinPolicy.OPEN)
        )
        _uiState.value = LearnUiState(classes = mockClasses)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
