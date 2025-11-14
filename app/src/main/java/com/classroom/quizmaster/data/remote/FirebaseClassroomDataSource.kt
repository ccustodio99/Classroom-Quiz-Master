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

    suspend fun fetchTeacherProfile(): Result<Teacher?> = runCatching {
        val uid = authDataSource.currentUserId() ?: return@runCatching null
        val snapshot = teachersCollection().document(uid).get().await()
        snapshot.toObject(FirestoreTeacher::class.java)?.toDomain(uid)
    }

    suspend fun upsertTeacherProfile(teacher: Teacher): Result<Unit> = runCatching {
        teachersCollection()
            .document(teacher.id)
            .set(FirestoreTeacher.fromDomain(teacher))
            .await()
        Unit
    }.onFailure { Timber.e(it, "Failed to upsert teacher profile") }

    suspend fun updateLastActive(): Result<Unit> = runCatching {
        val uid = authDataSource.currentUserId() ?: return@runCatching
        teachersCollection()
            .document(uid)
            .update("lastActive", FieldValue.serverTimestamp())
            .await()
        Unit
    }.onFailure { Timber.w(it, "Failed to update teacher last active") }

    suspend fun fetchClassrooms(): Result<List<Classroom>> = runCatching {
        val uid = authDataSource.currentUserId() ?: return@runCatching emptyList()
        classroomsCollection()
            .whereEqualTo("teacherId", uid)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(FirestoreClassroom::class.java)?.toDomain(doc.id)
            }
    }

    suspend fun upsertClassroom(classroom: Classroom): Result<String> = runCatching {
        val document = if (classroom.id.isBlank()) {
            classroomsCollection().document()
        } else {
            classroomsCollection().document(classroom.id)
        }
        document.set(FirestoreClassroom.fromDomain(classroom)).await()
        document.id
    }

    suspend fun archiveClassroom(id: String, archivedAt: Instant): Result<Unit> = runCatching {
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
        Unit
    }.onFailure { Timber.e(it, "Failed to archive classroom $id") }

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
