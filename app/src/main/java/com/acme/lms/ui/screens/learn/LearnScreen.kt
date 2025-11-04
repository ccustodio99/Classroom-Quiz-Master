package com.acme.lms.ui.screens.learn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.acme.lms.ui.viewmodel.LearnViewModel

@Composable
fun LearnScreen(
    navController: NavController,
    viewModel: LearnViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Browse learning catalog (coming soon)")
    }
}
