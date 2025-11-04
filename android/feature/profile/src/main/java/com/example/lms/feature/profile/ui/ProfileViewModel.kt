package com.example.lms.feature.profile.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class OfflineDownload(
    val title: String,
    val sizeMb: Int,
)

data class PreferenceItem(
    val label: String,
    val value: String,
)

data class ProfileUiState(
    val displayName: String,
    val email: String,
    val role: String,
    val org: String,
    val downloads: List<OfflineDownload>,
    val preferences: List<PreferenceItem>,
)

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {
    val uiState = ProfileUiState(
        displayName = "Alex Kim",
        email = "alex.kim@example.org",
        role = "Learner",
        org = "Riverdale High",
        downloads = listOf(
            OfflineDownload("Biology Lab • Light Reactions", 45),
            OfflineDownload("World History • Primary Source Pack", 32),
        ),
        preferences = listOf(
            PreferenceItem("Reminder cadence", "Daily at 4 PM"),
            PreferenceItem("Adaptive difficulty", "Balanced"),
            PreferenceItem("Offline sync", "Wi-Fi only"),
        ),
    )
}

