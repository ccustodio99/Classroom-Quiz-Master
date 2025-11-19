package com.classroom.quizmaster.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.student.entry.EntryTab
import com.classroom.quizmaster.ui.student.entry.LanHostUi
import com.classroom.quizmaster.ui.student.entry.StudentEntryScreen
import com.classroom.quizmaster.ui.student.entry.StudentEntryUiState
import com.classroom.quizmaster.ui.student.play.StudentPlayScreen
import com.classroom.quizmaster.ui.student.play.StudentPlayUiState
import com.classroom.quizmaster.ui.teacher.host.HostLiveScreen
import com.classroom.quizmaster.ui.teacher.host.HostLiveUiState
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyScreen
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyUiState
import com.classroom.quizmaster.ui.teacher.quiz_editor.QuizEditorScreen
import com.classroom.quizmaster.ui.teacher.quiz_editor.QuizEditorUiState
import com.classroom.quizmaster.ui.components.LeaderboardList
import com.classroom.quizmaster.ui.components.TimerRing
import com.classroom.quizmaster.ui.model.DistributionBar
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import org.junit.Rule
import org.junit.Test

class UiComponentTests {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun joinFlow_enablesButtonWhenValid() {
        var state by mutableStateOf(
            StudentEntryUiState(
                tab = EntryTab.Code,
                avatarOptions = listOf(AvatarOption("1", "Nova", emptyList(), "spark")),
                lanHosts = emptyList(),
                canJoin = false,
                joinCode = "",
                joinCodeValid = false
            )
        )
        composeRule.setContent {
            QuizMasterTheme {
                StudentEntryScreen(
                    state = state,
                    onTabSelect = {},
                    onNicknameChange = {
                        state = state.copy(nickname = it, nicknameError = null)
                    },
                    onAvatarSelect = {},
                    onJoinCodeChange = {},
                    onHostSelect = {},
                    onRefreshLan = {},
                    onJoinLan = {},
                    onJoinCode = {},
                    onClearError = {}
                )
            }
        }
        composeRule.onNodeWithText("Join session").assertIsNotEnabled()
        composeRule.runOnUiThread {
            state = state.copy(
                canJoin = true,
                nickname = "Ava",
                joinCode = "SCILAN",
                joinCodeValid = true
            )
        }
        composeRule.onNodeWithText("Join session").assertIsEnabled()
    }

    @Test
    fun quizEditor_addQuestionInvokesCallback() {
        var addedType: QuestionTypeUi? = null
        composeRule.setContent {
            QuizMasterTheme {
                QuizEditorScreen(
                    state = QuizEditorUiState(),
                    onTitleChange = {},
                    onGradeChange = {},
                    onSubjectChange = {},
                    onTimeChange = {},
                    onShuffleChange = {},
                    onAddQuestion = { addedType = it },
                    onQuestionStem = { _, _ -> },
                    onAnswerChange = { _, _, _ -> },
                    onToggleCorrect = { _, _ -> },
                    onExplanationChange = { _, _ -> },
                    onReorderQuestion = { _, _ -> },
                    onSaveClick = {},
                    onDiscardClick = {},
                    onDiscardConfirmed = {},
                    onConfirmSave = {},
                    onDismissSaveDialog = {},
                    onDismissDiscardDialog = {}
                )
            }
        }
        composeRule.onNodeWithText("+ MultipleChoice").performClick()
        assert(addedType == QuestionTypeUi.MultipleChoice)
    }

    @Test
    fun quizEditor_reorderButtonsInvokeCallback() {
        var fromIndex = -1
        var toIndex = -1
        val editorState = QuizEditorUiState(
            questions = listOf(
                QuestionDraftUi(
                    id = "q1",
                    stem = "First?",
                    type = QuestionTypeUi.MultipleChoice,
                    answers = listOf(AnswerOptionUi("a1", "A", "One", true)),
                    explanation = ""
                ),
                QuestionDraftUi(
                    id = "q2",
                    stem = "Second?",
                    type = QuestionTypeUi.MultipleChoice,
                    answers = listOf(AnswerOptionUi("b1", "A", "Two", true)),
                    explanation = ""
                )
            )
        )
        composeRule.setContent {
            QuizMasterTheme {
                QuizEditorScreen(
                    state = editorState,
                    onTitleChange = {},
                    onGradeChange = {},
                    onSubjectChange = {},
                    onTimeChange = {},
                    onShuffleChange = {},
                    onAddQuestion = {},
                    onQuestionStem = { _, _ -> },
                    onAnswerChange = { _, _, _ -> },
                    onToggleCorrect = { _, _ -> },
                    onExplanationChange = { _, _ -> },
                    onReorderQuestion = { from, to ->
                        fromIndex = from
                        toIndex = to
                    },
                    onSaveClick = {},
                    onDiscardClick = {},
                    onDiscardConfirmed = {},
                    onConfirmSave = {},
                    onDismissSaveDialog = {},
                    onDismissDiscardDialog = {}
                )
            }
        }
        composeRule.onNodeWithContentDescription("Move question down").performClick()
        assert(fromIndex == 0 && toIndex == 1)
    }

    @Test
    fun hostControls_switchesLabelOnReveal() {
        var state by mutableStateOf(
            HostLiveUiState(
                question = QuestionDraftUi(
                    id = "q1",
                    stem = "Test",
                    type = QuestionTypeUi.MultipleChoice,
                    answers = listOf(
                        AnswerOptionUi("a", "A", "One", true)
                    ),
                    explanation = ""
                ),
                distribution = emptyList(),
                leaderboard = emptyList()
            )
        )
        composeRule.setContent {
            QuizMasterTheme {
                HostLiveScreen(
                    state = state,
                    onReveal = { state = state.copy(isRevealed = true) },
                    onNext = {},
                    onToggleLeaderboard = {},
                    onToggleMute = {},
                    onEnd = {}
                )
            }
        }
        composeRule.onNodeWithText("Reveal").performClick()
        composeRule.runOnUiThread { state = state.copy(isRevealed = true) }
        composeRule.onNodeWithText("Next question").assertExists()
    }

    @Test
    fun hostControls_invokesNextWhenRevealed() {
        var advanced = false
        val state = HostLiveUiState(
            isRevealed = true,
            question = QuestionDraftUi(
                id = "q1",
                stem = "Ready?",
                type = QuestionTypeUi.TrueFalse,
                answers = listOf(AnswerOptionUi("a", "A", "Yes", true)),
                explanation = ""
            ),
            leaderboard = emptyList(),
            distribution = emptyList()
        )
        composeRule.setContent {
            QuizMasterTheme {
                HostLiveScreen(
                    state = state,
                    onReveal = {},
                    onNext = { advanced = true },
                    onToggleLeaderboard = {},
                    onToggleMute = {},
                    onEnd = {}
                )
            }
        }
        composeRule.onNodeWithText("Next question").performClick()
        assert(advanced)
    }

    @Test
    fun launchLobby_startFlowCallsOnStart() {
        var started = false
        val lobbyState = LaunchLobbyUiState(
            joinCode = "R7FT",
            qrSubtitle = "09:12",
            qrPayload = "ws://192.168.0.10:48765/ws?token=demo",
            discoveredPeers = 3,
            players = listOf(
                PlayerLobbyUi("1", "Kai", AvatarOption("1", "K", emptyList(), "spark"), true, "Ready")
            ),
            statusChips = listOf(
                StatusChipUi("lan", "LAN", StatusChipType.Lan)
            )
        )
        composeRule.setContent {
            QuizMasterTheme {
                LaunchLobbyScreen(
                    state = lobbyState,
                    onToggleLeaderboard = {},
                    onToggleLock = {},
                    onStart = { started = true },
                    onEnd = {},
                    onKick = {}
                )
            }
        }
        composeRule.onNodeWithText("Start").performClick()
        composeRule.onNodeWithText("Start now").performClick()
        assert(started)
    }

    @Test
    fun leaderboard_rendersRows() {
        composeRule.setContent {
            QuizMasterTheme {
                LeaderboardList(
                    rows = listOf(
                        LeaderboardRowUi(1, "Ava", 980, 10, AvatarOption("1", "A", emptyList(), "spark"), true),
                        LeaderboardRowUi(2, "Liam", 920, -5, AvatarOption("2", "L", emptyList(), "atom"))
                    )
                )
            }
        }
        composeRule.onNodeWithText("Ava").assertExists()
        composeRule.onNodeWithText("Liam").assertExists()
    }

    @Test
    fun timer_hasAccessibleDescription() {
        composeRule.setContent {
            QuizMasterTheme {
                TimerRing(progress = 0.5f, remainingSeconds = 15)
            }
        }
        composeRule.onNodeWithContentDescription("Timer 15 seconds remaining")
            .assertContentDescriptionContains("15")
    }
}
