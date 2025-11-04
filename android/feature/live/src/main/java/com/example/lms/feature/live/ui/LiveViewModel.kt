package com.example.lms.feature.live.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LiveViewModel @Inject constructor() : ViewModel() {
    val title: String = "Live"
}

