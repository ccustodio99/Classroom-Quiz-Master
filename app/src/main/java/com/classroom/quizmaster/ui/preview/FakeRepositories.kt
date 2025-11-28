package com.classroom.quizmaster.ui.preview

import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import com.classroom.quizmaster.ui.state.QuizRepositoryUi
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
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
import kotlinx.coroutines.flow.flowOf

class FakeQuizRepositoryUi : QuizRepositoryUi {
    override val teacherHome: Flow<TeacherHomeUiState> = flowOf(TeacherHomeUiState())
    override fun quizEditorState(
        classroomId: String,
        topicId: String,
        quizId: String?
    ): Flow<QuizEditorUiState> = flowOf(QuizEditorUiState())
    override suspend fun persistDraft(state: QuizEditorUiState) = Unit
}

class FakeSessionRepositoryUi : SessionRepositoryUi {
    override val launchLobby: Flow<LaunchLobbyUiState> = flowOf(LaunchLobbyUiState())
    override val hostState: Flow<HostLiveUiState> = flowOf(HostLiveUiState())
    override val studentEntry: Flow<StudentEntryUiState> = flowOf(StudentEntryUiState())
    override val studentLobby: Flow<StudentLobbyUiState> = flowOf(StudentLobbyUiState())
    override val studentPlay: Flow<StudentPlayUiState> = flowOf(StudentPlayUiState())
    override val studentEnd: Flow<StudentEndUiState> = flowOf(StudentEndUiState())
    override val avatarOptions: Flow<List<AvatarOption>> = flowOf(emptyList())

    override suspend fun configureHostContext(classroomId: String, topicId: String?, quizId: String?) = Unit
    override suspend fun updateLeaderboardHidden(hidden: Boolean) = Unit
    override suspend fun updateLockAfterFirst(lock: Boolean) = Unit
    override suspend fun updateMuteSfx(muted: Boolean) = Unit
    override suspend fun startSession() = Unit
    override suspend fun endSession() = Unit
    override suspend fun revealAnswer() = Unit
    override suspend fun nextQuestion() = Unit
    override suspend fun kickParticipant(uid: String) = Unit
    override suspend fun toggleReady(studentId: String) = Unit
    override suspend fun refreshLanHosts() = Unit
    override suspend fun joinLanHost(hostId: String, nickname: String, avatarId: String?) = Result.success(Unit)
    override suspend fun joinWithCode(joinCode: String, nickname: String, avatarId: String?) = Result.success(Unit)
    override suspend fun submitStudentAnswer(answerIds: List<String>) = Unit
    override suspend fun updateStudentProfile(nickname: String, avatarId: String?) = Unit
    override suspend fun clearStudentError() = Unit
    override suspend fun syncSession() = Unit
}

class FakeAssignmentRepositoryUi : AssignmentRepositoryUi {
    override val assignments: Flow<AssignmentsUiState> = flowOf(AssignmentsUiState())
    override val reports: Flow<ReportsUiState> = flowOf(ReportsUiState())
}
