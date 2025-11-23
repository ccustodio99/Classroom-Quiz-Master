package com.classroom.quizmaster.ui.teacher.classrooms.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.JoinRequestStatus
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ClassroomStudentsUiState(
    val classroomId: String = "",
    val classroomName: String = "",
    val joinCode: String = "",
    val isArchived: Boolean = false,
    val identifier: String = "",
    val isAdding: Boolean = false,
    val addError: String? = null,
    val pendingRequests: List<JoinRequestUi> = emptyList(),
    val students: List<StudentRowUi> = emptyList(),
    val processingRequestId: String? = null,
    val removingStudentId: String? = null
)

data class JoinRequestUi(
    val id: String,
    val studentName: String,
    val contact: String,
    val requestedAgo: String
)

data class StudentRowUi(
    val id: String,
    val name: String,
    val contact: String,
    val joined: String
)

@HiltViewModel
class ClassroomStudentsViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classroomId: String =
        savedStateHandle[TeacherClassroomDetailViewModel.CLASSROOM_ID_KEY]
            ?: throw IllegalArgumentException("classroomId is required")

    private val identifier = MutableStateFlow("")
    private val addInFlight = MutableStateFlow(false)
    private val addError = MutableStateFlow<String?>(null)
    private val processingRequest = MutableStateFlow<String?>(null)
    private val removingStudent = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<String>()
    val events = _events

    private val rosterSources = combine(
        classroomRepository.classrooms,
        classroomRepository.students,
        classroomRepository.joinRequests
    ) { classrooms, students, requests ->
        Triple(classrooms, students, requests)
    }

    private val inputState = combine(
        identifier,
        addInFlight,
        addError,
        processingRequest,
        removingStudent
    ) { input, adding, error, processing, removing ->
        AddStudentUiState(
            identifier = input,
            isAdding = adding,
            error = error,
            processingRequestId = processing,
            removingStudentId = removing
        )
    }

    val uiState: StateFlow<ClassroomStudentsUiState> =
        combine(rosterSources, inputState) { (classrooms, students, requests), inputs ->
            val classroom = classrooms.firstOrNull { it.id == classroomId }
            val roster = students.filter { student -> classroom?.students?.contains(student.id) == true }
            ClassroomStudentsUiState(
                classroomId = classroomId,
                classroomName = classroom?.name.orEmpty(),
                joinCode = classroom?.joinCode.orEmpty(),
                isArchived = classroom?.isArchived == true,
                identifier = inputs.identifier,
                isAdding = inputs.isAdding,
                addError = inputs.error,
                pendingRequests = requests
                    .filter { it.classroomId == classroomId && it.status == JoinRequestStatus.PENDING }
                    .sortedByDescending { it.createdAt }
                    .map { request ->
                        val student = students.firstOrNull { it.id == request.studentId }
                        JoinRequestUi(
                            id = request.id,
                            studentName = student?.displayName ?: request.studentId,
                            contact = student?.email.orEmpty(),
                            requestedAgo = formatRelative(request.createdAt)
                        )
                    },
                students = roster
                    .sortedBy { it.displayName.lowercase() }
                    .map { student -> student.toRowUi() },
                processingRequestId = inputs.processingRequestId,
                removingStudentId = inputs.removingStudentId
            )
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ClassroomStudentsUiState(classroomId = classroomId)
            )

    fun updateIdentifier(value: String) {
        identifier.value = value
        addError.value = null
    }

    fun addStudent() {
        val target = identifier.value.trim()
        if (target.isBlank()) {
            addError.value = "Enter a student email or username"
            return
        }
        viewModelScope.launch {
            addInFlight.value = true
            addError.value = null
            runCatching { classroomRepository.addStudentByEmailOrUsername(classroomId, target) }
                .onSuccess {
                    identifier.value = ""
                    _events.emit("Student added to classroom")
                }
                .onFailure { error ->
                    addError.value = error.message ?: "Unable to add student"
                }
            addInFlight.value = false
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            processingRequest.value = requestId
            runCatching { classroomRepository.approveJoinRequest(requestId) }
                .onSuccess { _events.emit("Request approved") }
                .onFailure { _events.emit(errorMessage(it)) }
            processingRequest.value = null
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            processingRequest.value = requestId
            runCatching { classroomRepository.denyJoinRequest(requestId) }
                .onSuccess { _events.emit("Request declined") }
                .onFailure { _events.emit(errorMessage(it)) }
            processingRequest.value = null
        }
    }

    fun removeStudent(studentId: String) {
        viewModelScope.launch {
            removingStudent.value = studentId
            runCatching { classroomRepository.removeStudentFromClassroom(classroomId, studentId) }
                .onSuccess { _events.emit("Student removed") }
                .onFailure { _events.emit(errorMessage(it)) }
            removingStudent.value = null
        }
    }

    private fun errorMessage(error: Throwable): String =
        error.message ?: "Something went wrong"

    private fun Student.toRowUi(): StudentRowUi = StudentRowUi(
        id = id,
        name = displayName.ifBlank { "Student" },
        contact = email,
        joined = formatDate(createdAt)
    )

    private fun formatDate(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.monthNumber}/${local.dayOfMonth}/${local.year}"
    }

    private fun formatRelative(instant: Instant): String {
        val now = kotlinx.datetime.Clock.System.now()
        val duration = (now - instant)
        val minutes = duration.inWholeMinutes
        val hours = duration.inWholeHours
        val days = duration.inWholeDays
        return when {
            minutes < 1 -> "Just now"
            hours < 1 -> "$minutes min ago"
            days < 1 -> "$hours hr ago"
            else -> "$days d ago"
        }
    }
}

private data class AddStudentUiState(
    val identifier: String,
    val isAdding: Boolean,
    val error: String?,
    val processingRequestId: String?,
    val removingStudentId: String?
)
