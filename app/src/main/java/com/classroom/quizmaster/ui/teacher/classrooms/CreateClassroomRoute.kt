package com.classroom.quizmaster.ui.teacher.classrooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.PrimaryButton

@Composable
fun CreateClassroomRoute(
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Set up your first classroom",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Classrooms group topics, quizzes, live sessions, and assignments.",
            style = MaterialTheme.typography.bodyLarge
        )
        PrimaryButton(
            text = "Save classroom",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
