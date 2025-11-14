package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface ClassroomRepository {
    val classrooms: Flow<List<Classroom>>
    val topics: Flow<List<Topic>>
    val archivedClassrooms: Flow<List<Classroom>>
    val archivedTopics: Flow<List<Topic>>

    suspend fun refresh()
    suspend fun upsertClassroom(classroom: Classroom): String
    suspend fun archiveClassroom(classroomId: String, archivedAt: Instant = Clock.System.now())
    suspend fun upsertTopic(topic: Topic): String
    suspend fun archiveTopic(topicId: String, archivedAt: Instant = Clock.System.now())
    suspend fun getClassroom(id: String): Classroom?
    suspend fun getTopic(id: String): Topic?
}
