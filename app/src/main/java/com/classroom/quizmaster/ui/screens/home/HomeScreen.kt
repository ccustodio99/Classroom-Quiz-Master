package com.classroom.quizmaster.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.classroom.quizmaster.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val tasks by viewModel.todayTasks.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Continue learning", style = MaterialTheme.typography.titleLarge)
        val first = remember(tasks) { tasks.firstOrNull() }
        if (first != null) {
            Button(onClick = { navController.navigate("firstLesson/${first.classId}/${first.id}") }) {
                Text("Start next lesson (<2 min)")
            }
        } else {
            Text("No tasks yet. Explore the catalog →")
        }
    }
}
