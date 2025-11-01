package com.classroom.quizmaster.ui.feature.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.Assessment
import com.classroom.quizmaster.domain.model.BrainstormActivity
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.domain.model.InteractiveActivity
import com.classroom.quizmaster.domain.model.Item
import com.classroom.quizmaster.domain.model.Lesson
import com.classroom.quizmaster.domain.model.LessonSlide
import com.classroom.quizmaster.domain.model.MiniCheck
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.ModuleSettings
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.OpenEndedActivity
import com.classroom.quizmaster.domain.model.PollActivity
import com.classroom.quizmaster.domain.model.PuzzleActivity
import com.classroom.quizmaster.domain.model.QuizActivity
import com.classroom.quizmaster.domain.model.SliderActivity
import com.classroom.quizmaster.domain.model.TrueFalseActivity as TrueFalseInteractive
import com.classroom.quizmaster.domain.model.TrueFalseItem
import com.classroom.quizmaster.domain.model.TypeAnswerActivity
import com.classroom.quizmaster.domain.model.WordCloudActivity
import com.classroom.quizmaster.ui.util.summaryLabel
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
import kotlin.math.roundToInt
import kotlin.random.Random

class ModuleBuilderViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(ModuleBuilderUiState().withPreview())
    val uiState: StateFlow<ModuleBuilderUiState> = _uiState

    fun onClassroomNameChanged(value: String) = updateState { it.copy(classroomName = value) }

    fun onSubjectChanged(value: String) = updateState { it.copy(subject = value) }

    fun onClassroomDescriptionChanged(value: String) = updateState { it.copy(classroomDescription = value) }

    fun onTopicChanged(value: String) = updateState { it.copy(topic = value) }

    fun onObjectivesChanged(value: String) = updateState { it.copy(objectives = value) }

    fun onSlidesChanged(value: String) = updateState { it.copy(slides = value) }

    fun onTimePerItemChanged(value: String) = updateState { it.copy(timePerItem = value) }

    private fun updateState(transform: (ModuleBuilderUiState) -> ModuleBuilderUiState) {
        val updated = transform(_uiState.value).copy(errors = emptyList(), message = null)
        _uiState.value = updated.withPreview()
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val objectives = parseObjectives(state.objectives)
            if (objectives.isEmpty()) {
                _uiState.value = state.copy(
                    errors = listOf("Magdagdag ng hindi bababa sa isang learning objective."),
                    message = null
                ).withPreview()
                return@launch
            }
            val slides = parseSlides(state.slides)
            if (slides.isEmpty()) {
                _uiState.value = state.copy(
                    errors = listOf("Magdagdag ng kahit isang lesson slide."),
                    message = null
                ).withPreview()
                return@launch
            }
            val timePerItemSeconds = state.timePerItem.toIntOrNull() ?: 60
            val items = container.itemBankAgent.query(objectives, limit = max(objectives.size * 6, 12))
            if (items.isEmpty()) {
                _uiState.value = state.copy(
                    errors = listOf("Walang nakitang item para sa mga layunin."),
                    message = null
                ).withPreview()
                return@launch
            }
            val (generatedPre, generatedPost) = buildParallelForms(items)
            if (generatedPre.isEmpty() || generatedPost.isEmpty()) {
                _uiState.value = state.copy(
                    errors = listOf("Hindi makabuo ng parallel na pagsusulit para sa post-test."),
                    message = null
                ).withPreview()
                return@launch
            }
            val formSize = minOf(generatedPre.size, generatedPost.size, objectives.size * 4)
            if (formSize == 0) {
                _uiState.value = state.copy(
                    errors = listOf("Hindi sapat ang mga item para sa pre at post test."),
                    message = null
                ).withPreview()
                return@launch
            }
            val preItems = generatedPre.take(formSize)
            val postItems = generatedPost.take(formSize)
            val topic = state.topic.ifBlank { "G11 Math Module" }
            val classroomName = state.classroomName.ifBlank { "${state.subject.ifBlank { "General Mathematics" }} Circle" }
            val subject = state.subject.ifBlank { "G11 General Mathematics" }
            val classroom = ClassroomProfile(
                id = UUID.randomUUID().toString(),
                name = classroomName,
                subject = subject,
                description = state.classroomDescription.ifBlank { "Learning circle for $subject" }
            )
            val interactiveActivities = generateInteractiveActivities(
                topic = topic,
                objectives = objectives,
                slides = slides,
                timePerItemSeconds = timePerItemSeconds,
                classroomName = classroomName
            )
            val module = Module(
                id = UUID.randomUUID().toString(),
                classroom = classroom,
                subject = subject,
                topic = topic,
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
                    },
                    interactiveActivities = interactiveActivities
                ),
                postTest = Assessment(id = UUID.randomUUID().toString(), items = postItems),
                settings = ModuleSettings(timePerItemSeconds = timePerItemSeconds)
            )
            val violations = container.moduleBuilderAgent.validate(module)
            if (violations.isNotEmpty()) {
                _uiState.value = state.copy(
                    errors = violations.map { it.message },
                    message = null
                ).withPreview()
                return@launch
            }
            container.moduleBuilderAgent.createOrUpdate(module)
                .onSuccess {
                    _uiState.value = ModuleBuilderUiState(message = "Module saved!").withPreview()
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = state.copy(
                        errors = listOf(error.message ?: "Unknown error"),
                        message = null
                    ).withPreview()
                }
        }
    }

    private fun parseObjectives(raw: String): List<String> {
        return raw.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty) }
    }

    private fun parseSlides(raw: String): List<String> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    private fun ModuleBuilderUiState.withPreview(): ModuleBuilderUiState {
        val objectives = parseObjectives(objectives)
        val slides = parseSlides(slides)
        val activities = generateInteractiveActivities(
            topic = topic.ifBlank { "G11 Math Module" },
            objectives = objectives,
            slides = slides,
            timePerItemSeconds = timePerItem.toIntOrNull() ?: 60,
            classroomName = classroomName.ifBlank { "${subject.ifBlank { "General Mathematics" }} Circle" }
        )
        val knowledge = activities.filter { it.isScored }.map { it.summaryLabel() }
        val opinions = activities.filterNot { it.isScored }.map { it.summaryLabel() }
        return copy(interactivePreview = InteractivePreviewSummary(knowledge, opinions))
    }

    private fun generateInteractiveActivities(
        topic: String,
        objectives: List<String>,
        slides: List<String>,
        timePerItemSeconds: Int,
        classroomName: String
    ): List<InteractiveActivity> {
        val sanitizedTopic = topic.ifBlank { "New lesson" }
        val highlightSnippets = slides.flatMap { content ->
            content.split('.', '•', '-', '\n')
                .map { it.trim() }
                .filter { it.length > 3 }
        }.distinct()
        val focusObjective = objectives.firstOrNull()?.trim().takeUnless { it.isNullOrBlank() }
            ?: sanitizedTopic
        val quizOptions = buildList {
            add(focusObjective)
            addAll(highlightSnippets)
            addAll(objectives.drop(1))
        }.filter { it.isNotBlank() }
            .distinct()
            .take(4)
            .let { options ->
                if (options.size < 4) {
                    options + List(4 - options.size) { index -> "Opsyon ${index + 1}" }
                } else {
                    options
                }
            }
        val quiz = QuizActivity(
            id = "quiz-${UUID.randomUUID()}",
            title = "Quiz blast",
            prompt = "Alin ang tumutugma sa layuning $focusObjective?",
            options = quizOptions,
            correctAnswers = listOf(0),
            allowMultiple = false
        )
        val trueFalse = TrueFalseInteractive(
            id = "tf-${UUID.randomUUID()}",
            title = "True or False",
            prompt = "$sanitizedTopic ay konektado sa $focusObjective.",
            correctAnswer = true
        )
        val typeAnswer = TypeAnswerActivity(
            id = "type-${UUID.randomUUID()}",
            title = "Type answer",
            prompt = "I-type ang keyword na nagpapatunay sa $focusObjective.",
            correctAnswer = focusObjective.take(20)
        )
        val puzzleBlocks = if (objectives.isNotEmpty()) objectives else highlightSnippets.take(4)
        val normalizedBlocks = if (puzzleBlocks.isEmpty()) {
            listOf("Define", "Solve", "Reflect")
        } else {
            puzzleBlocks
        }
        val puzzle = PuzzleActivity(
            id = "puzzle-${UUID.randomUUID()}",
            title = "Arrange the flow",
            prompt = "Ayusin ang proseso para sa $sanitizedTopic.",
            blocks = normalizedBlocks,
            correctOrder = normalizedBlocks
        )
        val slider = SliderActivity(
            id = "slider-${UUID.randomUUID()}",
            title = "Confidence slider",
            prompt = "Gaano ka kahanda na ituro ang $sanitizedTopic?",
            minValue = 0,
            maxValue = 100,
            target = timePerItemSeconds.coerceIn(10, 180)
        )
        val poll = PollActivity(
            id = "poll-${UUID.randomUUID()}",
            title = "Pulse check",
            prompt = "Anong mood ng $classroomName matapos ang aralin?",
            options = listOf("Game na!", "Medyo kulang", "Kailangan ng demo", "Maganda ang pacing")
        )
        val wordCloud = WordCloudActivity(
            id = "cloud-${UUID.randomUUID()}",
            title = "Word cloud",
            prompt = "I-describe ang $sanitizedTopic sa iisang salita.",
            maxWords = 1,
            maxCharacters = 12
        )
        val openEnded = OpenEndedActivity(
            id = "open-${UUID.randomUUID()}",
            title = "Reflection",
            prompt = "Ano ang pinaka-kailangan mong gabay tungkol sa $sanitizedTopic?",
            maxCharacters = 240
        )
        val brainstormCategories = if (objectives.isNotEmpty()) {
            objectives.take(3)
        } else {
            listOf("Ideas", "Questions", "Examples")
        }
        val brainstorm = BrainstormActivity(
            id = "brain-${UUID.randomUUID()}",
            title = "Brainstorm",
            prompt = "Mag-ambag ng ideya kung paano i-aapply ang $sanitizedTopic.",
            categories = brainstormCategories,
            voteLimit = 2
        )
        return listOf(
            quiz,
            trueFalse,
            typeAnswer,
            puzzle,
            slider,
            poll,
            wordCloud,
            openEnded,
            brainstorm
        )
    }

}
data class ModuleBuilderUiState(
    val classroomName: String = "",
    val subject: String = "G11 General Mathematics",
    val classroomDescription: String = "",
    val topic: String = "",
    val objectives: String = "LO1, LO2, LO3",
    val slides: String = "Panimula sa simple interest\nPagkuwenta ng compound interest",
    val timePerItem: String = "60",
    val interactivePreview: InteractivePreviewSummary = InteractivePreviewSummary(),
    val errors: List<String> = emptyList(),
    val message: String? = null
)

data class InteractivePreviewSummary(
    val knowledgeChecks: List<String> = emptyList(),
    val opinionPulse: List<String> = emptyList()
) {
    val total: Int get() = knowledgeChecks.size + opinionPulse.size
    val knowledgeCount: Int get() = knowledgeChecks.size
    val opinionCount: Int get() = opinionPulse.size
    fun isEmpty(): Boolean = total == 0
}

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
