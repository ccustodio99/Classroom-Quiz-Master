package com.acme.lms.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.acme.lms.agents.LiveSessionAgent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val liveSessionAgent: LiveSessionAgent
) : ViewModel()
