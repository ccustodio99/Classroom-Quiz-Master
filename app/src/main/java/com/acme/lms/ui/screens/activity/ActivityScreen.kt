package com.acme.lms.ui.screens.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.acme.lms.ui.viewmodel.ActivityViewModel

@Composable
fun ActivityScreen(
    navController: NavController,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Live sessions & in-class activities")
    }
}
