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
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

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
            val items = container.itemBankAgent.query(objectives, limit = objectives.size * 6)
            if (items.isEmpty()) {
                _uiState.value = state.copy(errors = listOf("Walang nakitang item para sa mga layunin."))
                return@launch
            }
            val (generatedPre, generatedPost) = buildParallelForms(items)
            if (generatedPre.isEmpty() || generatedPost.isEmpty()) {
                _uiState.value = state.copy(errors = listOf("Hindi makabuo ng parallel na pagsusulit para sa post-test."))
                return@launch
            }
            val formSize = min(generatedPre.size, generatedPost.size, objectives.size * 4)
            if (formSize == 0) {
                _uiState.value = state.copy(errors = listOf("Hindi sapat ang mga item para sa pre at post test."))
                return@launch
            }
            val preItems = generatedPre.take(formSize)
            val postItems = generatedPost.take(formSize)
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

data class ModuleBuilderUiState(
    val topic: String = "",
    val objectives: String = "LO1, LO2, LO3",
    val slides: String = "Panimula sa simple interest\nPagkuwenta ng compound interest",
    val timePerItem: String = "60",
    val errors: List<String> = emptyList(),
    val message: String? = null
)

private fun buildParallelForms(items: List<Item>): Pair<List<Item>, List<Item>> {
    if (items.isEmpty()) return emptyList<Item>() to emptyList()
    val preItems = items.mapIndexed { index, item -> item.withFormSuffix("pre-${index + 1}") }
    val postItems = buildPostForm(items)
    return preItems to postItems
}

private fun buildPostForm(items: List<Item>): List<Item> {
    if (items.isEmpty()) return emptyList()
    val grouped = items.groupBy { it.objective }
    val rotationPools = grouped.mapValues { (_, bucket) ->
        if (bucket.size > 1) {
            ArrayDeque(bucket.drop(1) + bucket.take(1))
        } else {
            ArrayDeque(listOf(bucket.first().parallelVariant(0)))
        }
    }.toMutableMap()

    return items.mapIndexed { index, item ->
        val pool = rotationPools[item.objective]
        val candidate = if (pool != null && pool.isNotEmpty()) {
            pool.removeFirst()
        } else {
            item.parallelVariant(index)
        }
        val ensuredVariant = if (areItemsEquivalent(item, candidate)) {
            item.parallelVariant(index + 1)
        } else {
            candidate
        }
        ensuredVariant.withFormSuffix("post-${index + 1}")
    }
}

private fun Item.withFormSuffix(suffix: String): Item = when (this) {
    is MultipleChoiceItem -> copy(id = "$id-$suffix")
    is TrueFalseItem -> copy(id = "$id-$suffix")
    is NumericItem -> copy(id = "$id-$suffix")
    is MatchingItem -> copy(id = "$id-$suffix")
}

private fun Item.parallelVariant(seed: Int): Item = when (this) {
    is MultipleChoiceItem -> parallelVariant(seed)
    is TrueFalseItem -> parallelVariant(seed)
    is NumericItem -> parallelVariant(seed)
    is MatchingItem -> parallelVariant(seed)
}

private fun MultipleChoiceItem.parallelVariant(seed: Int): MultipleChoiceItem {
    val random = Random(id.hashCode() + seed * 31)
    val permutation = choices.indices.shuffled(random)
    val normalized = if (permutation == choices.indices.toList() && choices.size > 1) {
        (choices.indices.drop(1) + choices.indices.take(1))
    } else {
        permutation
    }
    val newChoices = normalized.map { choices[it] }
    val newCorrectIndex = normalized.indexOf(correctIndex).takeIf { it >= 0 } ?: correctIndex
    val updatedPrompt = if (prompt.contains("Variant", ignoreCase = true)) {
        prompt
    } else {
        "$prompt (Variant B: ibang pagkakasunod-sunod ng pagpipilian.)"
    }
    val updatedExplanation = if (explanation.contains("variant", ignoreCase = true)) {
        explanation
    } else {
        "$explanation (Shuffled choices para sa post-test.)"
    }
    return copy(
        prompt = updatedPrompt,
        choices = newChoices,
        correctIndex = newCorrectIndex,
        explanation = updatedExplanation
    )
}

private fun TrueFalseItem.parallelVariant(seed: Int): TrueFalseItem {
    val normalizedPrompt = prompt.removePrefix("Post-test: ").removePrefix("Suriin: ").trim()
    val updatedPrompt = "Post-test: Suriin kung totoo o mali — $normalizedPrompt"
    val updatedExplanation = if (explanation.contains("variant", ignoreCase = true)) {
        explanation
    } else {
        "$explanation (Post-test paraphrase.)"
    }
    return copy(prompt = updatedPrompt, explanation = updatedExplanation)
}

private fun NumericItem.parallelVariant(seed: Int): NumericItem {
    val adjustments = listOf(-0.05, 0.05, 0.08)
    val adjustment = adjustments[abs(seed) % adjustments.size]
    val multiplier = 1.0 + adjustment
    var replaced = false
    val updatedPrompt = NUMBER_PATTERN.replace(prompt) { match ->
        val sanitized = match.value.replace(",", "")
        val value = sanitized.toDoubleOrNull()
        if (value != null) {
            replaced = true
            formatNumberLike(match.value, value * multiplier)
        } else {
            match.value
        }
    }
    val percentText = if (adjustment >= 0) "tumaas ng ${(adjustment * 100).roundToInt()}%" else "bumaba ng ${(-adjustment * 100).roundToInt()}%"
    val promptWithNote = if (replaced) {
        "$updatedPrompt (Variant B: datos na $percentText.)"
    } else {
        "$prompt (Variant B: i-adjust ang sagot na $percentText.)"
    }
    val updatedAnswer = answer * multiplier
    val updatedTolerance = max(tolerance, abs(updatedAnswer) * 0.02)
    val updatedExplanation = if (explanation.contains("variant", ignoreCase = true)) {
        explanation
    } else {
        "$explanation (Post-test variant na $percentText ang ginamit.)"
    }
    return copy(
        prompt = promptWithNote,
        answer = updatedAnswer,
        tolerance = updatedTolerance,
        explanation = updatedExplanation
    )
}

private fun MatchingItem.parallelVariant(seed: Int): MatchingItem {
    if (pairs.size <= 1) {
        val updatedPrompt = if (prompt.contains("Variant", ignoreCase = true)) {
            prompt
        } else {
            "$prompt (Variant B: ibang paglalarawan.)"
        }
        return copy(prompt = updatedPrompt)
    }
    val random = Random(id.hashCode() + seed * 17)
    val shuffled = pairs.shuffled(random)
    val normalized = if (shuffled == pairs) {
        shuffled.drop(1) + shuffled.take(1)
    } else {
        shuffled
    }
    val updatedPrompt = if (prompt.contains("Variant", ignoreCase = true)) {
        prompt
    } else {
        "$prompt (Variant B: ibang pagkakasunod-sunod ng pares.)"
    }
    val updatedExplanation = if (explanation.contains("variant", ignoreCase = true)) {
        explanation
    } else {
        "$explanation (Post-test variant na inayos muli ang mga pares.)"
    }
    return copy(prompt = updatedPrompt, pairs = normalized, explanation = updatedExplanation)
}

private fun areItemsEquivalent(first: Item, second: Item): Boolean {
    if (first::class != second::class) return false
    return when (first) {
        is MultipleChoiceItem -> {
            val other = second as MultipleChoiceItem
            first.prompt.equals(other.prompt, ignoreCase = true) &&
                first.choices.map { it.trim() } == other.choices.map { it.trim() } &&
                first.correctIndex == other.correctIndex
        }
        is TrueFalseItem -> {
            val other = second as TrueFalseItem
            first.prompt.equals(other.prompt, ignoreCase = true) && first.answer == other.answer
        }
        is NumericItem -> {
            val other = second as NumericItem
            first.prompt.equals(other.prompt, ignoreCase = true) &&
                abs(first.answer - other.answer) <= max(first.tolerance, other.tolerance)
        }
        is MatchingItem -> {
            val other = second as MatchingItem
            first.prompt.equals(other.prompt, ignoreCase = true) && first.pairs == other.pairs
        }
    }
}

private val NUMBER_PATTERN = Regex("\\d+(?:,\\d{3})*(?:\\.\\d+)?")

private fun formatNumberLike(original: String, value: Double): String {
    val decimalPortion = original.substringAfter('.', "")
    val decimalCount = decimalPortion.takeWhile { it.isDigit() }.length
    val formatter = (NumberFormat.getNumberInstance(Locale.US) as DecimalFormat).apply {
        isGroupingUsed = original.contains(',')
        maximumFractionDigits = decimalCount
        minimumFractionDigits = decimalCount
    }
    return formatter.format(value)
}
