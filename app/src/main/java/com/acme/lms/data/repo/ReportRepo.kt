package com.acme.lms.data.repo

import com.acme.lms.data.model.FileRef

interface ReportRepo {
    suspend fun exportLearningGainCsv(classId: String): FileRef
}
