package com.classroom.quizmaster.ui.feature.livesession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.LiveSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LiveSessionViewModel(
    private val container: AppContainer,
    private val moduleId: String,
    private val existingSessionId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveSessionUiState())
    val uiState: StateFlow<LiveSessionUiState> = _uiState

    private var observeJob: Job? = null
    private var sessionId: String? = null
    private var itemLookup: Map<String, ItemMeta> = emptyMap()
    init {
        viewModelScope.launch { loadModuleMeta() }
        val initialSession = existingSessionId?.takeIf { it.isNotBlank() }
            ?: container.liveSessionAgent.createSession(moduleId)
        sessionId = initialSession
        _uiState.update { it.copy(sessionCode = initialSession) }
        observeSession(initialSession)
    }

    private suspend fun loadModuleMeta() {
        val module = container.moduleRepository.getModule(moduleId)
        if (module != null) {
            val topicItems = module.lesson.topics.flatMap { topic ->
                topic.preTest.items + topic.postTest.items
            }
            val lookup = (module.preTest.items + module.postTest.items + topicItems)
                .associate { item -> item.id to ItemMeta(prompt = item.prompt, objective = item.objective) }
            itemLookup = lookup
            _uiState.update {
                it.copy(
                    moduleTopic = module.topic,
                    availableQuestions = lookup.map { (id, meta) ->
                        QuestionOption(id = id, prompt = meta.prompt, objective = meta.objective)
                    }
                )
            }
        }
    }

    private fun observeSession(targetSessionId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            var activeSessionId = targetSessionId
            val flow = try {
                container.liveSessionAgent.observe(activeSessionId)
            } catch (_: IllegalArgumentException) {
                activeSessionId = container.liveSessionAgent.createSession(moduleId)
                _uiState.update {
                    it.copy(
                        sessionCode = activeSessionId,
                        totalParticipants = 0,
                        totalResponses = 0,
                        participants = emptyList(),
                        responses = emptyList()
                    )
                }
                container.liveSessionAgent.observe(activeSessionId)
            }
            sessionId = activeSessionId
            flow.collect { snapshot ->
                updateFromSnapshot(activeSessionId, snapshot)
            }
        }
    }

    private fun updateFromSnapshot(sessionId: String, snapshot: LiveSnapshot) {
        val participants = snapshot.participants.map { student ->
            val answerCount = snapshot.answers.values.sumOf { answers ->
                answers.count { it.studentId == student.id }
            }
            ParticipantSummary(
                id = student.id,
                name = student.displayName,
                answerCount = answerCount
            )
        }.sortedWith(compareByDescending<ParticipantSummary> { it.answerCount }.thenBy { it.name.lowercase() })

        val responses = snapshot.answers.map { (itemId, answers) ->
            val meta = itemLookup[itemId]
            ItemResponseSummary(
                itemId = itemId,
                prompt = meta?.prompt ?: "Item $itemId",
                objective = meta?.objective,
                answerCounts = answers.groupingBy { it.answer }.eachCount()
            )
        }.sortedBy { it.prompt.lowercase() }

        _uiState.update { current ->
            current.copy(
                sessionCode = sessionId,
                totalParticipants = snapshot.participants.size,
                totalResponses = snapshot.answers.values.sumOf { it.size },
                participants = participants,
                responses = responses,
                activeItemId = snapshot.activeItemId,
                activePrompt = snapshot.activePrompt ?: snapshot.activeItemId?.let { itemLookup[it]?.prompt },
                activeObjective = snapshot.activeObjective ?: snapshot.activeItemId?.let { itemLookup[it]?.objective }
            )
        }
    }

    fun regenerateSession() {
        endActiveSession()
        val newSession = container.liveSessionAgent.createSession(moduleId)
        sessionId = newSession
        _uiState.update { current ->
            current.copy(
                sessionCode = newSession,
                totalParticipants = 0,
                totalResponses = 0,
                participants = emptyList(),
                responses = emptyList(),
                activeItemId = null,
                activePrompt = null,
                activeObjective = null
            )
        }
        observeSession(newSession)
    }

    fun setActiveQuestion(itemId: String?) {
        val currentSession = sessionId ?: return
        val meta = itemId?.let { itemLookup[it] }
        val success = container.liveSessionAgent.setActiveItem(
            sessionId = currentSession,
            itemId = itemId,
            prompt = meta?.prompt,
            objective = meta?.objective
        )
        if (success) {
            _uiState.update { current ->
                current.copy(
                    activeItemId = itemId,
                    activePrompt = meta?.prompt,
                    activeObjective = meta?.objective
                )
            }
        }
    }
    private fun endActiveSession() {
        val active = sessionId ?: return
        runCatching { container.liveSessionAgent.endSession(active) }
    }

    override fun onCleared() {
        endActiveSession()
        super.onCleared()
    }
}

data class LiveSessionUiState(
    val sessionCode: String? = null,
    val moduleTopic: String? = null,
    val totalParticipants: Int = 0,
    val totalResponses: Int = 0,
    val participants: List<ParticipantSummary> = emptyList(),
    val responses: List<ItemResponseSummary> = emptyList(),
    val activeItemId: String? = null,
    val activePrompt: String? = null,
    val activeObjective: String? = null,
    val availableQuestions: List<QuestionOption> = emptyList()
)

data class ParticipantSummary(
    val id: String,
    val name: String,
    val answerCount: Int
)

data class ItemResponseSummary(
    val itemId: String,
    val prompt: String,
    val objective: String?,
    val answerCounts: Map<String, Int>
)

private data class ItemMeta(
    val prompt: String,
    val objective: String?
)

data class QuestionOption(
    val id: String,
    val prompt: String,
    val objective: String?
)
