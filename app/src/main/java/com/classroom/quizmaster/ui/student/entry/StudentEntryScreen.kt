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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.ui.components.AssistiveInfoCard
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SegmentOption
import com.classroom.quizmaster.ui.components.SegmentedControl
import com.classroom.quizmaster.ui.components.ScreenHeader
import com.classroom.quizmaster.ui.components.SectionCard
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.util.JoinCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EntryTab { Lan, Code }

data class StudentEntryUiState(
    val tab: EntryTab = EntryTab.Lan,
    val teachers: List<Teacher> = emptyList(),
    val selectedTeacherId: String? = null,
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
    private val sessionRepositoryUi: SessionRepositoryUi,
    private val classroomRepository: ClassroomRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentEntryUiState())
    val uiState: StateFlow<StudentEntryUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentEntry.collectLatest { incoming ->
                _uiState.update { current ->
                    current.copy(
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
        viewModelScope.launch {
            classroomRepository.classrooms.collectLatest { classrooms ->
                val teachers = classrooms.mapNotNull { classroom ->
                    authRepository.getTeacher(classroom.teacherId).first()
                }.distinct()
                _uiState.update {
                    it.copy(teachers = teachers)
                }
            }
        }
    }

    fun selectTab(tab: EntryTab) {
        _uiState.update { it.copy(tab = tab).recalculateJoinEligibility() }
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

    fun selectTeacher(teacherId: String) {
        _uiState.update { it.copy(selectedTeacherId = teacherId).recalculateJoinEligibility() }
    }

    fun refreshTeachers() {
        viewModelScope.launch { classroomRepository.refresh() }
    }

    fun joinTeacher(onJoined: () -> Unit) {
        val state = _uiState.value
        val teacherId = state.selectedTeacherId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null) }
            // TODO: Implement join teacher functionality
            onJoined()
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
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null) }
            sessionRepositoryUi.joinWithCode(state.joinCode, "", null)
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
        val lanReady = selectedTeacherId != null
        val codeReady = joinCodeValid
        val allowJoin = when (tab) {
            EntryTab.Lan -> lanReady
            EntryTab.Code -> codeReady
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
        onJoinCodeChange = viewModel::updateJoinCode,
        onTeacherSelect = viewModel::selectTeacher,
        onRefreshTeachers = viewModel::refreshTeachers,
        onJoinTeacher = { viewModel.joinTeacher(onJoined) },
        onJoinCode = { viewModel.joinByCode(onJoined) },
        onClearError = viewModel::clearError,
        onTeacherSignIn = onTeacherSignIn
    )
}

@Composable
fun StudentEntryScreen(
    state: StudentEntryUiState,
    onTabSelect: (EntryTab) -> Unit,
    onJoinCodeChange: (String) -> Unit,
    onTeacherSelect: (String) -> Unit,
    onRefreshTeachers: () -> Unit,
    onJoinTeacher: () -> Unit,
    onJoinCode: () -> Unit,
    onClearError: () -> Unit,
    onTeacherSignIn: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Join Classroom",
            subtitle = "Discover nearby teachers or use a join code from your teacher."
        )
        if (!state.networkAvailable) {
            AssistiveInfoCard(
                title = "You're offline",
                body = "You can still get ready and your answers will sync once a connection is restored."
            )
        }
        if (onTeacherSignIn != null) {
            TextButton(
                onClick = onTeacherSignIn,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Are you a teacher? Sign in")
            }
        }
        SectionCard(
            title = "How do you want to join?",
            subtitle = "Switch between nearby teachers and code entry."
        ) {
            SegmentedControl(
                options = listOf(
                    SegmentOption(EntryTab.Lan.name, "Nearby", "Discover teachers"),
                    SegmentOption(EntryTab.Code.name, "Join code", "Enter 6-8 characters")
                ),
                selectedId = state.tab.name,
                onSelected = { onTabSelect(EntryTab.valueOf(it)) }
            )
        }
        when (state.tab) {
            EntryTab.Lan -> TeacherList(
                state = state,
                onTeacherSelect = onTeacherSelect,
                onRefresh = onRefreshTeachers,
                onJoin = onJoinTeacher
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
private fun TeacherList(
    state: StudentEntryUiState,
    onTeacherSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    onJoin: () -> Unit
) {
    SectionCard(
        title = "Nearby teachers",
        subtitle = if (state.networkAvailable) {
            "Pick a teacher running on the same network."
        } else {
            "Connect to Wi‑Fi to see teachers hosting a lobby."
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Available teachers", style = MaterialTheme.typography.titleMedium)
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
                    Text("Searching for teachers", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.teachers.isEmpty()) {
                EmptyState(
                    title = "No teachers detected",
                    message = "Ask your teacher to open the lobby or use a join code."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(state.teachers, key = { it.id }) { teacher ->
                        TeacherCard(
                            teacher = teacher,
                            selected = teacher.id == state.selectedTeacherId,
                            onSelect = { onTeacherSelect(teacher.id) }
                        )
                    }
                }
                PrimaryButton(
                    text = if (state.isJoining) "Joining..." else "Join selected teacher",
                    onClick = onJoin,
                    enabled = state.canJoin && !state.isJoining,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TeacherCard(
    teacher: Teacher,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Teacher ${teacher.displayName}"
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = teacher.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun JoinCodeCard(
    state: StudentEntryUiState,
    onJoinCodeChange: (String) -> Unit,
    onJoin: () -> Unit
) {
    SectionCard(
        title = "Join with a code",
        subtitle = "Enter 6-8 letters shared by your teacher. We automatically uppercase letters."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.OutlinedTextField(
                value = state.joinCode,
                onValueChange = onJoinCodeChange,
                label = { Text("Enter join code") },
                isError = state.joinCodeError != null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                supportingText = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Use letters only; you'll be able to paste and we'll keep it tidy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.joinCodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                text = if (state.isJoining) "Joining..." else "Join with code",
                onClick = onJoin,
                enabled = state.canJoin && !state.isJoining,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@QuizPreviews
@Composable
private fun StudentEntryPreview() {
    QuizMasterTheme {
        StudentEntryScreen(
            state = StudentEntryUiState(),
            onTabSelect = {},
            onJoinCodeChange = {},
            onTeacherSelect = {},
            onRefreshTeachers = {},
            onJoinTeacher = {},
            onJoinCode = {},
            onClearError = {}
        )
    }
}
