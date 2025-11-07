package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.remote.FirebaseAssignmentDataSource
import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val remote: FirebaseAssignmentDataSource
) : AssignmentRepository {

    override suspend fun createAssignment(assignment: Assignment) {
        remote.createAssignment(assignment)
    }

    override suspend fun submitHomework(submission: Submission) {
        remote.saveSubmission(submission)
    }
}
