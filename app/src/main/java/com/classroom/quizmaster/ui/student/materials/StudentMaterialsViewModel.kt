package com.classroom.quizmaster.ui.student.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.LearningMaterialRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.ui.materials.MaterialSummaryUi
import com.classroom.quizmaster.ui.materials.toSummaryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.classroom.quizmaster.util.switchMapLatest

@HiltViewModel
class StudentMaterialsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val learningMaterialRepository: LearningMaterialRepository
) : ViewModel() {

    private val classroomFlow = sessionRepository.session
        .map { it?.classroomId }

    val uiState: StateFlow<StudentMaterialsUiState> =
        classroomFlow
            .switchMapLatest { classroomId ->
                learningMaterialRepository.observeStudentMaterials(classroomId = classroomId)
                    .map { materials -> classroomId to materials }
            }
            .map { (classroomId, materials) ->
                val summaries = materials.map { it.toSummaryUi() }
                StudentMaterialsUiState(
                    materials = summaries,
                    emptyMessage = if (classroomId.isNullOrBlank()) {
                        "Join a class to receive shared materials."
                    } else if (summaries.isEmpty()) {
                        "Your teacher hasn't shared materials for this class yet."
                    } else {
                        ""
                    }
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                StudentMaterialsUiState()
            )
}

data class StudentMaterialsUiState(
    val materials: List<MaterialSummaryUi> = emptyList(),
    val emptyMessage: String = ""
)
