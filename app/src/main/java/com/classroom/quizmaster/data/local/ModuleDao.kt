package com.classroom.quizmaster.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules WHERE archived = 0 ORDER BY created_at DESC")
    fun observeActiveModules(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules ORDER BY created_at DESC")
    fun observeAllModules(): Flow<List<ModuleEntity>>

    @Query(
        "SELECT * FROM modules WHERE classroom_id = :classroomId AND (:includeArchived OR archived = 0) " +
            "ORDER BY created_at DESC"
    )
    fun observeByClassroom(classroomId: String, includeArchived: Boolean): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE archived = 0")
    suspend fun getActive(): List<ModuleEntity>

    @Query("SELECT * FROM modules")
    suspend fun getAll(): List<ModuleEntity>

    @Query("SELECT * FROM modules WHERE classroom_id = :classroomId AND (:includeArchived OR archived = 0)")
    suspend fun getByClassroom(classroomId: String, includeArchived: Boolean): List<ModuleEntity>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getModule(id: String): ModuleEntity?

    @Query(
        "SELECT * FROM modules WHERE pre_assessment_id = :assessmentId OR post_assessment_id = :assessmentId LIMIT 1"
    )
    suspend fun getByAssessmentId(assessmentId: String): ModuleEntity?

    @Query("SELECT * FROM modules WHERE lesson_id = :lessonId LIMIT 1")
    suspend fun getByLessonId(lessonId: String): ModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ModuleEntity)

    @Update
    suspend fun update(entity: ModuleEntity)

    @Query("UPDATE modules SET archived = :archived, updated_at = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)
}
