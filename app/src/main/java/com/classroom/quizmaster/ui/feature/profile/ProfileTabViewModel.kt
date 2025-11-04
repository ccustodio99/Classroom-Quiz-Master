package com.classroom.quizmaster.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileTabViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val syncAgent = container.syncAgent
    private val moduleRepository = container.moduleRepository

    private val _uiState = MutableStateFlow(ProfileTabUiState())
    val uiState: StateFlow<ProfileTabUiState> = _uiState.asStateFlow()

    fun pushAllModules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.InProgress, message = "Uploading modules…")
            val modules = moduleRepository.listModules()
            modules.forEach { module ->
                syncAgent.pushModule(module.id).onFailure { error ->
                    _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Error, message = error.localizedMessage)
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Success, message = "Modules synced to Firebase.")
        }
    }

    fun pullUpdates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.InProgress, message = "Checking for updates…")
            val result = syncAgent.pullUpdates()
            result.onSuccess { count ->
                val label = if (count > 0) "Pulled $count module updates." else "No new updates."
                _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Success, message = label)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Error, message = error.localizedMessage)
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class ProfileTabUiState(
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val message: String? = null
)

