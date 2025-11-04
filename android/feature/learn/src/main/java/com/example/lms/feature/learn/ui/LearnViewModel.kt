package com.example.lms.feature.learn.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LearnViewModel @Inject constructor() : ViewModel() {
    val title: String = "Learn"
}

