package com.classroom.quizmaster.ui.feature.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.Assessment
import com.classroom.quizmaster.domain.model.Item
import com.classroom.quizmaster.domain.model.Lesson
import com.classroom.quizmaster.domain.model.LessonSlide
import com.classroom.quizmaster.domain.model.MiniCheck
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.ModuleSettings
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.TrueFalseItem
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModuleBuilderViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(ModuleBuilderUiState())
    val uiState: StateFlow<ModuleBuilderUiState> = _uiState

    fun onTopicChanged(value: String) {
        _uiState.value = _uiState.value.copy(topic = value, errors = emptyList(), message = null)
    }

    fun onObjectivesChanged(value: String) {
        _uiState.value = _uiState.value.copy(objectives = value, errors = emptyList(), message = null)
    }

    fun onSlidesChanged(value: String) {
        _uiState.value = _uiState.value.copy(slides = value, errors = emptyList(), message = null)
    }

    fun onTimePerItemChanged(value: String) {
        _uiState.value = _uiState.value.copy(timePerItem = value, errors = emptyList(), message = null)
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val objectives = state.objectives.split(',').mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }
            if (objectives.isEmpty()) {
                _uiState.value = state.copy(errors = listOf("Magdagdag ng hindi bababa sa isang learning objective."))
                return@launch
            }
            val slides = state.slides.split('\n').mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) null else trimmed
            }
            if (slides.isEmpty()) {
                _uiState.value = state.copy(errors = listOf("Magdagdag ng kahit isang lesson slide."))
                return@launch
            }
            val items = container.itemBankAgent.query(objectives, limit = objectives.size * 4)
            if (items.isEmpty()) {
                _uiState.value = state.copy(errors = listOf("Walang nakitang item para sa mga layunin."))
                return@launch
            }
            val preItems = items
            val postItems = items.mapIndexed { index, item ->
                item.copyForForm("post-${index + 1}")
            }
            val module = Module(
                id = UUID.randomUUID().toString(),
                topic = state.topic.ifBlank { "G11 Math Module" },
                objectives = objectives,
                preTest = Assessment(id = UUID.randomUUID().toString(), items = preItems),
                lesson = Lesson(
                    id = UUID.randomUUID().toString(),
                    slides = slides.mapIndexed { index, text ->
                        LessonSlide(
                            id = "slide-${index + 1}",
                            title = "Slide ${index + 1}",
                            content = text,
                            miniCheck = MiniCheck(prompt = "Ano ang takeaway?", correctAnswer = text.take(15))
                        )
                    }
                ),
                postTest = Assessment(id = UUID.randomUUID().toString(), items = postItems),
                settings = ModuleSettings(timePerItemSeconds = state.timePerItem.toIntOrNull() ?: 60)
            )
            val violations = container.moduleBuilderAgent.validate(module)
            if (violations.isNotEmpty()) {
                _uiState.value = state.copy(errors = violations.map { it.message })
                return@launch
            }
            container.moduleBuilderAgent.createOrUpdate(module)
                .onSuccess {
                    _uiState.value = ModuleBuilderUiState(message = "Module saved!")
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = state.copy(errors = listOf(error.message ?: "Unknown error"))
                }
        }
    }
}

private fun Item.copyForForm(suffix: String): Item = when (this) {
    is MultipleChoiceItem -> copy(id = "$id-$suffix")
    is TrueFalseItem -> copy(id = "$id-$suffix")
    is NumericItem -> copy(id = "$id-$suffix")
    is MatchingItem -> copy(id = "$id-$suffix")
}

data class ModuleBuilderUiState(
    val topic: String = "",
    val objectives: String = "LO1, LO2, LO3",
    val slides: String = "Panimula sa simple interest\nPagkuwenta ng compound interest",
    val timePerItem: String = "60",
    val errors: List<String> = emptyList(),
    val message: String? = null
)
