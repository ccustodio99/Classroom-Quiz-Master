package com.classroom.quizmaster.ui.feature.dashboard

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.sample.GeneralMathSampleCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val container: AppContainer,
    private val snackbarHostState: SnackbarHostState,
    private val teacherId: String?,
    private val classroomId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        observeModules()
    }

    private fun observeModules() {
        viewModelScope.launch {
            val flow = classroomId?.let { id ->
                container.moduleRepository.observeModulesByClassroom(id)
            } ?: container.moduleRepository.observeModules()

            flow.collect { modules ->
                val filtered = filterForTeacher(modules)
                if (filtered.isEmpty()) {
                    if (teacherId != null) {
                        seedSampleModules()
                    } else {
                        _uiState.value = DashboardUiState()
                    }
                    return@collect
                }
                val summaries = filtered.map { module ->
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
        if (teacherId == null) {
            viewModelScope.launch {
                snackbarHostState.showSnackbar("Only teachers can create modules.")
            }
            return
        }
        viewModelScope.launch { buildSampleModules(showMessage = true) }
    }

    private suspend fun seedSampleModules() {
        if (teacherId != null) {
            buildSampleModules(showMessage = false)
        }
    }

    private suspend fun buildSampleModules(showMessage: Boolean) {
        val owner = teacherId ?: return
        val modules = GeneralMathSampleCatalog.modules().map { module ->
            module.copy(classroom = module.classroom.copy(ownerId = owner))
        }
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
                    message = "Sample modules created.",
                    withDismissAction = false
                )
            }
            if (failedTopics.isNotEmpty()) {
                snackbarHostState.showSnackbar(
                    message = "Hindi naisave ang: {failedTopics.joinToString()}"
                )
            }
        }
    }

    private fun filterForTeacher(modules: List<Module>): List<Module> {
        val byTeacher = teacherId?.let { ownerId -> modules.filter { it.classroom.ownerId == ownerId } } ?: modules
        return classroomId?.let { id -> byTeacher.filter { it.classroom.id == id } } ?: byTeacher
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
