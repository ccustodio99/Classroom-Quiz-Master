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
    val topTopics: List<String> = emptyList(),
    val questionRows: List<ReportRowUi> = emptyList(),
    val lastUpdated: String = ""
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    assignmentRepositoryUi: AssignmentRepositoryUi
) : ViewModel() {
    val uiState: StateFlow<ReportsUiState> = assignmentRepositoryUi.reports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}
