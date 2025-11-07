package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Submission

interface AssignmentRepository {
    suspend fun createAssignment(assignment: Assignment)
    suspend fun submitHomework(submission: Submission)
}
