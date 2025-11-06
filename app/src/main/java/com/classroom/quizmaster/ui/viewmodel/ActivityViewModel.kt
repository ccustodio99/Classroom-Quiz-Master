package com.classroom.quizmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.classroom.quizmaster.agents.LiveSessionAgent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val liveSessionAgent: LiveSessionAgent
) : ViewModel()
