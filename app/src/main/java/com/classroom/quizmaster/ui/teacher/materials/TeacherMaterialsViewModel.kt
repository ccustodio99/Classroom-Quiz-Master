package com.classroom.quizmaster.ui.teacher.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.LearningMaterialRepository
import com.classroom.quizmaster.ui.model.SelectionOptionUi
import com.classroom.quizmaster.ui.materials.MaterialSummaryUi
import com.classroom.quizmaster.ui.materials.toSummaryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TeacherMaterialsViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    private val learningMaterialRepository: LearningMaterialRepository
) : ViewModel() {

    private val filter = MutableStateFlow(FilterState())
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private val materialsFlow = filter.flatMapLatest { filterState ->
        learningMaterialRepository.observeTeacherMaterials(
            classroomId = filterState.classroomId?.takeIf { it.isNotBlank() },
            includeArchived = filterState.showArchived
        )
    }

    val uiState: StateFlow<TeacherMaterialsUiState> =
        combine(
            classroomRepository.classrooms,
            materialsFlow,
            filter
        ) { classrooms, materials, filterState ->
            val activeClassrooms = classrooms.filterNot { it.isArchived }
            val options = buildList {
                add(SelectionOptionUi("", "All classrooms"))
                addAll(
                    activeClassrooms
                        .sortedBy { it.name.lowercase() }
                        .map { classroom ->
                            SelectionOptionUi(
                                id = classroom.id,
                                label = classroom.name,
                                supportingText = listOfNotNull(
                                    classroom.grade.takeIf { it.isNotBlank() }?.let { "Grade $it" },
                                    classroom.subject.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                            )
                        }
                )
            }
            val summaries = materials.map { it.toSummaryUi() }
            val emptyMessage = if (filterState.showArchived) {
                "No archived materials yet"
            } else {
                "No learning materials yet"
            }
            TeacherMaterialsUiState(
                materials = summaries,
                classroomOptions = options,
                selectedClassroomId = filterState.classroomId,
                showArchived = filterState.showArchived,
                emptyMessage = emptyMessage,
                isShareEnabled = !filterState.showArchived &&
                    !filterState.classroomId.isNullOrBlank() &&
                    summaries.isNotEmpty()
            )
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TeacherMaterialsUiState()
            )

    fun selectClassroom(classroomId: String?) {
        filter.update { it.copy(classroomId = classroomId?.takeIf { it.isNotBlank() }) }
    }

    fun toggleArchived(showArchived: Boolean) {
        filter.update { it.copy(showArchived = showArchived) }
    }

    fun shareCurrentClassroom() {
        val classroomId = filter.value.classroomId
        if (classroomId.isNullOrBlank()) {
            viewModelScope.launch { _events.emit("Select a classroom to sync with students") }
            return
        }
        viewModelScope.launch {
            runCatching { learningMaterialRepository.shareSnapshotForClassroom(classroomId) }
                .onSuccess { _events.emit("Shared materials with nearby students") }
                .onFailure { err ->
                    _events.emit(err.message ?: "Unable to share materials over LAN")
                }
        }
    }

    fun archive(materialId: String) {
        viewModelScope.launch {
            runCatching { learningMaterialRepository.archive(materialId) }
                .onSuccess { _events.emit("Material archived") }
                .onFailure { err ->
                    _events.emit(err.message ?: "Unable to archive material")
                }
        }
    }

    private data class FilterState(
        val classroomId: String? = null,
        val showArchived: Boolean = false
    )
}

data class TeacherMaterialsUiState(
    val materials: List<MaterialSummaryUi> = emptyList(),
    val classroomOptions: List<SelectionOptionUi> = emptyList(),
    val selectedClassroomId: String? = null,
    val showArchived: Boolean = false,
    val emptyMessage: String = "",
    val isShareEnabled: Boolean = false
)
