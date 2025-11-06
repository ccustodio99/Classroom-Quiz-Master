package com.classroom.quizmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.classroom.quizmaster.agents.ClassworkAgent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val classworkAgent: ClassworkAgent
) : ViewModel()
