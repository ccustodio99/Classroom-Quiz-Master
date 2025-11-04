package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.Classwork
import com.classroom.quizmaster.domain.model.Submission

interface ClassworkAgent {
  suspend fun createOrUpdate(classwork: Classwork): Result<Unit>
  suspend fun getAssignments(classId: String): List<Classwork>
  suspend fun submitAssignment(submission: Submission): Result<Unit>
}
