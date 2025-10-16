package com.classroom.quizmaster.ui.feature.dashboard

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.sample.GeneralMathSampleCatalog
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
        viewModelScope.launch { buildSampleModules(showMessage = true) }
    }

    private suspend fun seedSampleModule() {
        buildSampleModules(showMessage = false)
    }

    private suspend fun buildSampleModules(showMessage: Boolean) {
        val modules = GeneralMathSampleCatalog.modules()
        var savedCount = 0
        val failedTopics = mutableListOf<String>()

        modules.forEach { module ->
            container.moduleBuilderAgent.createOrUpdate(module)
                .onSuccess { savedCount += 1 }
                .onFailure { failedTopics += module.topic }
        }

        if (showMessage) {
            if (savedCount > 0) {
                snackbarHostState.showSnackbar(
                    message = "Sample packs dropped! ✨",
                    withDismissAction = false
                )
            }
            if (failedTopics.isNotEmpty()) {
                snackbarHostState.showSnackbar(
                    message = "Hindi naisave ang: ${failedTopics.joinToString()}"
                )
            }
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
