package com.classroom.quizmaster.ui.neutral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.data.demo.OfflineDemoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface OfflineDemoEvent {
    data object Success : OfflineDemoEvent
    data class Error(val message: String) : OfflineDemoEvent
}

@HiltViewModel
class NeutralWelcomeViewModel @Inject constructor(
    private val offlineDemoManager: OfflineDemoManager
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _events = MutableSharedFlow<OfflineDemoEvent>()
    val events: SharedFlow<OfflineDemoEvent> = _events

    fun enableOfflineDemo() {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            offlineDemoManager.enableDemoMode()
                .onSuccess { _events.emit(OfflineDemoEvent.Success) }
                .onFailure { error ->
                    _events.emit(
                        OfflineDemoEvent.Error(error.message ?: "Unable to prepare offline demo.")
                    )
                }
            _loading.value = false
        }
    }
}
