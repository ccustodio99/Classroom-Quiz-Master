package com.classroom.quizmaster.ui.teacher.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.model.ReportRowUi
import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val average: Int = 0,
    val median: Int = 0,
    val classPreAverage: Int = 0,
    val classPostAverage: Int = 0,
    val classDelta: Int = 0,
    val topTopics: List<String> = emptyList(),
    val questionRows: List<ReportRowUi> = emptyList(),
    val lastUpdated: String = "",
    val studentProgress: List<StudentProgressUi> = emptyList(),
    val studentImprovement: List<StudentImprovementUi> = emptyList()
)

data class StudentProgressUi(
    val name: String,
    val completed: Int,
    val total: Int,
    val score: Int
)

data class StudentImprovementUi(
    val name: String,
    val preAvg: Int,
    val postAvg: Int,
    val delta: Int,
    val preAttempts: Int,
    val postAttempts: Int
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    assignmentRepositoryUi: AssignmentRepositoryUi
) : ViewModel() {
    val uiState: StateFlow<ReportsUiState> = assignmentRepositoryUi.reports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}
