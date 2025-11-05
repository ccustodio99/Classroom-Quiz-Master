package com.acme.lms.agents

import com.example.lms.core.model.Attempt
import com.example.lms.core.model.Submission

interface AssessmentAgent {
    suspend fun start(classId: String, classworkId: String, userId: String): Attempt
    suspend fun submit(attempt: Attempt): Result<Submission>
}
