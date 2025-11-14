package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Teacher
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FirebaseClassroomDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authDataSource: FirebaseAuthDataSource
) {

    private fun teachersCollection() = firestore.collection("teachers")

    private fun classroomsCollection() = firestore.collection("classrooms")

    suspend fun fetchTeacherProfile(): Result<Teacher?> = try {
        val uid = authDataSource.currentUserId() ?: return Result.success(null)
        val snapshot = teachersCollection().document(uid).get().await()
        Result.success(snapshot.toObject(FirestoreTeacher::class.java)?.toDomain(uid))
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun upsertTeacherProfile(teacher: Teacher): Result<Unit> = try {
        teachersCollection()
            .document(teacher.id)
            .set(FirestoreTeacher.fromDomain(teacher))
            .await()
        Result.success(Unit)
    } catch (error: Exception) {
        Timber.e(error, "Failed to upsert teacher profile")
        Result.failure(error)
    }

    suspend fun updateLastActive(): Result<Unit> = try {
        val uid = authDataSource.currentUserId() ?: return Result.success(Unit)
        teachersCollection()
            .document(uid)
            .update("lastActive", FieldValue.serverTimestamp())
            .await()
        Result.success(Unit)
    } catch (error: Exception) {
        Timber.w(error, "Failed to update teacher last active")
        Result.failure(error)
    }

    suspend fun fetchClassrooms(): Result<List<Classroom>> = try {
        val uid = authDataSource.currentUserId() ?: return Result.success(emptyList())
        val documents = classroomsCollection()
            .whereEqualTo("teacherId", uid)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(FirestoreClassroom::class.java)?.toDomain(doc.id)
            }
        Result.success(documents)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun upsertClassroom(classroom: Classroom): Result<String> = try {
        val document = if (classroom.id.isBlank()) {
            classroomsCollection().document()
        } else {
            classroomsCollection().document(classroom.id)
        }
        document.set(FirestoreClassroom.fromDomain(classroom)).await()
        Result.success(document.id)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun archiveClassroom(id: String, archivedAt: Instant): Result<Unit> = try {
        classroomsCollection()
            .document(id)
            .update(
                mapOf(
                    "isArchived" to true,
                    "archivedAt" to archivedAt.toEpochMilliseconds(),
                    "updatedAt" to archivedAt.toEpochMilliseconds()
                )
            )
            .await()
        Result.success(Unit)
    } catch (error: Exception) {
        Timber.e(error, "Failed to archive classroom $id")
        Result.failure(error)
    }

    private data class FirestoreTeacher(
        val displayName: String = "",
        val email: String = "",
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val lastActive: Long? = null
    ) {
        fun toDomain(id: String): Teacher = Teacher(
            id = id,
            displayName = displayName,
            email = email,
            createdAt = Instant.fromEpochMilliseconds(createdAt)
        )

        companion object {
            fun fromDomain(teacher: Teacher) = FirestoreTeacher(
                displayName = teacher.displayName,
                email = teacher.email,
                createdAt = teacher.createdAt.toEpochMilliseconds(),
                lastActive = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    private data class FirestoreClassroom(
        val teacherId: String = "",
        val name: String = "",
        val grade: String = "",
        val subject: String = "",
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val updatedAt: Long = createdAt,
        val isArchived: Boolean = false,
        val archivedAt: Long? = null
    ) {
        fun toDomain(id: String): Classroom = Classroom(
            id = id,
            teacherId = teacherId,
            name = name,
            grade = grade,
            subject = subject,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            isArchived = isArchived,
            archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
        )

        companion object {
            fun fromDomain(classroom: Classroom) = FirestoreClassroom(
                teacherId = classroom.teacherId,
                name = classroom.name,
                grade = classroom.grade,
                subject = classroom.subject,
                createdAt = classroom.createdAt.toEpochMilliseconds(),
                updatedAt = classroom.updatedAt.toEpochMilliseconds(),
                isArchived = classroom.isArchived,
                archivedAt = classroom.archivedAt?.toEpochMilliseconds()
            )
        }
    }
}
