package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.LearningMaterial
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface LearningMaterialRepository {
    fun observeTeacherMaterials(
        classroomId: String? = null,
        topicId: String? = null,
        includeArchived: Boolean = false
    ): Flow<List<LearningMaterial>>

    fun observeStudentMaterials(
        classroomId: String? = null,
        topicId: String? = null
    ): Flow<List<LearningMaterial>>

    fun observeMaterial(materialId: String): Flow<LearningMaterial?>

    suspend fun get(materialId: String): LearningMaterial?

    suspend fun upsert(material: LearningMaterial): String

    suspend fun archive(materialId: String, archivedAt: Instant = Clock.System.now())

    suspend fun delete(materialId: String)

    suspend fun shareSnapshotForClassroom(classroomId: String)

    suspend fun importSnapshot(classroomId: String, materials: List<LearningMaterial>)
}
