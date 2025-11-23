package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.model.JoinRequest
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.Teacher
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface ClassroomRepository {
    val classrooms: Flow<List<Classroom>>
    val topics: Flow<List<Topic>>
    val archivedClassrooms: Flow<List<Classroom>>
    val archivedTopics: Flow<List<Topic>>
    val joinRequests: Flow<List<JoinRequest>>
    val students: Flow<List<Student>>

    suspend fun refresh()
    suspend fun upsertClassroom(classroom: Classroom): String
    suspend fun archiveClassroom(classroomId: String, archivedAt: Instant = Clock.System.now())
    suspend fun upsertTopic(topic: Topic): String
    suspend fun archiveTopic(topicId: String, archivedAt: Instant = Clock.System.now())
    suspend fun createJoinRequest(joinCode: String)
    suspend fun createJoinRequest(classroomId: String, teacherId: String)
    suspend fun approveJoinRequest(requestId: String)
    suspend fun denyJoinRequest(requestId: String)
    suspend fun addStudentByEmailOrUsername(classroomId: String, identifier: String)
    suspend fun removeStudentFromClassroom(classroomId: String, studentId: String)
    suspend fun getStudent(id: String): Student?
    suspend fun searchTeachers(query: String): List<Teacher>
    suspend fun getClassroomsForTeacher(teacherId: String): List<Classroom>
    suspend fun getClassroom(id: String): Classroom?
    suspend fun getTopic(id: String): Topic?
}
