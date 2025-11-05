package com.acme.lms.agents

import com.acme.lms.data.model.Classwork
import com.acme.lms.data.model.Submission

interface ClassworkAgent {
    suspend fun createOrUpdate(classwork: Classwork): Result<Unit>
    suspend fun getAssignments(classId: String): List<Classwork>
    suspend fun submitAssignment(submission: Submission): Result<Unit>
}
