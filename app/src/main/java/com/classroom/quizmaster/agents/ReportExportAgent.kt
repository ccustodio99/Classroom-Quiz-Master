package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.FileRef

interface ReportExportAgent {
    suspend fun exportClassSummaryPdf(classId: String): Result<FileRef>
    suspend fun exportLearningGainCsv(classId: String): Result<FileRef>
}
