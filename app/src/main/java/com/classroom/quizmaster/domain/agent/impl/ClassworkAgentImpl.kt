package com.classroom.quizmaster.domain.agent.impl

import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.data.local.ClassworkBundle
import com.classroom.quizmaster.data.local.SyncEntityType
import com.classroom.quizmaster.domain.agent.ClassworkAgent
import com.classroom.quizmaster.domain.model.Classwork
import com.classroom.quizmaster.domain.model.Submission
import java.util.UUID

class ClassworkAgentImpl(
    private val localData: BlueprintLocalDataSource
) : ClassworkAgent {

    override suspend fun createOrUpdate(classwork: Classwork): Result<Unit> = runCatching {
        val normalized = classwork.copy(
            title = classwork.title.trim(),
            topic = classwork.topic?.trim(),
            points = classwork.points?.coerceAtLeast(0)
        )
        localData.upsertClasswork(
            ClassworkBundle(
                item = normalized
            )
        )
        localData.enqueueSync(
            entityType = SyncEntityType.CLASSWORK,
            entityId = normalized.id,
            payload = normalized
        )
    }

    override suspend fun getAssignments(classId: String): List<Classwork> =
        localData.classworkFor(classId).map { it.item }

    override suspend fun submitAssignment(submission: Submission): Result<Unit> = runCatching {
        val normalized = submission.copy(
            id = submission.id.ifBlank { UUID.randomUUID().toString() }
        )
        localData.recordSubmission(normalized)
        localData.enqueueSync(
            entityType = SyncEntityType.SUBMISSION,
            entityId = normalized.id,
            payload = normalized
        )
    }
}
