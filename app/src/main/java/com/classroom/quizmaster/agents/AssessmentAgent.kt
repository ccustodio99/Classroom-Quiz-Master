package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.Attempt
import com.classroom.quizmaster.data.model.Submission

interface AssessmentAgent {
    suspend fun start(classId: String, classworkId: String, userId: String): Attempt
    suspend fun submit(attempt: Attempt): Result<Submission>
}
