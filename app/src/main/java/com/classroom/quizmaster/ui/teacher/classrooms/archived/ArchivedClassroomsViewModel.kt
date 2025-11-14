package com.classroom.quizmaster.ui.teacher.classrooms.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ArchivedTopicUi(
    val id: String,
    val name: String,
    val description: String
)

data class ArchivedClassroomUi(
    val id: String,
    val name: String,
    val meta: String?,
    val topics: List<ArchivedTopicUi>
)

data class ArchivedClassroomsUiState(
    val classrooms: List<ArchivedClassroomUi> = emptyList()
)

@HiltViewModel
class ArchivedClassroomsViewModel @Inject constructor(
    classroomRepository: ClassroomRepository
) : ViewModel() {

    val uiState: StateFlow<ArchivedClassroomsUiState> =
        combine(
            classroomRepository.archivedClassrooms,
            classroomRepository.archivedTopics
        ) { classrooms, topics ->
            val archived = classrooms.sortedByDescending { it.archivedAt }
                .map { classroom ->
                    val topicSummaries = topics
                        .filter { it.classroomId == classroom.id }
                        .sortedByDescending { it.archivedAt }
                        .map { topic ->
                            ArchivedTopicUi(
                                id = topic.id,
                                name = topic.name,
                                description = topic.description
                            )
                        }
                    ArchivedClassroomUi(
                        id = classroom.id,
                        name = classroom.name,
                        meta = listOfNotNull(
                            classroom.subject.takeIf { it.isNotBlank() },
                            classroom.grade.takeIf { it.isNotBlank() }?.let { "Grade $it" }
                        ).joinToString(" · ").ifBlank { null },
                        topics = topicSummaries
                    )
                }
            ArchivedClassroomsUiState(classrooms = archived)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ArchivedClassroomsUiState()
            )
}

