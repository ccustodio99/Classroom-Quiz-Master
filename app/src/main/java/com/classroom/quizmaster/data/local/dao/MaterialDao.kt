package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.classroom.quizmaster.data.local.entity.LearningMaterialEntity
import com.classroom.quizmaster.data.local.entity.MaterialAttachmentEntity
import com.classroom.quizmaster.data.local.entity.MaterialWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {

    @Transaction
    @Query(
        "SELECT * FROM learning_materials WHERE teacherId = :teacherId AND isArchived = 0 ORDER BY updatedAt DESC"
    )
    fun observeActiveForTeacher(teacherId: String): Flow<List<MaterialWithAttachments>>

    @Transaction
    @Query(
        "SELECT * FROM learning_materials WHERE teacherId = :teacherId AND isArchived = 1 ORDER BY archivedAt DESC, updatedAt DESC"
    )
    fun observeArchivedForTeacher(teacherId: String): Flow<List<MaterialWithAttachments>>

    @Transaction
    @Query(
        "SELECT * FROM learning_materials WHERE classroomId = :classroomId AND isArchived = 0 ORDER BY updatedAt DESC"
    )
    fun observeActiveForClassroom(classroomId: String): Flow<List<MaterialWithAttachments>>

    @Transaction
    @Query("SELECT * FROM learning_materials WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeAllActive(): Flow<List<MaterialWithAttachments>>

    @Transaction
    @Query("SELECT * FROM learning_materials WHERE id = :materialId LIMIT 1")
    fun observeMaterial(materialId: String): Flow<MaterialWithAttachments?>

    @Transaction
    @Query("SELECT * FROM learning_materials WHERE id = :materialId LIMIT 1")
    suspend fun getMaterial(materialId: String): MaterialWithAttachments?

    @Transaction
    @Query("SELECT * FROM learning_materials WHERE classroomId = :classroomId")
    suspend fun listForClassroom(classroomId: String): List<MaterialWithAttachments>

    @Transaction
    @Query("SELECT * FROM learning_materials WHERE classroomId = :classroomId AND isArchived = 0")
    suspend fun listActiveForClassroom(classroomId: String): List<MaterialWithAttachments>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterial(material: LearningMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(attachments: List<MaterialAttachmentEntity>)

    @Query("DELETE FROM material_attachments WHERE materialId = :materialId")
    suspend fun clearAttachments(materialId: String)

    @Query(
        "DELETE FROM material_attachments WHERE materialId = :materialId AND id NOT IN (:keepIds)"
    )
    suspend fun pruneAttachments(materialId: String, keepIds: List<String>)

    @Query("DELETE FROM material_attachments WHERE materialId IN (SELECT id FROM learning_materials WHERE classroomId = :classroomId)")
    suspend fun deleteAttachmentsForClassroom(classroomId: String)

    @Query("DELETE FROM learning_materials WHERE id = :materialId")
    suspend fun deleteMaterial(materialId: String)

    @Query("DELETE FROM learning_materials WHERE classroomId = :classroomId")
    suspend fun deleteMaterialsForClassroom(classroomId: String)
}
