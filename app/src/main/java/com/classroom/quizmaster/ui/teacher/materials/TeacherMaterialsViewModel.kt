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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import timber.log.Timber

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TeacherMaterialsViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    private val learningMaterialRepository: LearningMaterialRepository
) : ViewModel() {

    private val filter = MutableStateFlow(FilterState())
    private val transferState = MutableStateFlow<MaterialTransferState?>(null)
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { classroomRepository.refresh() }
                .onFailure { Timber.w(it, "Failed to refresh classrooms/topics") }
        }
    }

    private val normalizedFilter = combine(filter, classroomRepository.topics) { filterState, topics ->
        val availableTopics = topics
            .filterNot { it.isArchived }
            .filter { topic ->
                filterState.classroomId == null || topic.classroomId == filterState.classroomId
            }
        val resolvedTopic = when {
            filterState.topicId == null -> null
            availableTopics.any { it.id == filterState.topicId } -> filterState.topicId
            else -> filterState.topicId
        }
        filterState.copy(topicId = resolvedTopic)
    }

    private val materialsFlow = normalizedFilter.flatMapLatest { filterState ->
        learningMaterialRepository.observeTeacherMaterials(
            classroomId = filterState.classroomId?.takeIf { it.isNotBlank() },
            topicId = filterState.topicId?.takeIf { it.isNotBlank() },
            includeArchived = filterState.showArchived
        )
    }

    val uiState: StateFlow<TeacherMaterialsUiState> =
        combine(
            classroomRepository.classrooms,
            classroomRepository.topics,
            materialsFlow,
            normalizedFilter,
            transferState
        ) { classrooms, topics, materials, filterState, transferState ->
            val activeClassrooms = classrooms.filterNot { it.isArchived }
            val sortedClassrooms = activeClassrooms.sortedBy { it.name.lowercase() }
            val classroomOptions = buildList {
                add(SelectionOptionUi("", "All classrooms"))
                addAll(
                    sortedClassrooms.map { classroom ->
                        SelectionOptionUi(
                            id = classroom.id,
                            label = classroom.name,
                            supportingText = listOfNotNull(
                                classroom.grade.takeIf { it.isNotBlank() }?.let { "Grade " },
                                classroom.subject.takeIf { it.isNotBlank() }
                            ).joinToString(" ??? ")
                        )
                    }
                )
            }
            val transferClassroomOptions = sortedClassrooms.map { classroom ->
                SelectionOptionUi(
                    id = classroom.id,
                    label = classroom.name,
                    supportingText = listOfNotNull(
                        classroom.grade.takeIf { it.isNotBlank() }?.let { "Grade " },
                        classroom.subject.takeIf { it.isNotBlank() }
                    ).joinToString(" ??? ")
                )
            }

            val topicsByClassroom = topics
                .filterNot { it.isArchived }
                .groupBy { it.classroomId }
                .mapValues { (_, grouped) ->
                    buildList {
                        add(SelectionOptionUi("", "All topics"))
                        addAll(
                            grouped
                                .sortedBy { it.name.lowercase() }
                                .map { topic -> SelectionOptionUi(topic.id, topic.name, topic.description) }
                        )
                    }
                }

            val topicOptions = topicsByClassroom[filterState.classroomId].orEmpty()
            val selectedTopicId = filterState.topicId.takeIf { id ->
                !id.isNullOrBlank() && topicOptions.any { it.id == id }
            }

            val summaries = materials.map { it.toSummaryUi() }
            val emptyMessage = if (filterState.showArchived) {
                "No archived materials yet"
            } else {
                "No learning materials yet"
            }
            val transferDialog = transferState?.let { pending ->
                if (transferClassroomOptions.isEmpty()) {
                    null
                } else {
                    val selectedClassroomId = pending.selectedClassroomId
                        ?.takeIf { id -> transferClassroomOptions.any { it.id == id } }
                    val topicOptionsForDialog = selectedClassroomId
                        ?.let { topicsByClassroom[it].orEmpty() }
                        ?: emptyList()
                    val selectedTopicForDialog = pending.selectedTopicId
                        ?.takeIf { id -> topicOptionsForDialog.any { it.id == id } }
                    val materialTitle = materials.firstOrNull { it.id == pending.materialId }?.title ?: "Material"
                    MaterialTransferDialogUi(
                        materialTitle = materialTitle,
                        mode = pending.mode,
                        classroomOptions = transferClassroomOptions,
                        selectedClassroomId = selectedClassroomId,
                        topicOptions = topicOptionsForDialog,
                        selectedTopicId = selectedTopicForDialog,
                        isSubmitting = pending.isSubmitting
                    )
                }
            }

            TeacherMaterialsUiState(
                materials = summaries,
                classroomOptions = classroomOptions,
                topicOptions = topicOptions,
                selectedClassroomId = filterState.classroomId,
                selectedTopicId = selectedTopicId,
                showArchived = filterState.showArchived,
                emptyMessage = emptyMessage,
                isShareEnabled = !filterState.showArchived &&
                    !filterState.classroomId.isNullOrBlank() &&
                    summaries.isNotEmpty(),
                transferDialog = transferDialog
            )
        }            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TeacherMaterialsUiState()
            )

    fun selectClassroom(classroomId: String?) {
        filter.update {
            it.copy(
                classroomId = classroomId?.takeIf { id -> id.isNotBlank() },
                topicId = null
            )
        }
    }

    fun selectTopic(topicId: String?) {
        filter.update { it.copy(topicId = topicId?.takeIf { id -> id.isNotBlank() }) }
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

    fun requestMove(materialId: String) {
        transferState.value = MaterialTransferState(
            materialId = materialId,
            mode = MaterialTransferMode.Move,
            selectedClassroomId = filter.value.classroomId,
            selectedTopicId = filter.value.topicId
        )
    }

    fun requestCopy(materialId: String) {
        transferState.value = MaterialTransferState(
            materialId = materialId,
            mode = MaterialTransferMode.Copy,
            selectedClassroomId = filter.value.classroomId,
            selectedTopicId = filter.value.topicId
        )
    }

    fun selectTransferClassroom(classroomId: String) {
        transferState.update { state ->
            state?.copy(
                selectedClassroomId = classroomId,
                selectedTopicId = null
            )
        }
    }

    fun selectTransferTopic(topicId: String?) {
        transferState.update { state ->
            state?.copy(selectedTopicId = topicId?.takeIf { it.isNotBlank() })
        }
    }

    fun dismissTransferDialog() {
        if (transferState.value?.isSubmitting == true) return
        transferState.value = null
    }

    fun confirmTransfer() {
        val pending = transferState.value ?: return
        val classroomId = pending.selectedClassroomId
        if (classroomId.isNullOrBlank()) {
            viewModelScope.launch { _events.emit("Choose a classroom first") }
            return
        }
        val topicId = pending.selectedTopicId?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            transferState.update { it?.copy(isSubmitting = true) }
            val result = runCatching {
                when (pending.mode) {
                    MaterialTransferMode.Move -> learningMaterialRepository.move(
                        pending.materialId,
                        classroomId,
                        topicId
                    )

                    MaterialTransferMode.Copy -> learningMaterialRepository.duplicate(
                        pending.materialId,
                        classroomId,
                        topicId
                    )
                }
            }
            result
                .onSuccess {
                    _events.emit(
                        if (pending.mode == MaterialTransferMode.Move) {
                            "Material moved"
                        } else {
                            "Material copied"
                        }
                    )
                    transferState.value = null
                }
                .onFailure { err ->
                    transferState.update { it?.copy(isSubmitting = false) }
                    val action = if (pending.mode == MaterialTransferMode.Move) "move" else "copy"
                    _events.emit(err.message ?: "Unable to $action material")
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
        val topicId: String? = null,
        val showArchived: Boolean = false
    )
}

data class TeacherMaterialsUiState(
    val materials: List<MaterialSummaryUi> = emptyList(),
    val classroomOptions: List<SelectionOptionUi> = emptyList(),
    val topicOptions: List<SelectionOptionUi> = emptyList(),
    val selectedClassroomId: String? = null,
    val selectedTopicId: String? = null,
    val showArchived: Boolean = false,
    val emptyMessage: String = "",
    val isShareEnabled: Boolean = false,
    val transferDialog: MaterialTransferDialogUi? = null
)

data class MaterialTransferDialogUi(
    val materialTitle: String,
    val mode: MaterialTransferMode,
    val classroomOptions: List<SelectionOptionUi>,
    val selectedClassroomId: String?,
    val topicOptions: List<SelectionOptionUi>,
    val selectedTopicId: String?,
    val isSubmitting: Boolean
)

enum class MaterialTransferMode { Move, Copy }

private data class MaterialTransferState(
    val materialId: String,
    val mode: MaterialTransferMode,
    val selectedClassroomId: String? = null,
    val selectedTopicId: String? = null,
    val isSubmitting: Boolean = false
)

