package com.acme.lms.agents.impl

import com.acme.lms.agents.ClassworkAgent
import com.acme.lms.data.model.Classwork
import com.acme.lms.data.model.Submission
import com.acme.lms.data.repo.ClassworkRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassworkAgentImpl @Inject constructor(
    private val classworkRepo: ClassworkRepo
) : ClassworkAgent {

    override suspend fun createOrUpdate(classwork: Classwork): Result<Unit> =
        runCatching {
            require(classwork.classId.isNotEmpty()) { "Classwork requires classId" }
            classworkRepo.upsert(classwork)
        }

    override suspend fun getAssignments(classId: String): List<Classwork> =
        classworkRepo.listOnce(classId)

    override suspend fun submitAssignment(submission: Submission): Result<Unit> =
        runCatching {
            require(submission.classId.isNotEmpty()) { "Submission requires classId" }
            classworkRepo.submit(submission)
        }
}
