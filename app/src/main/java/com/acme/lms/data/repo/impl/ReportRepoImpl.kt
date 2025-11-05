package com.acme.lms.data.repo.impl

import com.acme.lms.data.model.FileRef
import com.acme.lms.data.repo.ReportRepo
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject

class ReportRepoImpl @Inject constructor() : ReportRepo {
    override suspend fun exportLearningGainCsv(classId: String): FileRef {
        // Simulate CSV generation
        delay(1000)
        return FileRef(UUID.randomUUID().toString(), "learning_gain.csv")
    }
}
