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
import com.classroom.quizmaster.domain.model.LearningMaterial
import com.classroom.quizmaster.domain.model.LearningMaterialType
import com.classroom.quizmaster.domain.model.LessonTopic
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

    fun onGradeLevelChanged(value: String) = updateState { it.copy(gradeLevel = value) }

    fun onSectionChanged(value: String) = updateState { it.copy(section = value) }

    fun onClassroomDescriptionChanged(value: String) = updateState { it.copy(classroomDescription = value) }

    fun onTopicChanged(value: String) = updateState { it.copy(topic = value) }

    fun onObjectivesChanged(value: String) = updateState { it.copy(objectives = value) }

    fun onSlidesChanged(value: String) = updateState { it.copy(slides = value) }

    fun onTimePerItemChanged(value: String) = updateState { it.copy(timePerItem = value) }

    fun addTopic() = updateState { state ->
        state.copy(topics = state.topics + LessonTopicDraft())
    }

    fun removeTopic(id: String) = updateState { state ->
        val remaining = state.topics.filterNot { it.id == id }
        state.copy(topics = remaining.ifEmpty { listOf(LessonTopicDraft()) })
    }

    fun updateTopicName(id: String, value: String) = updateTopic(id) { it.copy(name = value) }

    fun updateTopicObjectives(id: String, value: String) = updateTopic(id) { it.copy(objectives = value) }

    fun updateTopicDetails(id: String, value: String) = updateTopic(id) { it.copy(details = value) }

    fun addLearningMaterial(topicId: String) = updateTopic(topicId) { topic ->
        topic.copy(materials = topic.materials + LearningMaterialDraft())
    }

    fun removeLearningMaterial(topicId: String, materialId: String) = updateTopic(topicId) { topic ->
        topic.copy(materials = topic.materials.filterNot { it.id == materialId })
    }

    fun updateLearningMaterial(topicId: String, materialId: String, transform: (LearningMaterialDraft) -> LearningMaterialDraft) {
        updateTopic(topicId) { topic ->
            topic.copy(
                materials = topic.materials.map { material ->
                    if (material.id == materialId) transform(material) else material
                }
            )
        }
    }

    fun addPreTestQuestion(topicId: String) = updateTopic(topicId) { topic ->
        topic.copy(preTest = topic.preTest + MultipleChoiceDraft())
    }

    fun updatePreTestQuestion(
        topicId: String,
        questionId: String,
        transform: (MultipleChoiceDraft) -> MultipleChoiceDraft
    ) {
        updateTopic(topicId) { topic ->
            topic.copy(
                preTest = topic.preTest.map { question ->
                    if (question.id == questionId) transform(question) else question
                }
            )
        }
    }

    fun removePreTestQuestion(topicId: String, questionId: String) = updateTopic(topicId) { topic ->
        topic.copy(preTest = topic.preTest.filterNot { it.id == questionId })
    }

    fun addPostTestMultipleChoice(topicId: String) = updateTopic(topicId) { topic ->
        topic.copy(postTest = topic.postTest + PostTestItemDraft.MultipleChoice())
    }

    fun addPostTestTrueFalse(topicId: String) = updateTopic(topicId) { topic ->
        topic.copy(postTest = topic.postTest + PostTestItemDraft.TrueFalse())
    }

    fun addPostTestNumeric(topicId: String) = updateTopic(topicId) { topic ->
        topic.copy(postTest = topic.postTest + PostTestItemDraft.Numeric())
    }

    fun updatePostTestItem(
        topicId: String,
        itemId: String,
        transform: (PostTestItemDraft) -> PostTestItemDraft
    ) {
        updateTopic(topicId) { topic ->
            topic.copy(
                postTest = topic.postTest.map { item ->
                    if (item.id == itemId) transform(item) else item
                }
            )
        }
    }

    fun removePostTestItem(topicId: String, itemId: String) = updateTopic(topicId) { topic ->
        topic.copy(postTest = topic.postTest.filterNot { it.id == itemId })
    }

    fun addInteractiveQuiz(topicId: String) = updateTopic(topicId) { topic ->
        topic.copy(interactive = topic.interactive + InteractiveQuizDraft())
    }

    fun updateInteractiveQuiz(
        topicId: String,
        quizId: String,
        transform: (InteractiveQuizDraft) -> InteractiveQuizDraft
    ) {
        updateTopic(topicId) { topic ->
            topic.copy(
                interactive = topic.interactive.map { quiz ->
                    if (quiz.id == quizId) transform(quiz) else quiz
                }
            )
        }
    }

    fun removeInteractiveQuiz(topicId: String, quizId: String) = updateTopic(topicId) { topic ->
        topic.copy(interactive = topic.interactive.filterNot { it.id == quizId })
    }

    private fun updateTopic(id: String, transform: (LessonTopicDraft) -> LessonTopicDraft) {
        updateState { state ->
            state.copy(
                topics = state.topics.map { topic ->
                    if (topic.id == id) transform(topic) else topic
                }
            )
        }
    }

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
            val topicErrors = validateTopics(state.topics)
            if (topicErrors.isNotEmpty()) {
                _uiState.value = state.copy(
                    errors = topicErrors,
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
            val subject = state.resolvedSubject()
            val gradeLevel = state.resolvedGradeLevel()
            val section = state.resolvedSection()
            val classroomName = state.resolvedClassroomName()
            val classroom = ClassroomProfile(
                id = UUID.randomUUID().toString(),
                name = classroomName,
                subject = subject,
                description = state.resolvedDescription(subject, gradeLevel, section),
                gradeLevel = gradeLevel,
                section = section
            )
            val interactiveActivities = generateInteractiveActivities(
                topic = topic,
                objectives = objectives,
                slides = slides,
                timePerItemSeconds = timePerItemSeconds,
                classroomName = classroomName
            )
            val defaultObjective = objectives.firstOrNull() ?: topic
            val lessonTopics = state.topics.mapIndexed { index, draft ->
                draft.toLessonTopic(
                    index = index,
                    timePerItemSeconds = timePerItemSeconds,
                    defaultObjective = defaultObjective
                )
            }
            val combinedObjectives = (objectives + lessonTopics.flatMap { it.learningObjectives })
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val module = Module(
                id = UUID.randomUUID().toString(),
                classroom = classroom,
                subject = subject,
                topic = topic,
                objectives = combinedObjectives,
                preTest = Assessment(
                    id = UUID.randomUUID().toString(),
                    items = preItems,
                    timePerItemSec = timePerItemSeconds
                ),
                lesson = Lesson(
                    id = UUID.randomUUID().toString(),
                    slides = buildLessonSlides(slides, objectives),
                    interactiveActivities = interactiveActivities,
                    topics = lessonTopics
                ),
                postTest = Assessment(
                    id = UUID.randomUUID().toString(),
                    items = postItems,
                    timePerItemSec = timePerItemSeconds
                ),
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
        return raw.split(',', '\n')
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .distinct()
    }

    private fun parseSlides(raw: String): List<String> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    private fun parseTopicObjectives(raw: String): List<String> {
        return raw.split(',', '\n')
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .distinct()
    }

    private fun validateTopics(topics: List<LessonTopicDraft>): List<String> {
        if (topics.isEmpty()) {
            return listOf("Magdagdag ng hindi bababa sa isang lesson topic.")
        }
        val errors = mutableListOf<String>()
        topics.forEachIndexed { index, topic ->
            val topicLabel = "Paksa ${index + 1}"
            if (topic.name.isBlank()) {
                errors += "$topicLabel: Magbigay ng pangalan ng paksa."
            }
            if (topic.details.isBlank()) {
                errors += "$topicLabel: Magdagdag ng lesson details."
            }
            val objectives = parseTopicObjectives(topic.objectives)
            if (objectives.isEmpty()) {
                errors += "$topicLabel: Magdagdag ng learning objective."
            }
            topic.materials.forEach { material ->
                errors += validateLearningMaterial(material, topicLabel)
            }
            if (topic.preTest.isEmpty()) {
                errors += "$topicLabel: Magdagdag ng kahit isang pre-test question."
            }
            topic.preTest.forEachIndexed { qIndex, question ->
                errors += validateMultipleChoice(question, "$topicLabel pre-test ${qIndex + 1}")
            }
            if (topic.postTest.isEmpty()) {
                errors += "$topicLabel: Magdagdag ng kahit isang post-test item."
            }
            topic.postTest.forEachIndexed { qIndex, item ->
                errors += validatePostTestItem(item, "$topicLabel post-test ${qIndex + 1}")
            }
            if (topic.interactive.isEmpty()) {
                errors += "$topicLabel: Magdagdag ng interactive quiz para sa learners."
            }
            topic.interactive.forEachIndexed { qIndex, quiz ->
                errors += validateInteractiveQuiz(quiz, "$topicLabel interactive ${qIndex + 1}")
            }
        }
        return errors
    }

    private fun validateLearningMaterial(material: LearningMaterialDraft, topicLabel: String): List<String> {
        if (material.title.isBlank() && material.reference.isBlank()) {
            return emptyList()
        }
        val errors = mutableListOf<String>()
        if (material.title.isBlank()) {
            errors += "$topicLabel: Lagyan ng pamagat ang learning material."
        }
        if (material.reference.isBlank()) {
            errors += "$topicLabel: Magbigay ng link o file reference para sa ${material.title.ifBlank { "learning material" }}."
        }
        return errors
    }

    private fun validateMultipleChoice(question: MultipleChoiceDraft, label: String): List<String> {
        val errors = mutableListOf<String>()
        if (question.prompt.isBlank()) {
            errors += "$label: Ilagay ang tanong."
        }
        val filledChoices = question.choices.mapIndexed { index, choice -> index to choice.trim() }
        if (filledChoices.count { it.second.isNotEmpty() } < 2) {
            errors += "$label: Magdagdag ng hindi bababa sa dalawang pagpipilian."
        }
        if (question.correctIndex !in question.choices.indices) {
            errors += "$label: Piliin kung alin ang tamang sagot."
        } else if (question.choices[question.correctIndex].isBlank()) {
            errors += "$label: Punan ang tamang sagot."
        }
        return errors
    }

    private fun validatePostTestItem(item: PostTestItemDraft, label: String): List<String> = when (item) {
        is PostTestItemDraft.MultipleChoice -> validateMultipleChoice(item.asMultipleChoiceDraft(), label)
        is PostTestItemDraft.TrueFalse -> buildList {
            if (item.prompt.isBlank()) add("$label: Ilagay ang tanong para sa True/False.")
        }
        is PostTestItemDraft.Numeric -> buildList {
            if (item.prompt.isBlank()) add("$label: Ilagay ang problem statement.")
            if (item.answer.toDoubleOrNull() == null) add("$label: Ibigay ang tamang numerong sagot.")
        }
    }

    private fun validateInteractiveQuiz(quiz: InteractiveQuizDraft, label: String): List<String> {
        val errors = mutableListOf<String>()
        if (quiz.prompt.isBlank()) {
            errors += "$label: Ilagay ang prompt."
        }
        val filled = quiz.options.mapIndexed { index, option -> index to option.trim() }
        if (filled.count { it.second.isNotEmpty() } < 2) {
            errors += "$label: Magdagdag ng hindi bababa sa dalawang opsyon."
        }
        if (quiz.correctAnswers.isEmpty()) {
            errors += "$label: Tukuyin ang tamang sagot."
        } else {
            val invalidIndex = quiz.correctAnswers.firstOrNull { it !in quiz.options.indices }
            if (invalidIndex != null) {
                errors += "$label: May tamang sagot na wala sa mga pagpipilian."
            }
            val blankCorrect = quiz.correctAnswers.firstOrNull { index ->
                index in quiz.options.indices && quiz.options[index].isBlank()
            }
            if (blankCorrect != null) {
                errors += "$label: Punan ang tamang opsyon bago piliin."
            }
            if (!quiz.allowMultiple && quiz.correctAnswers.size > 1) {
                errors += "$label: Isang tamang sagot lang ang pinapayagan sa single answer mode."
            }
        }
        return errors
    }

    private fun ModuleBuilderUiState.withPreview(): ModuleBuilderUiState {
        val objectives = parseObjectives(objectives)
        val slides = parseSlides(slides)
        val activities = generateInteractiveActivities(
            topic = topic.ifBlank { "G11 Math Module" },
            objectives = objectives,
            slides = slides,
            timePerItemSeconds = timePerItem.toIntOrNull() ?: 60,
            classroomName = resolvedClassroomName()
        )
        val knowledge = activities.filter { it.isScored }.map { it.summaryLabel() }
        val opinions = activities.filterNot { it.isScored }.map { it.summaryLabel() }
        val topicInteractiveCount = topics.sumOf { it.interactive.size }
        return copy(interactivePreview = InteractivePreviewSummary(knowledge, opinions, topicInteractiveCount))
    }

    private fun ModuleBuilderUiState.resolvedSubject(): String {
        return subject.ifBlank { "G11 General Mathematics" }
    }

    private fun ModuleBuilderUiState.resolvedGradeLevel(): String {
        return gradeLevel.ifBlank { "Grade 11" }
    }

    private fun ModuleBuilderUiState.resolvedSection(): String {
        return section.trim()
    }

    private fun ModuleBuilderUiState.resolvedClassroomName(): String {
        val subject = resolvedSubject()
        val gradeLevel = resolvedGradeLevel()
        val section = resolvedSection()
        val fallback = buildString {
            append(gradeLevel)
            if (section.isNotEmpty()) {
                append(' ')
                append(section)
            }
        }.ifBlank { subject }
        val defaultName = if (fallback.isNotBlank()) fallback else "$subject Circle"
        return classroomName.ifBlank { defaultName }
    }

    private fun ModuleBuilderUiState.resolvedDescription(
        subject: String,
        gradeLevel: String,
        section: String
    ): String {
        if (classroomDescription.isNotBlank()) return classroomDescription
        val base = "Learning circle for $gradeLevel $subject".replace(Regex("\\s+"), " ").trim()
        return if (section.isNotEmpty()) {
            "$base - Section $section"
        } else {
            base
        }
    }

    private fun buildLessonSlides(slides: List<String>, objectives: List<String>): List<LessonSlide> {
        if (slides.isEmpty()) return emptyList()
        val normalizedObjectives = objectives.filter { it.isNotBlank() }
        return slides.mapIndexed { index, text ->
            val objective = normalizedObjectives.getOrNull(index % maxOf(normalizedObjectives.size, 1))
            LessonSlide(
                id = "slide-${index + 1}",
                title = "Slide ${index + 1}",
                content = text,
                miniCheck = MiniCheck(
                    prompt = buildMiniCheckPrompt(objective),
                    correctAnswer = extractMiniCheckAnswer(text),
                    objectives = objective?.let { listOf(it) } ?: emptyList()
                )
            )
        }
    }

    private fun buildMiniCheckPrompt(objective: String?): String {
        return if (objective.isNullOrBlank()) {
            "Ano ang takeaway mula sa bahaging ito?"
        } else {
            "Ibahagi ang pangunahing ideya para sa layunin $objective."
        }
    }

    private fun extractMiniCheckAnswer(text: String): String {
        val normalized = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return ""
        val sentences = normalized.split(Regex("(?<=[.!?]) "))
        val preferred = sentences.firstOrNull { it.length >= 40 }
            ?: sentences.maxByOrNull { it.length }
            ?: normalized
        return preferred.trim().take(160)
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
        val sliderMin = 0
        val sliderMax = 100
        val slider = SliderActivity(
            id = "slider-${UUID.randomUUID()}",
            title = "Confidence slider",
            prompt = "Gaano ka kahanda na ituro ang $sanitizedTopic?",
            minValue = sliderMin,
            maxValue = sliderMax,
            target = timePerItemSeconds.coerceIn(sliderMin, sliderMax)
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

    private fun LessonTopicDraft.toLessonTopic(
        index: Int,
        timePerItemSeconds: Int,
        defaultObjective: String
    ): LessonTopic {
        val topicId = id.ifBlank { UUID.randomUUID().toString() }
        val parsedObjectives = parseTopicObjectives(objectives).ifEmpty { listOf(defaultObjective) }
        val focusObjective = parsedObjectives.firstOrNull() ?: defaultObjective
        val materialsDomain = materials.mapNotNull { it.toLearningMaterialOrNull(topicId) }
        val preItems = preTest.mapIndexed { itemIndex, question ->
            question.toItem(
                objective = focusObjective,
                fallbackPromptIndex = itemIndex,
                itemPrefix = "$topicId-pre"
            )
        }
        val postItems = postTest.mapIndexed { itemIndex, draft ->
            draft.toItem(
                objective = focusObjective,
                fallbackPromptIndex = itemIndex,
                itemPrefix = "$topicId-post"
            )
        }
        val interactiveActivities = interactive.mapIndexed { sequence, quiz ->
            quiz.toActivity(
                topicId = topicId,
                sequence = sequence,
                fallbackTitle = name.ifBlank { "Topic ${index + 1}" }
            )
        }
        return LessonTopic(
            id = topicId,
            name = name.ifBlank { "Topic ${index + 1}" },
            learningObjectives = parsedObjectives,
            details = details.trim(),
            materials = materialsDomain,
            preTest = Assessment(id = "$topicId-pre", items = preItems, timePerItemSec = timePerItemSeconds),
            postTest = Assessment(id = "$topicId-post", items = postItems, timePerItemSec = timePerItemSeconds),
            interactiveAssessments = interactiveActivities
        )
    }

    private fun LearningMaterialDraft.toLearningMaterialOrNull(topicId: String): LearningMaterial? {
        if (title.isBlank() && reference.isBlank()) return null
        val normalizedId = id.ifBlank { UUID.randomUUID().toString() }
        return LearningMaterial(
            id = "$topicId-material-$normalizedId",
            title = title.ifBlank { reference.ifBlank { "Learning material" } },
            type = type,
            reference = reference.trim()
        )
    }

    private fun MultipleChoiceDraft.toItem(
        objective: String,
        fallbackPromptIndex: Int,
        itemPrefix: String
    ): MultipleChoiceItem {
        val normalizedId = id.ifBlank { UUID.randomUUID().toString() }
        val baseChoices = choices.map { it.trim() }
        val padded = if (baseChoices.size >= 4) baseChoices else baseChoices + List(4 - baseChoices.size) { "" }
        val resolvedChoices = padded.mapIndexed { index, choice ->
            if (choice.isNotEmpty()) choice else "Opsyon ${index + 1}"
        }
        val safeCorrectIndex = correctIndex.coerceIn(0, resolvedChoices.lastIndex)
        return MultipleChoiceItem(
            id = "$itemPrefix-$normalizedId",
            objective = objective,
            prompt = prompt.ifBlank { "Tanong ${fallbackPromptIndex + 1}" },
            choices = resolvedChoices,
            correctIndex = safeCorrectIndex,
            explanation = rationale.ifBlank { "Teacher-authored item." }
        )
    }

    private fun PostTestItemDraft.toItem(
        objective: String,
        fallbackPromptIndex: Int,
        itemPrefix: String
    ): Item = when (this) {
        is PostTestItemDraft.MultipleChoice ->
            this.asMultipleChoiceDraft().toItem(objective, fallbackPromptIndex, itemPrefix)
        is PostTestItemDraft.TrueFalse -> {
            val normalizedId = id.ifBlank { UUID.randomUUID().toString() }
            TrueFalseItem(
                id = "$itemPrefix-$normalizedId",
                objective = objective,
                prompt = prompt.ifBlank { "True or False ${fallbackPromptIndex + 1}" },
                answer = answer,
                explanation = explanation.ifBlank { "Teacher-authored true/false item." }
            )
        }
        is PostTestItemDraft.Numeric -> {
            val normalizedId = id.ifBlank { UUID.randomUUID().toString() }
            val parsedAnswer = answer.toDoubleOrNull() ?: 0.0
            val parsedTolerance = tolerance.toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.01
            NumericItem(
                id = "$itemPrefix-$normalizedId",
                objective = objective,
                prompt = prompt.ifBlank { "Problem ${fallbackPromptIndex + 1}" },
                answer = parsedAnswer,
                tolerance = parsedTolerance,
                explanation = explanation.ifBlank { "Teacher-authored numeric item." }
            )
        }
    }

    private fun PostTestItemDraft.MultipleChoice.asMultipleChoiceDraft(): MultipleChoiceDraft {
        return MultipleChoiceDraft(
            id = id,
            prompt = prompt,
            choices = choices,
            correctIndex = correctIndex,
            rationale = rationale
        )
    }

    private fun InteractiveQuizDraft.toActivity(
        topicId: String,
        sequence: Int,
        fallbackTitle: String
    ): InteractiveActivity {
        val normalizedId = id.ifBlank { UUID.randomUUID().toString() }
        val trimmedOptions = options.map { it.trim() }
        val padded = if (trimmedOptions.size >= 4) trimmedOptions else trimmedOptions + List(4 - trimmedOptions.size) { "" }
        val resolvedOptions = padded.mapIndexed { index, option ->
            if (option.isNotEmpty()) option else "Opsyon ${index + 1}"
        }
        val normalizedAnswers = if (allowMultiple) {
            if (correctAnswers.isEmpty()) setOf(0) else correctAnswers
        } else {
            setOf(correctAnswers.firstOrNull() ?: 0)
        }
        val clampedAnswers = normalizedAnswers.map { it.coerceIn(0, resolvedOptions.lastIndex) }
        return QuizActivity(
            id = "quiz-$topicId-$normalizedId",
            title = title.ifBlank { "$fallbackTitle Quiz" },
            prompt = prompt.ifBlank { "Sagutan ang tanong ${sequence + 1}." },
            options = resolvedOptions,
            correctAnswers = clampedAnswers,
            allowMultiple = allowMultiple,
            isScored = true
        )
    }
}

data class ModuleBuilderUiState(
    val classroomName: String = "",
    val subject: String = "G11 General Mathematics",
    val gradeLevel: String = "Grade 11",
    val section: String = "",
    val classroomDescription: String = "",
    val topic: String = "",
    val objectives: String = "LO1, LO2, LO3",
    val slides: String = "Panimula sa simple interest\nPagkuwenta ng compound interest",
    val timePerItem: String = "60",
    val topics: List<LessonTopicDraft> = listOf(LessonTopicDraft()),
    val interactivePreview: InteractivePreviewSummary = InteractivePreviewSummary(),
    val errors: List<String> = emptyList(),
    val message: String? = null
)

data class InteractivePreviewSummary(
    val knowledgeChecks: List<String> = emptyList(),
    val opinionPulse: List<String> = emptyList(),
    val topicInteractiveCount: Int = 0
) {
    val total: Int get() = knowledgeChecks.size + opinionPulse.size
    val knowledgeCount: Int get() = knowledgeChecks.size
    val opinionCount: Int get() = opinionPulse.size
    fun isEmpty(): Boolean = total == 0
}

data class LessonTopicDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val objectives: String = "",
    val details: String = "",
    val materials: List<LearningMaterialDraft> = listOf(LearningMaterialDraft()),
    val preTest: List<MultipleChoiceDraft> = listOf(MultipleChoiceDraft()),
    val postTest: List<PostTestItemDraft> = listOf(PostTestItemDraft.MultipleChoice()),
    val interactive: List<InteractiveQuizDraft> = listOf(InteractiveQuizDraft())
)

data class LearningMaterialDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val type: LearningMaterialType = LearningMaterialType.Document,
    val reference: String = ""
)

data class MultipleChoiceDraft(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String = "",
    val choices: List<String> = List(4) { "" },
    val correctIndex: Int = 0,
    val rationale: String = ""
)

sealed interface PostTestItemDraft {
    val id: String

    data class MultipleChoice(
        override val id: String = UUID.randomUUID().toString(),
        val prompt: String = "",
        val choices: List<String> = List(4) { "" },
        val correctIndex: Int = 0,
        val rationale: String = ""
    ) : PostTestItemDraft

    data class TrueFalse(
        override val id: String = UUID.randomUUID().toString(),
        val prompt: String = "",
        val answer: Boolean = true,
        val explanation: String = ""
    ) : PostTestItemDraft

    data class Numeric(
        override val id: String = UUID.randomUUID().toString(),
        val prompt: String = "",
        val answer: String = "",
        val tolerance: String = "0.5",
        val explanation: String = ""
    ) : PostTestItemDraft
}

data class InteractiveQuizDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val prompt: String = "",
    val options: List<String> = List(4) { "" },
    val correctAnswers: Set<Int> = setOf(0),
    val allowMultiple: Boolean = false
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
