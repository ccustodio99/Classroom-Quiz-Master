package com.acme.lms.agents

import com.acme.lms.data.model.FileRef

interface ReportExportAgent {
    suspend fun exportClassSummaryPdf(classId: String): Result<FileRef>
    suspend fun exportLearningGainCsv(classId: String): Result<FileRef>
}
