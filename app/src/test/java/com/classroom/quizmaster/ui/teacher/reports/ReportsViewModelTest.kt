package com.classroom.quizmaster.ui.teacher.reports

import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.usecase.ExportReportUseCase
import com.classroom.quizmaster.testing.MainDispatcherRule
import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsUiState
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `export success updates links and emits open event`() = runTest {
        val reportsFlow = MutableStateFlow(
            ReportsUiState(
                isLoading = false,
                classroomName = "Class",
                lastUpdatedLabel = "just now"
            )
        )
        val assignmentRepoUi = FakeAssignmentUiRepo(reportsFlow)
        val assignmentRepo = mockk<AssignmentRepository>(relaxed = true)
        val classroomRepo = mockk<ClassroomRepository>(relaxed = true)
        val quizRepo = mockk<QuizRepository>(relaxed = true)
        val exportUseCase = mockk<ExportReportUseCase>()
        coEvery { exportUseCase.invoke(any()) } returns Result.success(
            ExportReportUseCase.ReportLinks(csvUrl = "csv-url", pdfUrl = "pdf-url")
        )

        val viewModel = ReportsViewModel(assignmentRepoUi, assignmentRepo, classroomRepo, quizRepo, exportUseCase)
        advanceUntilIdle()

        val events = mutableListOf<ReportsEvent>()
        val job = launch { viewModel.events.take(1).toList(events) }

        viewModel.exportCsv()
        advanceUntilIdle()
        job.cancel()

        val state = viewModel.uiState.value
        assertThat(state.exportState.lastCsvUrl).isEqualTo("csv-url")
        assertThat(state.exportState.isExporting).isFalse()
        val snackbar = events.filterIsInstance<ReportsEvent.Snackbar>().single()
        assertThat(snackbar.action).isInstanceOf(SnackbarAction.OpenLink::class.java)
        val action = snackbar.action as SnackbarAction.OpenLink
        assertThat(action.url).isEqualTo("csv-url")
    }

    @Test
    fun `export failure surfaces error message`() = runTest {
        val reportsFlow = MutableStateFlow(ReportsUiState(isLoading = false))
        val assignmentRepoUi = FakeAssignmentUiRepo(reportsFlow)
        val assignmentRepo = mockk<AssignmentRepository>(relaxed = true)
        val classroomRepo = mockk<ClassroomRepository>(relaxed = true)
        val quizRepo = mockk<QuizRepository>(relaxed = true)
        val exportUseCase = mockk<ExportReportUseCase>()
        coEvery { exportUseCase.invoke(any()) } returns Result.failure(IllegalStateException("boom"))

        val viewModel = ReportsViewModel(assignmentRepoUi, assignmentRepo, classroomRepo, quizRepo, exportUseCase)
        advanceUntilIdle()

        val events = mutableListOf<ReportsEvent>()
        val job = launch { viewModel.events.take(1).toList(events) }

        viewModel.exportPdf()
        advanceUntilIdle()
        job.cancel()

        val state = viewModel.uiState.value
        assertThat(state.exportState.lastExportError).contains("boom")
        val snackbar = events.filterIsInstance<ReportsEvent.Snackbar>().single()
        assertThat(snackbar.message).contains("boom")
    }
}

private class FakeAssignmentUiRepo(
    private val reportsFlow: MutableStateFlow<ReportsUiState>
) : AssignmentRepositoryUi {
    override val assignments: Flow<AssignmentsUiState> = MutableStateFlow(AssignmentsUiState())
    override val reports: Flow<ReportsUiState> = reportsFlow
    override fun selectReportsClassroom(classroomId: String?) {}
}
