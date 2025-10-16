package com.classroom.quizmaster.ui.feature.reports

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.agents.CsvRow
import com.classroom.quizmaster.domain.model.ClassReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportsViewModel(
    private val container: AppContainer,
    private val moduleId: String,
    private val snackbarHostState: SnackbarHostState
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState(isLoading = true))
    val uiState: StateFlow<ReportsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ReportsUiState(isLoading = true)
            runCatching { container.scoringAnalyticsAgent.buildClassReport(moduleId) }
                .onSuccess { report ->
                    _uiState.value = ReportsUiState(isLoading = false, report = report)
                }
                .onFailure { error ->
                    _uiState.value = ReportsUiState(isLoading = false, errorMessage = error.message)
                }
        }
    }

    fun exportClassPdf() {
        val report = _uiState.value.report ?: return
        viewModelScope.launch {
            runCatching { container.reportExportAgent.exportClassPdf(report) }
                .onSuccess { file -> snackbarHostState.showSnackbar("PDF saved: ${file.path}") }
                .onFailure { snackbarHostState.showSnackbar("Export failed: ${it.message}") }
        }
    }

    fun exportCsv() {
        val report = _uiState.value.report ?: return
        viewModelScope.launch {
            val rows = buildCsv(report)
            runCatching { container.reportExportAgent.exportCsv(rows) }
                .onSuccess { file -> snackbarHostState.showSnackbar("CSV saved: ${file.path}") }
                .onFailure { snackbarHostState.showSnackbar("Export failed: ${it.message}") }
        }
    }

    private fun buildCsv(report: ClassReport): List<CsvRow> {
        val header = CsvRow(listOf("Student", "Pre %", "Post %"))
        val data = report.attempts.map { attempt ->
            CsvRow(
                listOf(
                    attempt.student.displayName,
                    attempt.prePercent?.let { String.format("%.1f", it) } ?: "",
                    attempt.postPercent?.let { String.format("%.1f", it) } ?: ""
                )
            )
        }
        return listOf(header) + data
    }
}

data class ReportsUiState(
    val isLoading: Boolean = false,
    val report: ClassReport? = null,
    val errorMessage: String? = null
)
