package com.classroom.quizmaster.agents.impl

import com.classroom.quizmaster.agents.ClassworkAgent
import com.classroom.quizmaster.data.model.Classwork
import com.classroom.quizmaster.data.model.Submission
import com.classroom.quizmaster.data.repo.ClassworkRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassworkAgentImpl @Inject constructor(
    private val classworkRepo: ClassworkRepo
) : ClassworkAgent {

    override suspend fun createOrUpdate(classwork: Classwork): Result<Unit> {
        require(classwork.classId.isNotEmpty()) { "Classwork requires classId" }
        return runCatching { classworkRepo.upsert(classwork) }
    }

    override suspend fun getAssignments(classId: String): List<Classwork> =
        classworkRepo.listOnce(classId)

    override suspend fun submitAssignment(submission: Submission): Result<Unit> {
        require(submission.classId.isNotEmpty()) { "Submission requires classId" }
        return runCatching { classworkRepo.submit(submission) }
    }
}
