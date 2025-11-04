package com.acme.lms.agents.impl

import com.acme.lms.agents.ReportExportAgent
import com.acme.lms.data.model.FileRef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExportAgentImpl @Inject constructor() : ReportExportAgent {

    override suspend fun exportClassSummaryPdf(classId: String): Result<FileRef> =
        Result.failure(UnsupportedOperationException("PDF export will be implemented when renderer is ready"))

    override suspend fun exportLearningGainCsv(classId: String): Result<FileRef> =
        Result.failure(UnsupportedOperationException("CSV export will be implemented when renderer is ready"))
}
