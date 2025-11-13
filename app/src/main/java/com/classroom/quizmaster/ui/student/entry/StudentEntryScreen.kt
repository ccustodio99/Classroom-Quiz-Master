package com.classroom.quizmaster.ui.student.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.components.AvatarPicker
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.NickNameField
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SegmentOption
import com.classroom.quizmaster.ui.components.SegmentedControl
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.ConnectionQuality
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.util.JoinCodeGenerator
import com.classroom.quizmaster.util.NicknamePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EntryTab { Lan, Code }

data class LanHostUi(
    val id: String,
    val teacherName: String,
    val subject: String,
    val players: Int,
    val latencyMs: Int,
    val joinCode: String,
    val quality: ConnectionQuality,
    val lastSeen: String
)

data class StudentEntryUiState(
    val tab: EntryTab = EntryTab.Lan,
    val nickname: String = "",
    val nicknameError: String? = null,
    val avatarOptions: List<AvatarOption> = emptyList(),
    val selectedAvatarId: String? = null,
    val lanHosts: List<LanHostUi> = emptyList(),
    val selectedHostId: String? = null,
    val joinCode: String = "",
    val joinCodeValid: Boolean = false,
    val joinCodeError: String? = null,
    val canJoin: Boolean = false,
    val statusMessage: String = "",
    val isDiscovering: Boolean = false,
    val isJoining: Boolean = false,
    val errorMessage: String? = null,
    val lastSeenHosts: String = "",
    val networkAvailable: Boolean = true
)

@HiltViewModel
class StudentEntryViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentEntryUiState())
    val uiState: StateFlow<StudentEntryUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentEntry.collectLatest { incoming ->
                _uiState.update { current ->
                    current.copy(
                        avatarOptions = incoming.avatarOptions,
                        lanHosts = incoming.lanHosts,
                        statusMessage = incoming.statusMessage,
                        isDiscovering = incoming.isDiscovering,
                        isJoining = incoming.isJoining,
                        errorMessage = incoming.errorMessage,
                        lastSeenHosts = incoming.lastSeenHosts,
                        networkAvailable = incoming.networkAvailable
                    ).recalculateJoinEligibility()
                }
            }
        }
        viewModelScope.launch { sessionRepositoryUi.refreshLanHosts() }
    }

    fun selectTab(tab: EntryTab) {
        _uiState.update { it.copy(tab = tab).recalculateJoinEligibility() }
    }

    fun updateNickname(value: String) {
        val trimmed = value.take(24)
        val error = NicknamePolicy.validationError(trimmed)
        _uiState.update {
            it.copy(nickname = trimmed, nicknameError = error).recalculateJoinEligibility()
        }
    }

    fun selectAvatar(id: String) {
        _uiState.update { it.copy(selectedAvatarId = id).recalculateJoinEligibility() }
    }

    fun updateJoinCode(raw: String) {
        val normalized = JoinCodeGenerator.normalize(raw)
        val isValid = JoinCodeGenerator.isValid(normalized)
        val error = if (normalized.isEmpty()) null else if (!isValid) {
            "Join code must be 6-8 characters"
        } else null
        _uiState.update {
            it.copy(
                joinCode = normalized,
                joinCodeValid = isValid,
                joinCodeError = error
            ).recalculateJoinEligibility()
        }
    }

    fun selectHost(hostId: String) {
        _uiState.update { it.copy(selectedHostId = hostId).recalculateJoinEligibility() }
    }

    fun refreshLanHosts() {
        viewModelScope.launch { sessionRepositoryUi.refreshLanHosts() }
    }

    fun joinLan(onJoined: () -> Unit) {
        val state = _uiState.value
        val hostId = state.selectedHostId ?: return
        val nicknameError = NicknamePolicy.validationError(state.nickname)
        if (nicknameError != null) {
            _uiState.update { it.copy(nicknameError = nicknameError) }
            return
        }
        val sanitized = NicknamePolicy.sanitize(
            state.nickname.ifBlank { "Player" },
            hostId + state.joinCode
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null) }
            sessionRepositoryUi.joinLanHost(hostId, sanitized, state.selectedAvatarId)
                .onSuccess {
                    _uiState.update { it.copy(isJoining = false) }
                    onJoined()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            errorMessage = throwable.message ?: "Unable to join host"
                        )
                    }
                }
        }
    }

    fun joinByCode(onJoined: () -> Unit) {
        val state = _uiState.value
        if (!state.joinCodeValid) {
            _uiState.update {
                it.copy(joinCodeError = "Enter a valid join code (6-8 letters)")
            }
            return
        }
        val nicknameError = NicknamePolicy.validationError(state.nickname)
        if (nicknameError != null) {
            _uiState.update { it.copy(nicknameError = nicknameError) }
            return
        }
        val sanitized = NicknamePolicy.sanitize(
            state.nickname.ifBlank { "Player" },
            state.joinCode
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null) }
            sessionRepositoryUi.joinWithCode(state.joinCode, sanitized, state.selectedAvatarId)
                .onSuccess {
                    _uiState.update { it.copy(isJoining = false) }
                    onJoined()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            errorMessage = throwable.message ?: "Unable to join with code"
                        )
                    }
                }
        }
    }

    fun clearError() {
        viewModelScope.launch { sessionRepositoryUi.clearStudentError() }
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun StudentEntryUiState.recalculateJoinEligibility(): StudentEntryUiState {
        val hasNickname = nicknameError == null && nickname.isNotBlank()
        val hasAvatar = selectedAvatarId != null || avatarOptions.isEmpty()
        val lanReady = selectedHostId != null
        val codeReady = joinCodeValid
        val allowJoin = when (tab) {
            EntryTab.Lan -> hasNickname && hasAvatar && lanReady
            EntryTab.Code -> hasNickname && hasAvatar && codeReady
        }
        return copy(canJoin = allowJoin)
    }
}

@Composable
fun StudentEntryRoute(
    onJoined: () -> Unit,
    initialTab: EntryTab = EntryTab.Lan,
    onTeacherSignIn: (() -> Unit)? = null,
    viewModel: StudentEntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialTab) { viewModel.selectTab(initialTab) }
    StudentEntryScreen(
        state = state,
        onTabSelect = viewModel::selectTab,
        onNicknameChange = viewModel::updateNickname,
        onAvatarSelect = viewModel::selectAvatar,
        onJoinCodeChange = viewModel::updateJoinCode,
        onHostSelect = viewModel::selectHost,
        onRefreshLan = viewModel::refreshLanHosts,
        onJoinLan = { viewModel.joinLan(onJoined) },
        onJoinCode = { viewModel.joinByCode(onJoined) },
        onClearError = viewModel::clearError,
        onTeacherSignIn = onTeacherSignIn
    )
}

@Composable
fun StudentEntryScreen(
    state: StudentEntryUiState,
    onTabSelect: (EntryTab) -> Unit,
    onNicknameChange: (String) -> Unit,
    onAvatarSelect: (String) -> Unit,
    onJoinCodeChange: (String) -> Unit,
    onHostSelect: (String) -> Unit,
    onRefreshLan: () -> Unit,
    onJoinLan: () -> Unit,
    onJoinCode: () -> Unit,
    onClearError: () -> Unit,
    onTeacherSignIn: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Join Classroom",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Join LAN sessions nearby or enter a code shared by your teacher.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onTeacherSignIn != null) {
            TextButton(
                onClick = onTeacherSignIn,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Are you a teacher? Sign in")
            }
        }
        SegmentedControl(
            options = listOf(
                SegmentOption(EntryTab.Lan.name, "Nearby", "Discover LAN hosts"),
                SegmentOption(EntryTab.Code.name, "Join code", "Enter 6-8 characters")
            ),
            selectedId = state.tab.name,
            onSelected = { onTabSelect(EntryTab.valueOf(it)) }
        )
        NickNameField(
            value = state.nickname,
            onValueChange = onNicknameChange,
            errorText = state.nicknameError
        )
        AvatarPicker(
            avatars = state.avatarOptions,
            selectedId = state.selectedAvatarId,
            onAvatarSelected = { onAvatarSelect(it.id) }
        )
        when (state.tab) {
            EntryTab.Lan -> LanHostList(
                state = state,
                onHostSelect = onHostSelect,
                onRefresh = onRefreshLan,
                onJoin = onJoinLan
            )
            EntryTab.Code -> JoinCodeCard(
                state = state,
                onJoinCodeChange = onJoinCodeChange,
                onJoin = onJoinCode
            )
        }
        if (state.errorMessage != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "join-error" }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    SecondaryButton(text = "Dismiss", onClick = onClearError)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        val statusText = buildString {
            append(state.statusMessage.ifBlank { "Offline capable: answers sync when online." })
            if (state.lastSeenHosts.isNotBlank()) {
                append(" · Updated ${state.lastSeenHosts}")
            }
        }
        TagChip(text = statusText)
    }
}

@Composable
private fun LanHostList(
    state: StudentEntryUiState,
    onHostSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    onJoin: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nearby hosts", style = MaterialTheme.typography.titleMedium)
            SecondaryButton(
                text = if (state.isDiscovering) "Scanning..." else "Refresh",
                onClick = onRefresh,
                enabled = !state.isDiscovering
            )
        }
        if (state.isDiscovering) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
                Text("Searching for LAN hosts", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.lanHosts.isEmpty()) {
            EmptyState(
                title = "No hosts detected",
                message = "Ask your teacher to open the lobby or use a join code."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.lanHosts, key = { it.id }) { host ->
                    LanHostCard(
                        host = host,
                        selected = host.id == state.selectedHostId,
                        onSelect = { onHostSelect(host.id) }
                    )
                }
            }
            PrimaryButton(
                text = if (state.isJoining) "Joining..." else "Join selected host",
                onClick = onJoin,
                enabled = state.canJoin && !state.isJoining,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LanHostCard(
    host: LanHostUi,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Host ${host.teacherName} ${host.subject}"
            }
            .clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (selected) 6.dp else 2.dp,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = host.subject,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Host: ${host.teacherName} · ${host.players} players")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(text = "Code ${host.joinCode}")
                TagChip(text = qualityLabel(host.quality, host.latencyMs))
            }
            Text(
                text = "Last seen ${host.lastSeen}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun qualityLabel(quality: ConnectionQuality, latencyMs: Int): String =
    when (quality) {
        ConnectionQuality.Excellent -> "Excellent · ${latencyMs}ms"
        ConnectionQuality.Good -> "Good · ${latencyMs}ms"
        ConnectionQuality.Fair -> "Fair · ${latencyMs}ms"
        ConnectionQuality.Weak -> "Weak · ${latencyMs}ms"
        ConnectionQuality.Offline -> "Offline"
    }

@Composable
private fun JoinCodeCard(
    state: StudentEntryUiState,
    onJoinCodeChange: (String) -> Unit,
    onJoin: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Join with code", style = MaterialTheme.typography.titleMedium)
        androidx.compose.material3.OutlinedTextField(
            value = state.joinCode,
            onValueChange = onJoinCodeChange,
            label = { Text("Enter join code") },
            isError = state.joinCodeError != null,
            supportingText = {
                state.joinCodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton(
            text = if (state.isJoining) "Joining..." else "Join session",
            onClick = onJoin,
            enabled = state.canJoin && !state.isJoining,
            modifier = Modifier.fillMaxWidth()
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
                    AvatarOption("2", "Bolt", emptyList(), "atom"),
                ),
                lanHosts = listOf(
                    LanHostUi(
                        id = "room1",
                        teacherName = "Mr. Lee",
                        subject = "Science lightning round",
                        players = 6,
                        latencyMs = 18,
                        joinCode = "SCILAN",
                        quality = ConnectionQuality.Good,
                        lastSeen = "moments ago"
                    ),
                    LanHostUi(
                        id = "room2",
                        teacherName = "Coach Diaz",
                        subject = "History warmup",
                        players = 4,
                        latencyMs = 32,
                        joinCode = "HIST24",
                        quality = ConnectionQuality.Fair,
                        lastSeen = "1 min ago"
                    )
                ),
                statusMessage = "LAN connected",
                selectedHostId = "room1",
                canJoin = true
            ),
            onTabSelect = {},
            onNicknameChange = {},
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
