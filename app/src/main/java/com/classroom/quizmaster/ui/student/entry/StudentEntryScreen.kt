package com.classroom.quizmaster.ui.student.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import com.classroom.quizmaster.ui.components.AvatarPicker
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.NickNameField
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SegmentedControl
import com.classroom.quizmaster.ui.components.SegmentOption
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EntryTab { Lan, Code }

data class LanHostUi(
    val id: String,
    val teacherName: String,
    val subject: String,
    val players: Int,
    val latencyMs: Int
)

data class StudentEntryUiState(
    val tab: EntryTab = EntryTab.Lan,
    val nickname: String = "",
    val avatarOptions: List<AvatarOption> = emptyList(),
    val selectedAvatarId: String? = null,
    val lanHosts: List<LanHostUi> = emptyList(),
    val joinCode: String = "",
    val canJoin: Boolean = false,
    val statusMessage: String = ""
)

@HiltViewModel
class StudentEntryViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentEntryUiState())
    val uiState: StateFlow<StudentEntryUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentEntry.collect { incoming ->
                _uiState.update {
                    incoming.copy(
                        tab = it.tab,
                        nickname = it.nickname,
                        selectedAvatarId = it.selectedAvatarId,
                        joinCode = it.joinCode,
                        canJoin = it.canJoin
                    )
                }
            }
        }
    }

    fun selectTab(tab: EntryTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun updateNickname(value: String) {
        _uiState.update {
            val sanitized = value.take(24)
            it.copy(nickname = sanitized, canJoin = sanitized.length >= 3)
        }
    }

    fun selectAvatar(id: String) {
        _uiState.update { it.copy(selectedAvatarId = id) }
    }

    fun updateJoinCode(value: String) {
        _uiState.update { it.copy(joinCode = value.uppercase(), canJoin = value.length >= 4) }
    }

    fun join(onJoined: () -> Unit) {
        if (_uiState.value.canJoin) {
            onJoined()
        }
    }
}

@Composable
fun StudentEntryRoute(
    onJoined: () -> Unit,
    initialTab: EntryTab = EntryTab.Lan,
    viewModel: StudentEntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialTab) {
        viewModel.selectTab(initialTab)
    }
    StudentEntryScreen(
        state = state,
        onTabSelect = viewModel::selectTab,
        onNicknameChange = viewModel::updateNickname,
        onAvatarSelect = viewModel::selectAvatar,
        onJoinCodeChange = viewModel::updateJoinCode,
        onJoinLan = { viewModel.join(onJoined) },
        onJoinCode = { viewModel.join(onJoined) }
    )
}

@Composable
fun StudentEntryScreen(
    state: StudentEntryUiState,
    onTabSelect: (EntryTab) -> Unit,
    onNicknameChange: (String) -> Unit,
    onAvatarSelect: (String) -> Unit,
    onJoinCodeChange: (String) -> Unit,
    onJoinLan: () -> Unit,
    onJoinCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Join Classroom", style = MaterialTheme.typography.headlineMedium)
        SegmentedControl(
            options = listOf(
                SegmentOption(EntryTab.Lan.name, "LAN", "Nearby hosts"),
                SegmentOption(EntryTab.Code.name, "Code", "6-digit")
            ),
            selectedId = state.tab.name,
            onSelected = { onTabSelect(EntryTab.valueOf(it)) }
        )
        NickNameField(value = state.nickname, onValueChange = onNicknameChange)
        AvatarPicker(
            avatars = state.avatarOptions,
            selectedId = state.selectedAvatarId,
            onAvatarSelected = { onAvatarSelect(it.id) }
        )
        when (state.tab) {
            EntryTab.Lan -> LanTab(state, onJoinLan)
            EntryTab.Code -> JoinCodeTab(state, onJoinCode, onJoinCodeChange)
        }
        Spacer(modifier = Modifier.weight(1f))
        TagChip(text = state.statusMessage.ifBlank { "All progress saves locally offline." })
    }
}

@Composable
private fun LanTab(
    state: StudentEntryUiState,
    onJoinLan: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Hosts nearby", style = MaterialTheme.typography.titleMedium)
        if (state.lanHosts.isEmpty()) {
            EmptyState(
                title = "No LAN hosts detected",
                message = "Ask your teacher to open the lobby or try the join code tab."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.lanHosts, key = { it.id }) { host ->
                    SurfaceHost(host = host, onJoin = onJoinLan, enabled = state.canJoin)
                }
            }
        }
    }
}

@Composable
private fun SurfaceHost(
    host: LanHostUi,
    onJoin: () -> Unit,
    enabled: Boolean
) {
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(host.subject, style = MaterialTheme.typography.titleMedium)
            Text("Host: ${host.teacherName} - ${host.players} players")
            SecondaryButton(text = "Join", onClick = onJoin, enabled = enabled)
        }
    }
}

@Composable
private fun JoinCodeTab(
    state: StudentEntryUiState,
    onJoinCode: () -> Unit,
    onJoinCodeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Join code", style = MaterialTheme.typography.titleMedium)
        androidx.compose.material3.OutlinedTextField(
            value = state.joinCode,
            onValueChange = onJoinCodeChange,
            label = { Text("Enter join code") },
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton(
            text = "Join session",
            onClick = onJoinCode,
            enabled = state.canJoin
        )
    }
}

@QuizPreviews
@Composable
private fun StudentEntryPreview() {
    QuizMasterTheme {
        StudentEntryScreen(
            state = StudentEntryUiState(
                avatarOptions = listOf(
                    AvatarOption("1", "Nova", emptyList(), "spark"),
                    AvatarOption("2", "Bolt", emptyList(), "atom")
                ),
                lanHosts = listOf(
                    LanHostUi("1", "Mr. Lee", "Science lightning round", 6, 18)
                ),
                canJoin = true
            ),
            onTabSelect = {},
            onNicknameChange = {},
            onAvatarSelect = {},
            onJoinCodeChange = {},
            onJoinLan = {},
            onJoinCode = {}
        )
    }
}
