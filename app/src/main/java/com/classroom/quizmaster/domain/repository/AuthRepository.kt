package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String, displayName: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun signInAnonymously(nickname: String)
    suspend fun logout()
    suspend fun sendPasswordReset(email: String)
    suspend fun updateDisplayName(displayName: String)
    suspend fun lookupEmailForUsername(username: String): String?
    suspend fun upsertTeacherProfile(teacher: Teacher)
    suspend fun upsertStudentProfile(student: Student)
    suspend fun getTeacher(teacherId: String): Flow<Teacher?>
}
