package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.JoinRequest
import com.classroom.quizmaster.domain.model.JoinRequestStatus
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
    private fun studentsCollection() = firestore.collection("students")
    private fun classroomsCollection() = firestore.collection("classrooms")
    private fun joinRequestsCollection() = firestore.collection("joinRequests")
    private fun usersCollection() = firestore.collection("users")

    suspend fun fetchTeacherProfile(): Result<Teacher?> = try {
        val uid = authDataSource.currentUserId() ?: return Result.success(null)
        val snapshot = teachersCollection().document(uid).get().await()
        Result.success(snapshot.toObject(FirestoreTeacher::class.java)?.toDomain(uid))
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun searchTeachers(query: String): Result<List<Teacher>> = try {
        val snapshot = teachersCollection()
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
            .limit(20)
            .get()
            .await()

        val teachers = snapshot.documents.mapNotNull { doc ->
            doc.toObject(FirestoreTeacher::class.java)?.toDomain(doc.id)
        }
        Result.success(teachers)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun getClassroomsForTeacher(teacherId: String): Result<List<Classroom>> = try {
        // Support both legacy "archived" and current "isArchived" flags.
        val snapshots = listOf(
            classroomsCollection()
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("isArchived", false)
                .get()
                .await(),
            classroomsCollection()
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("archived", false)
                .get()
                .await()
        )
        val classrooms = snapshots
            .flatMap { it.documents }
            .mapNotNull { doc -> doc.toObject(FirestoreClassroom::class.java)?.toDomain(doc.id) }
            .distinctBy { it.id }
        Result.success(classrooms)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun getClassroomsForStudent(studentId: String): Result<List<Classroom>> = try {
        // Support both legacy "archived" and current "isArchived" flags.
        val snapshots = listOf(
            classroomsCollection()
                .whereArrayContains("students", studentId)
                .whereEqualTo("isArchived", false)
                .get()
                .await(),
            classroomsCollection()
                .whereArrayContains("students", studentId)
                .whereEqualTo("archived", false)
                .get()
                .await()
        )
        val classrooms = snapshots
            .flatMap { it.documents }
            .mapNotNull { doc -> doc.toObject(FirestoreClassroom::class.java)?.toDomain(doc.id) }
            .distinctBy { it.id }
        Result.success(classrooms)
    } catch (error: Exception) {
        Result.failure(error)
    }


    suspend fun fetchStudentProfile(studentId: String): Result<Student?> = try {
        val snapshot = studentsCollection().document(studentId).get().await()
        Result.success(snapshot.toObject(FirestoreStudent::class.java)?.toDomain(studentId))
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

    suspend fun upsertStudentProfile(student: Student): Result<Unit> = try {
        studentsCollection()
            .document(student.id)
            .set(FirestoreStudent.fromDomain(student))
            .await()
        Result.success(Unit)
    } catch (error: Exception) {
        Timber.e(error, "Failed to upsert student profile")
        Result.failure(error)
    }

    suspend fun findTeacherEmailByUsername(username: String): Result<String?> = try {
        val snapshot = teachersCollection()
            .whereEqualTo("displayName", username)
            .limit(1)
            .get()
            .await()
        val email = snapshot.documents.firstOrNull()
            ?.toObject(FirestoreTeacher::class.java)
            ?.email
        Result.success(email)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun findStudentByIdentifier(identifier: String): Result<Student?> = try {
        val query = if (identifier.contains("@")) {
            studentsCollection().whereEqualTo("email", identifier.trim())
        } else {
            studentsCollection().whereEqualTo("displayName", identifier.trim())
        }
        val snapshot = query.limit(1).get().await()
        val first = snapshot.documents.firstOrNull()
        val student = first
            ?.toObject(FirestoreStudent::class.java)
            ?.toDomain(first.id)
        Result.success(student)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun addStudentToClassroom(classroomId: String, studentId: String): Result<Unit> = try {
        val classroomRef = classroomsCollection().document(classroomId)
        firestore.runTransaction { tx ->
            tx.update(classroomRef, "students", FieldValue.arrayUnion(studentId))
        }.await()
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun removeStudentFromClassroom(classroomId: String, studentId: String): Result<Unit> = try {
        val classroomRef = classroomsCollection().document(classroomId)
        firestore.runTransaction { tx ->
            tx.update(classroomRef, "students", FieldValue.arrayRemove(studentId))
        }.await()
        Result.success(Unit)
    } catch (error: Exception) {
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

    suspend fun getClassroomByJoinCode(joinCode: String): Result<Classroom> = try {
        val snapshot = classroomsCollection()
            .whereEqualTo("joinCode", joinCode)
            .limit(1)
            .get()
            .await()

        if (snapshot.isEmpty) {
            Result.failure(Exception("Classroom not found"))
        } else {
            val doc = snapshot.documents[0]
            Result.success(doc.toObject(FirestoreClassroom::class.java)!!.toDomain(doc.id))
        }
    } catch (e: Exception) {
        Result.failure(e)
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

    suspend fun createJoinRequest(joinRequest: JoinRequest): Result<String> = try {
        val document = joinRequestsCollection().document()
        document.set(FirestoreJoinRequest.fromDomain(joinRequest)).await()
        Result.success(document.id)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun fetchJoinRequestsForTeacher(teacherId: String): Result<List<JoinRequest>> = try {
        val snapshot = joinRequestsCollection()
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        val requests = snapshot.documents.mapNotNull { doc ->
            doc.toObject(FirestoreJoinRequest::class.java)?.toDomain(doc.id)
        }
        Result.success(requests)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun approveJoinRequest(requestId: String): Result<Unit> = try {
        val requestRef = joinRequestsCollection().document(requestId)
        firestore.runTransaction {
            val request = it.get(requestRef).toObject(FirestoreJoinRequest::class.java)!!
            val classroomRef = classroomsCollection().document(request.classroomId)
            it.update(classroomRef, "students", FieldValue.arrayUnion(request.studentId))
            it.update(requestRef, "status", JoinRequestStatus.APPROVED.name)
            it.update(requestRef, "resolvedAt", FieldValue.serverTimestamp())
        }.await()
        Result.success(Unit)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun denyJoinRequest(requestId: String): Result<Unit> = try {
        joinRequestsCollection()
            .document(requestId)
            .update(
                mapOf(
                    "status" to JoinRequestStatus.DENIED.name,
                    "resolvedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
        Result.success(Unit)
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

    private data class FirestoreStudent(
        val displayName: String = "",
        val email: String = "",
        val createdAt: Long = Clock.System.now().toEpochMilliseconds()
    ) {
        fun toDomain(id: String): Student = Student(
            id = id,
            displayName = displayName,
            email = email,
            createdAt = Instant.fromEpochMilliseconds(createdAt)
        )

        companion object {
            fun fromDomain(student: Student) = FirestoreStudent(
                displayName = student.displayName,
                email = student.email,
                createdAt = student.createdAt.toEpochMilliseconds()
            )
        }
    }

    private data class FirestoreClassroom(
        val teacherId: String = "",
        val name: String = "",
        val grade: String = "",
        val subject: String = "",
        val joinCode: String = "",
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val updatedAt: Long = createdAt,
        val isArchived: Boolean = false,
        // Legacy field name kept for backward compatibility with old documents.
        val archived: Boolean? = null,
        val archivedAt: Long? = null,
        val students: List<String> = emptyList()
    ) {
        fun toDomain(id: String): Classroom = Classroom(
            id = id,
            teacherId = teacherId,
            name = name,
            grade = grade,
            subject = subject,
            joinCode = joinCode,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            isArchived = isArchived || (archived ?: false),
            archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds),
            students = students
        )

        companion object {
            fun fromDomain(classroom: Classroom) = FirestoreClassroom(
                teacherId = classroom.teacherId,
                name = classroom.name,
                grade = classroom.grade,
                subject = classroom.subject,
                joinCode = classroom.joinCode,
                createdAt = classroom.createdAt.toEpochMilliseconds(),
                updatedAt = classroom.updatedAt.toEpochMilliseconds(),
                isArchived = classroom.isArchived,
                archivedAt = classroom.archivedAt?.toEpochMilliseconds(),
                students = classroom.students
            )
        }
    }

    private data class FirestoreJoinRequest(
        val studentId: String = "",
        val classroomId: String = "",
        val teacherId: String = "",
        val status: String = JoinRequestStatus.PENDING.name,
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val resolvedAt: Long? = null
    ) {
        fun toDomain(id: String): JoinRequest = JoinRequest(
            id = id,
            studentId = studentId,
            classroomId = classroomId,
            teacherId = teacherId,
            status = JoinRequestStatus.valueOf(status),
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            resolvedAt = resolvedAt?.let(Instant::fromEpochMilliseconds)
        )

        companion object {
            fun fromDomain(joinRequest: JoinRequest) = FirestoreJoinRequest(
                studentId = joinRequest.studentId,
                classroomId = joinRequest.classroomId,
                teacherId = joinRequest.teacherId,
                status = joinRequest.status.name,
                createdAt = joinRequest.createdAt.toEpochMilliseconds(),
                resolvedAt = joinRequest.resolvedAt?.toEpochMilliseconds()
            )
        }
    }
}
