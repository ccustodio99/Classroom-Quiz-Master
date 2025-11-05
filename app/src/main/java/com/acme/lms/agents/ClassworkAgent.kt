package com.acme.lms.agents

import com.example.lms.core.model.Classwork
import com.example.lms.core.model.Submission

interface ClassworkAgent {
    suspend fun createOrUpdate(classwork: Classwork): Result<Unit>
    suspend fun getAssignments(classId: String): List<Classwork>
    suspend fun submitAssignment(submission: Submission): Result<Unit>
}
