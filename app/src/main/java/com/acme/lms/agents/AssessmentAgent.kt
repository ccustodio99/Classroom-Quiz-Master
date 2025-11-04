package com.acme.lms.agents

import com.acme.lms.data.model.Attempt
import com.acme.lms.data.model.Submission

interface AssessmentAgent {
    suspend fun start(classworkId: String, userId: String): Attempt
    suspend fun submit(attempt: Attempt): Result<Submission>
}
