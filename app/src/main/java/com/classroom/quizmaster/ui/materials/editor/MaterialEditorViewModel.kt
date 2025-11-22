package com.classroom.quizmaster.ui.materials.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.LearningMaterial
import com.classroom.quizmaster.domain.model.MaterialAttachment
import com.classroom.quizmaster.domain.model.MaterialAttachmentType
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.LearningMaterialRepository
import com.classroom.quizmaster.ui.model.SelectionOptionUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber
import java.util.UUID

@HiltViewModel
class MaterialEditorViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    private val learningMaterialRepository: LearningMaterialRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val materialIdArg: String = savedStateHandle.get<String>("materialId").orEmpty()
    private val classroomArg: String = savedStateHandle.get<String>("classroomId").orEmpty()
    private val topicArg: String = savedStateHandle.get<String>("topicId").orEmpty()

    private val _uiState = MutableStateFlow(MaterialEditorUiState())
    val uiState: StateFlow<MaterialEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MaterialEditorEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { classroomRepository.refresh() }
                .onFailure { Timber.w(it, "Failed to refresh classrooms/topics") }
        }

        viewModelScope.launch {
            val material = materialIdArg.takeIf { it.isNotBlank() }
                ?.let { learningMaterialRepository.get(it) }
            var seededMaterial = false

            combine(classroomRepository.classrooms, classroomRepository.topics) { classrooms, topics ->
                classrooms to topics
            }.collect { (classrooms, topics) ->
                val classroomOptions = classrooms
                    .filterNot { it.isArchived }
                    .sortedBy { it.name.lowercase() }
                    .map { classroom ->
                        SelectionOptionUi(
                            id = classroom.id,
                            label = classroom.name,
                            supportingText = listOfNotNull(
                                classroom.grade.takeIf { it.isNotBlank() }?.let { "Grade $it" },
                                classroom.subject.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                        )
                    }

                val topicsByClassroom = topics
                    .filterNot { it.isArchived }
                    .groupBy { it.classroomId }
                    .mapValues { (_, grouped) ->
                        grouped.sortedBy { it.name.lowercase() }
                            .map { topic -> SelectionOptionUi(topic.id, topic.name, topic.description) }
                    }

                _uiState.update { current ->
                    val selectedClassroom = current.selectedClassroomId
                        .takeIf { id -> id.isNotBlank() && classroomOptions.any { it.id == id } }
                        ?: material?.classroomId
                            ?.takeIf { id -> !seededMaterial && classroomOptions.any { option -> option.id == id } }
                        ?: classroomArg.takeIf { arg -> arg.isNotBlank() && classroomOptions.any { it.id == arg } }
                        ?: classroomOptions.firstOrNull()?.id.orEmpty()

                    val topicOptions = topicsByClassroom[selectedClassroom].orEmpty()
                    val selectedTopic = current.selectedTopicId
                        .takeIf { id -> id.isNotBlank() && topicOptions.any { it.id == id } }
                        ?: material?.topicId
                            ?.takeIf { id -> !seededMaterial && topicOptions.any { option -> option.id == id } }
                        ?: topicArg.takeIf { arg -> arg.isNotBlank() && topicOptions.any { it.id == arg } }
                        ?: topicOptions.firstOrNull()?.id.orEmpty()

                    if (!seededMaterial && material != null) {
                        seededMaterial = true
                        MaterialEditorUiState(
                            materialId = material.id,
                            title = material.title,
                            description = material.description,
                            body = material.body,
                            classroomOptions = classroomOptions,
                            topicsByClassroom = topicsByClassroom,
                            selectedClassroomId = selectedClassroom,
                            selectedTopicId = selectedTopic,
                            attachments = material.attachments.map { it.toDraft() },
                            createdAt = material.createdAt
                        ).validate()
                    } else {
                        current.copy(
                            classroomOptions = classroomOptions,
                            topicsByClassroom = topicsByClassroom,
                            selectedClassroomId = selectedClassroom,
                            selectedTopicId = selectedTopic
                        ).validate()
                    }
                }
            }
        }
    }

    fun updateTitle(value: String) = updateState { copy(title = value).validate() }

    fun updateDescription(value: String) = updateState { copy(description = value) }

    fun updateBody(value: String) = updateState { copy(body = value) }

    fun updateClassroom(classroomId: String) {
        val topics = _uiState.value.topicsByClassroom[classroomId].orEmpty()
        val firstTopic = topics.firstOrNull()?.id.orEmpty()
        updateState { copy(selectedClassroomId = classroomId, selectedTopicId = firstTopic).validate() }
    }

    fun updateTopic(topicId: String) = updateState { copy(selectedTopicId = topicId) }

    fun addAttachment(type: MaterialAttachmentType = MaterialAttachmentType.TEXT) {
        updateState {
            copy(
                attachments = attachments + MaterialAttachmentDraft(
                    id = "draft-${UUID.randomUUID()}",
                    type = type
                )
            )
        }
    }

    fun updateAttachment(id: String, transform: (MaterialAttachmentDraft) -> MaterialAttachmentDraft) {
        updateState {
            copy(attachments = attachments.map { draft -> if (draft.id == id) transform(draft) else draft })
        }
    }

    fun removeAttachment(id: String) {
        updateState { copy(attachments = attachments.filterNot { it.id == id }) }
    }

    fun save() {
        val current = _uiState.value
        if (!current.canSave || current.isSaving) return
        val payload = current.toDomain()
        viewModelScope.launch {
            updateState { copy(isSaving = true, errorMessage = null) }
            val saveResult = runCatching { learningMaterialRepository.upsert(payload) }
            val materialId = saveResult.getOrElse { err ->
                updateState { copy(isSaving = false, errorMessage = err.message ?: "Unable to save material") }
                _events.emit(MaterialEditorEvent.Error(err.message ?: "Unable to save material"))
                return@launch
            }
            val syncResult = runCatching {
                learningMaterialRepository.shareSnapshotForClassroom(payload.classroomId)
            }
            updateState { copy(isSaving = false) }
            if (syncResult.isFailure) {
                _events.emit(
                    MaterialEditorEvent.Error(
                        syncResult.exceptionOrNull()?.message
                            ?: "Saved, but could not sync materials to students"
                    )
                )
            }
            _events.emit(MaterialEditorEvent.Saved(materialId))
        }
    }

    private fun updateState(transform: MaterialEditorUiState.() -> MaterialEditorUiState) {
        _uiState.update { it.transform().validate() }
    }

    private fun MaterialEditorUiState.toDomain(): LearningMaterial {
        val now = Clock.System.now()
        val resolvedId = materialId.orEmpty()
        return LearningMaterial(
            id = resolvedId,
            teacherId = "",
            classroomId = selectedClassroomId,
            classroomName = "",
            topicId = selectedTopicId,
            topicName = "",
            title = title,
            description = description,
            body = body,
            attachments = attachments.map { it.toDomain(resolvedId.ifBlank { "mat-${UUID.randomUUID()}" }) },
            createdAt = createdAt ?: now,
            updatedAt = now
        )
    }

    private fun MaterialAttachmentDraft.toDomain(materialId: String): MaterialAttachment {
        val metadata = when (type) {
            MaterialAttachmentType.TEXT -> mapOf("text" to textContent)
            else -> emptyMap()
        }
        val resolvedMaterialId = if (materialId.isNotBlank()) materialId else "mat-${UUID.randomUUID()}"
        val resolvedId = id.takeIf { it.isNotBlank() } ?: "att-${UUID.randomUUID()}"
        return MaterialAttachment(
            id = resolvedId,
            materialId = resolvedMaterialId,
            displayName = displayName.ifBlank { type.name.lowercase().replaceFirstChar { it.titlecase() } },
            type = type,
            uri = if (type == MaterialAttachmentType.TEXT) "" else uri,
            mimeType = mimeType.ifBlank { null },
            sizeBytes = 0,
            metadata = metadata
        )
    }

    private fun MaterialAttachment.toDraft(): MaterialAttachmentDraft = MaterialAttachmentDraft(
        id = id,
        type = type,
        displayName = displayName,
        uri = uri,
        mimeType = mimeType.orEmpty(),
        textContent = metadata["text"].orEmpty()
    )

    private fun MaterialEditorUiState.validate(): MaterialEditorUiState {
        val canSave = title.isNotBlank() && selectedClassroomId.isNotBlank() && selectedTopicId.isNotBlank() && !isSaving
        return copy(canSave = canSave)
    }
}

data class MaterialEditorUiState(
    val materialId: String? = null,
    val title: String = "",
    val description: String = "",
    val body: String = "",
    val classroomOptions: List<SelectionOptionUi> = emptyList(),
    val topicsByClassroom: Map<String, List<SelectionOptionUi>> = emptyMap(),
    val selectedClassroomId: String = "",
    val selectedTopicId: String = "",
    val attachments: List<MaterialAttachmentDraft> = emptyList(),
    val createdAt: Instant? = null,
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val errorMessage: String? = null
)

data class MaterialAttachmentDraft(
    val id: String,
    val type: MaterialAttachmentType,
    val displayName: String = "",
    val uri: String = "",
    val mimeType: String = "",
    val textContent: String = ""
)

sealed interface MaterialEditorEvent {
    data class Saved(val materialId: String) : MaterialEditorEvent
    data class Error(val message: String) : MaterialEditorEvent
}
