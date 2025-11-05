package com.acme.lms.agents.impl

import com.acme.lms.agents.ReportExportAgent
import com.acme.lms.data.model.FileRef
import com.acme.lms.data.repo.ReportRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExportAgentImpl @Inject constructor(
    private val reportRepo: ReportRepo
) : ReportExportAgent {

    // TODO: Implement PDF export when renderer is ready
    override suspend fun exportClassSummaryPdf(classId: String): Result<FileRef> =
        Result.failure(UnsupportedOperationException("PDF export will be implemented when renderer is ready"))

    // TODO: Implement CSV export with real data
    override suspend fun exportLearningGainCsv(classId: String): Result<FileRef> =
        runCatching { reportRepo.exportLearningGainCsv(classId) }
}
