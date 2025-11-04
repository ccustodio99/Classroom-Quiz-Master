package com.acme.lms.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.acme.lms.data.repo.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {
    val userId: String? = authRepo.currentUserId
}
