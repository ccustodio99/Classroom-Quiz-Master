package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.Classwork
import com.classroom.quizmaster.data.model.Submission

interface ClassworkAgent {
    suspend fun createOrUpdate(classwork: Classwork): Result<Unit>
    suspend fun getAssignments(classId: String): List<Classwork>
    suspend fun submitAssignment(submission: Submission): Result<Unit>
}
