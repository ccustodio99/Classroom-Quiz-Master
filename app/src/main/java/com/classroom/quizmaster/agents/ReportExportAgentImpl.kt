package com.classroom.quizmaster.agents

import android.content.Context

class ReportExportAgentImpl(private val context: Context) : ReportExportAgent {
    override suspend fun exportClassSummaryPdf(classId: String): Result<FileRef> {
        TODO("Not yet implemented")
    }

    override suspend fun exportLearningGainCsv(classId: String): Result<FileRef> {
        TODO("Not yet implemented")
    }
}
