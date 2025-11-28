package com.classroom.quizmaster.ui.teacher.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.usecase.ExportReportUseCase
import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import timber.log.Timber

data class ReportsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val classroomName: String = "All classrooms",
    val selectedClassroomId: String? = null,
    val classroomOptions: List<ReportClassroomOption> = emptyList(),
    val lastUpdated: Instant? = null,
    val lastUpdatedLabel: String = "Not updated yet",
    val average: Int = 0,
    val median: Int = 0,
    val classPreAverage: Int = 0,
    val classPostAverage: Int = 0,
    val classDelta: Int = 0,
    val topTopics: List<TopicMasteryUi> = emptyList(),
    val assignments: List<AssignmentPerformanceUi> = emptyList(),
    val studentProgress: List<StudentProgressUi> = emptyList(),
    val studentImprovement: List<StudentImprovementUi> = emptyList(),
    val completionOverview: CompletionOverview = CompletionOverview(),
    val assignmentCompletion: List<AssignmentCompletionUi> = emptyList(),
    val studentCompletion: List<StudentCompletionUi> = emptyList(),
    val studentProgressAtRisk: List<StudentProgressAtRiskUi> = emptyList(),
    val questionDifficulty: List<QuestionDifficultyUi> = emptyList(),
    val exportState: ExportState = ExportState()
)

data class TopicMasteryUi(
    val topicName: String,
    val averageScore: Int
)

data class AssignmentPerformanceUi(
    val assignmentId: String,
    val title: String,
    val pValue: Int,
    val topDistractor: String = "n/a",
    val distractorRate: Int = 0
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

data class CompletionOverview(
    val onTimeRate: Int = 0,
    val lateRate: Int = 0,
    val notAttemptedRate: Int = 0
)

data class AssignmentCompletionUi(
    val assignmentId: String,
    val title: String,
    val totalStudents: Int,
    val completedOnTime: Int,
    val completedLate: Int,
    val notStarted: Int
)

data class StudentCompletionUi(
    val studentId: String,
    val name: String,
    val completedOnTime: Int,
    val completedLate: Int,
    val notAttempted: Int,
    val totalAssignments: Int
)

enum class StudentTrend { IMPROVING, STABLE, DECLINING, UNKNOWN }

data class StudentProgressAtRiskUi(
    val studentId: String,
    val name: String,
    val averageScore: Int,
    val trend: StudentTrend,
    val completedCount: Int,
    val missingCount: Int,
    val atRisk: Boolean
)

enum class QuestionDifficultyTag { TOO_EASY, TOO_HARD, CONFUSING, NORMAL, INSUFFICIENT_DATA }

data class QuestionDifficultyUi(
    val questionId: String,
    val assignmentTitle: String,
    val questionPreview: String,
    val pValue: Int,
    val topWrongOptionLabel: String?,
    val topWrongOptionRate: Int?,
    val difficultyTag: QuestionDifficultyTag,
    val attemptCount: Int
)

data class ReportClassroomOption(
    val id: String?,
    val name: String
)

data class ExportState(
    val isExporting: Boolean = false,
    val format: ExportFormat? = null,
    val lastCsvUrl: String? = null,
    val lastPdfUrl: String? = null,
    val lastExportError: String? = null
)

enum class ExportFormat { CSV, PDF }

sealed interface ReportsEvent {
    data class Snackbar(val message: String, val actionLabel: String? = null, val action: SnackbarAction? = null) : ReportsEvent
}

sealed interface SnackbarAction {
    data class OpenLink(val url: String) : SnackbarAction
    data class Retry(val format: ExportFormat) : SnackbarAction
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val assignmentRepositoryUi: AssignmentRepositoryUi,
    private val assignmentRepository: AssignmentRepository,
    private val classroomRepository: ClassroomRepository,
    private val quizRepository: QuizRepository,
    private val exportReportUseCase: ExportReportUseCase
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val loading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val exportState = MutableStateFlow(ExportState())

    private val reportsData = assignmentRepositoryUi.reports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ReportsUiState(isLoading = true)
        )

    private val _events = MutableSharedFlow<ReportsEvent>()
    val events: SharedFlow<ReportsEvent> = _events

    val uiState: StateFlow<ReportsUiState> = combine(
        reportsData,
        refreshing,
        loading,
        errorMessage,
        exportState
    ) { data, isRefreshing, isLoading, error, export ->
        data.copy(
            isRefreshing = isRefreshing,
            isLoading = isLoading,
            errorMessage = error,
            exportState = export
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReportsUiState(isLoading = true)
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.update { true }
            loading.update { true }
            val result = runCatching {
                assignmentRepository.refreshAssignments()
                classroomRepository.refresh()
                quizRepository.refresh()
            }
            result.onFailure { err ->
                errorMessage.update { err.message ?: "Unable to refresh reports" }
                Timber.w(err, "Reports refresh failed")
            }.onSuccess {
                errorMessage.update { null }
            }
            loading.update { false }
            refreshing.update { false }
        }
    }

    fun exportCsv(sessionId: String? = null) {
        export(ExportFormat.CSV, sessionId)
    }

    fun exportPdf(sessionId: String? = null) {
        export(ExportFormat.PDF, sessionId)
    }

    fun onClassroomSelected(classroomId: String?) {
        assignmentRepositoryUi.selectReportsClassroom(classroomId)
    }

    private fun export(format: ExportFormat, sessionId: String?) {
        if (exportState.value.isExporting) return
        viewModelScope.launch {
            exportState.update { it.copy(isExporting = true, format = format, lastExportError = null) }
            val result = exportReportUseCase(sessionId)
            result.onSuccess { links ->
                exportState.update {
                    it.copy(
                        isExporting = false,
                        format = null,
                        lastCsvUrl = links.csvUrl,
                        lastPdfUrl = links.pdfUrl,
                        lastExportError = null
                    )
                }
                val target = if (format == ExportFormat.CSV) links.csvUrl else links.pdfUrl
                _events.emit(
                    ReportsEvent.Snackbar(
                        message = if (format == ExportFormat.CSV) "CSV exported" else "PDF exported",
                        actionLabel = "Open",
                        action = SnackbarAction.OpenLink(target)
                    )
                )
            }.onFailure { err ->
                val friendly = friendlyExportError(err)
                exportState.update { it.copy(isExporting = false, format = null, lastExportError = friendly) }
                _events.emit(
                    ReportsEvent.Snackbar(
                        message = friendly,
                        actionLabel = "Retry",
                        action = SnackbarAction.Retry(format)
                    )
                )
                Timber.w(err, "Export failed")
            }
        }
    }

    private fun friendlyExportError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("submissions", ignoreCase = true) -> "Exports will be available once there are submissions."
            message.contains("teacher access", ignoreCase = true) -> "You need teacher access to export this report."
            message.contains("sync", ignoreCase = true) -> "Sync is still running. Try again in a moment."
            else -> message.ifBlank { "Export failed. Try again." }
        }
    }
}
