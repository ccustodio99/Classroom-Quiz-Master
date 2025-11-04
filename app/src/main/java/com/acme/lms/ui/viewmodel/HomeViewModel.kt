package com.acme.lms.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.lms.data.model.Classwork
import com.acme.lms.data.repo.ClassworkRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val classworkRepo: ClassworkRepo
) : ViewModel() {

    private val _todayTasks = MutableStateFlow<List<Classwork>>(emptyList())
    val todayTasks: StateFlow<List<Classwork>> = _todayTasks

    fun load(classPath: String) {
        viewModelScope.launch {
            _todayTasks.value = classworkRepo.listOnce(classPath)
        }
    }
}
