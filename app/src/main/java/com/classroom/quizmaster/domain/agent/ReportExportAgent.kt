package com.classroom.quizmaster.domain.agent

interface ReportExportAgent {
  suspend fun exportClassSummaryPdf(classId: String): Result<FileRef>
  suspend fun exportLearningGainCsv(classId: String): Result<FileRef>
}
