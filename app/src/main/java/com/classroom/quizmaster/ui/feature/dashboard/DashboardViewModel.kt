package com.classroom.quizmaster.ui.feature.dashboard

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.Assessment
import com.classroom.quizmaster.domain.model.Lesson
import com.classroom.quizmaster.domain.model.LessonSlide
import com.classroom.quizmaster.domain.model.MiniCheck
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.ModuleSettings
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val container: AppContainer,
    private val snackbarHostState: SnackbarHostState
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        observeModules()
    }

    private fun observeModules() {
        viewModelScope.launch {
            container.moduleRepository.observeModules().collect { modules ->
                if (modules.isEmpty()) {
                    seedSampleModule()
                    return@collect
                }
                val summaries = modules.map { module ->
                    ModuleSummary(
                        id = module.id,
                        topic = module.topic,
                        objectives = module.objectives,
                        attempts = null,
                        preAverage = null,
                        postAverage = null
                    )
                }
                _uiState.value = DashboardUiState(modules = summaries)
            }
        }
    }

    fun createQuickModule() {
        viewModelScope.launch { buildSampleModule(showMessage = true) }
    }

    private suspend fun seedSampleModule() {
        buildSampleModule(showMessage = false)
    }

    private suspend fun buildSampleModule(showMessage: Boolean) {
        val items = container.itemBankAgent.query(listOf("LO1", "LO2", "LO3"), limit = 4)
        val module = Module(
            id = UUID.randomUUID().toString(),
            topic = "Simple & Compound Interest",
            objectives = listOf("LO1", "LO2", "LO3"),
            preTest = Assessment(id = UUID.randomUUID().toString(), items = items),
            lesson = Lesson(
                id = UUID.randomUUID().toString(),
                slides = listOf(
                    LessonSlide(
                        id = "slide-1",
                        title = "Pagsusulit Bago ang Aralin",
                        content = "Balikan ang konsepto ng interest.",
                        miniCheck = MiniCheck("Ano ang formula ng simple interest?", "I=Prt")
                    ),
                    LessonSlide(
                        id = "slide-2",
                        title = "Talakayan",
                        content = "Ipakita ang pagkakaiba ng simple at compound interest.",
                        miniCheck = MiniCheck("Compound interest formula?", "A=P(1+r/m)^{mt}")
                    )
                )
            ),
            postTest = Assessment(id = UUID.randomUUID().toString(), items = items.shuffled()),
            settings = ModuleSettings(allowLeaderboard = true)
        )
        container.moduleBuilderAgent.createOrUpdate(module).onSuccess {
            if (showMessage) {
                snackbarHostState.showSnackbar("Handa na ang module!", withDismissAction = false)
            }
        }.onFailure {
            snackbarHostState.showSnackbar("Hindi naisave ang module: ${it.message}")
        }
    }
}

data class DashboardUiState(
    val modules: List<ModuleSummary> = emptyList()
)

data class ModuleSummary(
    val id: String,
    val topic: String,
    val objectives: List<String>,
    val attempts: Int?,
    val preAverage: Double?,
    val postAverage: Double?
)
