package com.classroom.quizmaster.ui.student.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.PrimaryButton

@Composable
fun StudentProfileRoute(
    onLoggedOut: () -> Unit,
    viewModel: StudentProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentProfileScreen(
        state = state,
        onLogout = {
            viewModel.logout()
            onLoggedOut()
        }
    )
}

@Composable
fun StudentProfileScreen(
    state: StudentProfileUiState,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineSmall)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = state.fullName, style = MaterialTheme.typography.titleMedium)
                Text(text = state.email.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Progress", style = MaterialTheme.typography.titleMedium)
                Divider()
                Text("Classrooms: ${state.classroomsCount}")
                Text("Assignments done: ${state.completedAssignments}")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = "Sign out",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
