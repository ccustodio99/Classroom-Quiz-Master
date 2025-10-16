package com.classroom.quizmaster.ui.feature.detail

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.Module
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModuleDetailViewModel(
    private val container: AppContainer,
    private val moduleId: String,
    private val snackbarHostState: SnackbarHostState
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModuleDetailUiState())
    val uiState: StateFlow<ModuleDetailUiState> = _uiState

    init {
        loadModule()
    }

    private fun loadModule() {
        viewModelScope.launch {
            val module = container.moduleRepository.getModule(moduleId)
            _uiState.value = ModuleDetailUiState(module = module)
        }
    }

    fun createLiveSession() {
        val module = _uiState.value.module ?: return
        val code = container.liveSessionAgent.createSession(module.id)
        _uiState.value = _uiState.value.copy(liveSessionCode = code)
    }

    fun assignHomework() {
        val module = _uiState.value.module ?: return
        val due = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L
        val assignment = container.assignmentAgent.assign(module.id, due)
        viewModelScope.launch {
            snackbarHostState.showSnackbar("Assignment created: ${assignment.id}")
        }
    }

    fun syncToCloud() {
        viewModelScope.launch {
            val result = container.syncAgent.pushModule(moduleId)
            snackbarHostState.showSnackbar(
                result.fold(
                    onSuccess = { "Module synced" },
                    onFailure = { "Sync failed: ${it.message}" }
                )
            )
        }
    }
}

data class ModuleDetailUiState(
    val module: Module? = null,
    val liveSessionCode: String? = null
)
