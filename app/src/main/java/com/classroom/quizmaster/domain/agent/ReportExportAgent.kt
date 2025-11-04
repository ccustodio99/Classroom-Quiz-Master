package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.FileRef

interface ReportExportAgent {
  suspend fun exportClassSummaryPdf(classId: String): Result<FileRef>
  suspend fun exportLearningGainCsv(classId: String): Result<FileRef>
}
