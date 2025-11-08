package com.classroom.quizmaster.ui.state

import com.classroom.quizmaster.ui.student.end.StudentEndUiState
import com.classroom.quizmaster.ui.student.entry.StudentEntryUiState
import com.classroom.quizmaster.ui.student.lobby.StudentLobbyUiState
import com.classroom.quizmaster.ui.student.play.StudentPlayUiState
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsUiState
import com.classroom.quizmaster.ui.teacher.home.TeacherHomeUiState
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyUiState
import com.classroom.quizmaster.ui.teacher.reports.ReportsUiState
import com.classroom.quizmaster.ui.teacher.host.HostLiveUiState
import com.classroom.quizmaster.ui.teacher.quiz_editor.QuizEditorUiState
import kotlinx.coroutines.flow.Flow

interface QuizRepositoryUi {
    val teacherHome: Flow<TeacherHomeUiState>
    fun quizEditorState(quizId: String?): Flow<QuizEditorUiState>
    suspend fun persistDraft(state: QuizEditorUiState)
}

interface SessionRepositoryUi {
    val launchLobby: Flow<LaunchLobbyUiState>
    val hostState: Flow<HostLiveUiState>
    val studentEntry: Flow<StudentEntryUiState>
    val studentLobby: Flow<StudentLobbyUiState>
    val studentPlay: Flow<StudentPlayUiState>
    val studentEnd: Flow<StudentEndUiState>

    suspend fun updateLeaderboardHidden(hidden: Boolean)
    suspend fun updateLockAfterFirst(lock: Boolean)
    suspend fun updateMuteSfx(muted: Boolean)
    suspend fun startSession()
    suspend fun endSession()
    suspend fun revealAnswer()
    suspend fun nextQuestion()
    suspend fun kickParticipant(uid: String)
    suspend fun toggleReady(studentId: String)
}

interface AssignmentRepositoryUi {
    val assignments: Flow<AssignmentsUiState>
    val reports: Flow<ReportsUiState>
}
