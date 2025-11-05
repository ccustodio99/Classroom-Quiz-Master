package com.acme.lms.data.repo

import com.example.lms.core.model.FileRef

interface ReportRepo {
    suspend fun exportLearningGainCsv(classId: String): FileRef
}
