package com.classroom.quizmaster.data.sync

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.LearningMaterial
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.Topic
import kotlinx.serialization.Serializable

@Serializable
data class UpsertClassroomPayload(val classroom: Classroom)

@Serializable
data class ArchiveClassroomPayload(val classroomId: String, val archivedAt: Long)

@Serializable
data class UpsertTopicPayload(val topic: Topic)

@Serializable
data class ArchiveTopicPayload(val topicId: String, val archivedAt: Long)

@Serializable
data class UpsertQuizPayload(val quiz: Quiz)

@Serializable
data class ArchiveQuizPayload(val quizId: String, val archivedAt: Long)

@Serializable
data class UpsertAssignmentPayload(val assignment: Assignment)

@Serializable
data class ArchiveAssignmentPayload(val assignmentId: String, val archivedAt: Long)

@Serializable
data class UpsertMaterialPayload(val material: LearningMaterial)

@Serializable
data class ArchiveMaterialPayload(val materialId: String, val archivedAt: Long)

@Serializable
data class DeleteMaterialPayload(val materialId: String)

object PendingOpTypes {
    const val CLASSROOM_UPSERT = "classroom_upsert"
    const val CLASSROOM_ARCHIVE = "classroom_archive"
    const val TOPIC_UPSERT = "topic_upsert"
    const val TOPIC_ARCHIVE = "topic_archive"
    const val QUIZ_UPSERT = "quiz_upsert"
    const val QUIZ_ARCHIVE = "quiz_archive"
    const val ASSIGNMENT_UPSERT = "assignment_upsert"
    const val ASSIGNMENT_ARCHIVE = "assignment_archive"
    const val MATERIAL_UPSERT = "material_upsert"
    const val MATERIAL_ARCHIVE = "material_archive"
    const val MATERIAL_DELETE = "material_delete"
}
