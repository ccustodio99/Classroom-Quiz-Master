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
        val pdfUrl: String,
        val exportVersion: String? = null,
        val exportedAt: String? = null
    )

    suspend operator fun invoke(sessionId: String? = null, assignmentId: String? = null): Result<ReportLinks> =
        runCatching {
            sessionRepository.syncPending()
            val pending = sessionRepository.pendingOpCount.first()
            if (pending > 0) {
                throw IllegalStateException("Sync in progress. Please wait before exporting.")
            }
            val resolvedSessionId = sessionId
                ?: sessionRepository.session.firstOrNull()?.id
            if (resolvedSessionId.isNullOrBlank() && assignmentId.isNullOrBlank()) {
                throw IllegalStateException("No session or assignment available to export.")
            }
            val payload = mutableMapOf<String, Any?>()
            resolvedSessionId?.let { payload["sessionId"] = it }
            assignmentId?.let { payload["assignmentId"] = it }
            val response = functionsDataSource
                .call("exportReport", payload)
                .getOrElse { throw it }
            val csvUrl = response["csvUrl"] as? String
            val pdfUrl = response["pdfUrl"] as? String
            val exportVersion = response["exportVersion"] as? String
            val exportedAt = response["exportedAt"] as? String
            if (csvUrl.isNullOrBlank() || pdfUrl.isNullOrBlank()) {
                throw IllegalStateException("Export failed to return download links.")
            }
            ReportLinks(
                csvUrl = csvUrl,
                pdfUrl = pdfUrl,
                exportVersion = exportVersion,
                exportedAt = exportedAt
            )
        }.recoverCatching { error ->
            when {
                error.message?.contains("NO_DATA", ignoreCase = true) == true -> {
                    throw IllegalStateException("Exports will be available once there are submissions.")
                }
                error.message?.contains("permission", ignoreCase = true) == true -> {
                    throw IllegalStateException("You need teacher access to export this report.")
                }
                error.message?.contains("sync", ignoreCase = true) == true -> {
                    throw IllegalStateException("Please wait for sync to complete before exporting.")
                }
                else -> throw error
            }
        }
}
