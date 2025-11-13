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

data class ClassroomOverviewUi(
    val id: String,
    val name: String,
    val grade: String?,
    val topicCount: Int,
    val quizCount: Int
)

data class HomeActionCard(
    val id: String,
    val title: String,
    val description: String,
    val route: String,
    val ctaLabel: String = "Open",
    val primary: Boolean = false
)

data class TeacherHomeUiState(
    val greeting: String = "Good day",
    val connectivityHeadline: String = "",
    val connectivitySupporting: String = "",
    val statusChips: List<StatusChipUi> = emptyList(),
    val classrooms: List<ClassroomOverviewUi> = emptyList(),
    val actionCards: List<HomeActionCard> = emptyList(),
    val recentQuizzes: List<QuizOverviewUi> = emptyList(),
    val emptyMessage: String = "",
    val isOfflineDemo: Boolean = false
)

const val ACTION_CREATE_QUIZ = "teacher_home:create_quiz"
const val ACTION_LAUNCH_SESSION = "teacher_home:launch_session"
const val ACTION_ASSIGNMENTS = "teacher_home:assignments"
const val ACTION_REPORTS = "teacher_home:reports"

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
