package com.classroom.quizmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.agents.ClassroomAgent
import com.classroom.quizmaster.data.model.Roster
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassroomViewModel @Inject constructor(
    private val classroomAgent: ClassroomAgent
) : ViewModel() {

    private val _roster = MutableStateFlow<List<Roster>>(emptyList())
    val roster: StateFlow<List<Roster>> = _roster

    fun loadRoster(classPath: String) {
        viewModelScope.launch {
            _roster.value = classroomAgent.getRoster(classPath)
        }
    }
}
