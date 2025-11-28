package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.data.remote.FirebaseFunctionsDataSource
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExportReportUseCaseTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `returns links when export succeeds`() = runTest {
        val sessionRepo = mockk<SessionRepository>()
        coEvery { sessionRepo.syncPending() } returns Unit
        every { sessionRepo.pendingOpCount } returns flowOf(0)
        every { sessionRepo.session } returns flowOf(null)
        val functions = mockk<FirebaseFunctionsDataSource>()
        coEvery { functions.call("exportReport", any()) } returns Result.success(
            mapOf("csvUrl" to "csv", "pdfUrl" to "pdf", "exportVersion" to "v2", "exportedAt" to "now")
        )

        val useCase = ExportReportUseCase(sessionRepo, functions)
        val result = useCase(sessionId = "s1")

        assertTrue(result.isSuccess)
        val links = result.getOrThrow()
        assertTrue(links.csvUrl == "csv" && links.pdfUrl == "pdf" && links.exportVersion == "v2")
    }

    @Test
    fun `maps no data error to friendly message`() = runTest {
        val sessionRepo = mockk<SessionRepository>()
        coEvery { sessionRepo.syncPending() } returns Unit
        every { sessionRepo.pendingOpCount } returns flowOf(0)
        every { sessionRepo.session } returns flowOf(null)
        val functions = mockk<FirebaseFunctionsDataSource>()
        coEvery { functions.call("exportReport", any()) } returns Result.failure(IllegalStateException("NO_DATA"))

        val useCase = ExportReportUseCase(sessionRepo, functions)
        val result = useCase(sessionId = "s1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("submissions", ignoreCase = true) == true)
    }
}
