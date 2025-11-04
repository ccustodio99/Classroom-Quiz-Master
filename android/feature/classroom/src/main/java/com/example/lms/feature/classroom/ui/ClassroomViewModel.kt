package com.example.lms.feature.classroom.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ClassroomViewModel @Inject constructor() : ViewModel() {
    val title: String = "Classroom"
}

