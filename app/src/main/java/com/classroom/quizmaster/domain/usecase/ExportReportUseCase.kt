package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.data.remote.FirebaseFunctionsDataSource
import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class ExportReportUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val functionsDataSource: FirebaseFunctionsDataSource
) {
    data class ReportLinks(
        val csvUrl: String,
        val pdfUrl: String
    )

    suspend operator fun invoke(sessionId: String? = null): Result<ReportLinks> = runCatching {
        sessionRepository.syncPending()
        val pending = sessionRepository.pendingOpCount.first()
        require(pending == 0) { "Pending attempts must sync before exporting" }
        val resolvedSessionId = sessionId
            ?: sessionRepository.session.firstOrNull()?.id
            ?: throw IllegalStateException("No active session to export")
        val response = functionsDataSource
            .call("exportReport", mapOf("sessionId" to resolvedSessionId))
            .getOrElse { throw it }
        val csvUrl = response["csvUrl"] as? String
        val pdfUrl = response["pdfUrl"] as? String
        if (csvUrl.isNullOrBlank() || pdfUrl.isNullOrBlank()) {
            throw IllegalStateException("Incomplete export payload")
        }
        ReportLinks(csvUrl = csvUrl, pdfUrl = pdfUrl)
    }
}
