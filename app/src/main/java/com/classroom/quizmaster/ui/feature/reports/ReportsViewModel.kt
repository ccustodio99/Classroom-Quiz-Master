package com.classroom.quizmaster.ui.feature.reports

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.ClassReport
import com.classroom.quizmaster.domain.model.CsvRow
import java.util.Locale
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
            try {
                val report = container.scoringAnalyticsAgent.buildClassReport(moduleId)
                _uiState.value = ReportsUiState(isLoading = false, report = report)
            } catch (error: Exception) {
                val message = error.message?.takeIf { it.isNotBlank() }
                    ?: "Failed to load reports"
                _uiState.value = ReportsUiState(isLoading = false, errorMessage = message)
            }
        }
    }

    fun exportClassPdf() {
        val report = _uiState.value.report ?: return
        viewModelScope.launch {
            try {
                val file = container.reportExportAgent.exportClassPdf(report)
                snackbarHostState.showSnackbar("PDF saved: ${file.path}")
            } catch (error: Exception) {
                val message = error.message?.takeIf { it.isNotBlank() } ?: "Export failed"
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    fun exportCsv() {
        val report = _uiState.value.report ?: return
        viewModelScope.launch {
            val rows = buildCsv(report)
            try {
                val file = container.reportExportAgent.exportCsv(rows)
                snackbarHostState.showSnackbar("CSV saved: ${file.path}")
            } catch (error: Exception) {
                val message = error.message?.takeIf { it.isNotBlank() } ?: "Export failed"
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    private fun buildCsv(report: ClassReport): List<CsvRow> {
        val header = CsvRow(listOf("Student", "Pre %", "Post %"))
        val data = report.attempts.map { attempt ->
            CsvRow(
                listOf(
                    attempt.student.displayName,
                    attempt.prePercent?.let { String.format(Locale.US, "%.1f", it) } ?: "",
                    attempt.postPercent?.let { String.format(Locale.US, "%.1f", it) } ?: ""
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
