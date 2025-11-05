package com.acme.lms.agents

import com.example.lms.core.model.FileRef

interface ReportExportAgent {
    suspend fun exportClassSummaryPdf(classId: String): Result<FileRef>
    suspend fun exportLearningGainCsv(classId: String): Result<FileRef>
}
