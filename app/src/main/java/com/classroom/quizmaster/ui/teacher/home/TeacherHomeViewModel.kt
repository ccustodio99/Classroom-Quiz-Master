package com.classroom.quizmaster.ui.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.model.QuizOverviewUi
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.state.QuizRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class QuickStat(
    val label: String,
    val value: String,
    val trendLabel: String,
    val positive: Boolean
)

data class HomeActionCard(
    val id: String,
    val title: String,
    val description: String,
    val route: String
)

data class TeacherHomeUiState(
    val greeting: String = "Good day",
    val connectivityHeadline: String = "",
    val connectivitySupporting: String = "",
    val statusChips: List<StatusChipUi> = emptyList(),
    val quickStats: List<QuickStat> = emptyList(),
    val actionCards: List<HomeActionCard> = emptyList(),
    val recentQuizzes: List<QuizOverviewUi> = emptyList(),
    val emptyMessage: String = "",
    val isOfflineDemo: Boolean = false
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    quizRepositoryUi: QuizRepositoryUi
) : ViewModel() {

    val uiState: StateFlow<TeacherHomeUiState> = quizRepositoryUi.teacherHome
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TeacherHomeUiState()
        )
}
