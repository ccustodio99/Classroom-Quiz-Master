package com.classroom.quizmaster.ui.teacher.classrooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton

@Composable
fun CreateClassroomRoute(
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    CreateClassroomScreen(
        name = name,
        grade = grade,
        subject = subject,
        onNameChanged = { name = it },
        onGradeChanged = { grade = it },
        onSubjectChanged = { subject = it },
        onSave = {
            // TODO: actually persist the classroom
            onDone()
        },
        onBack = onDone
    )
}

@Composable
fun CreateClassroomScreen(
    name: String,
    grade: String,
    subject: String,
    onNameChanged: (String) -> Unit,
    onGradeChanged: (String) -> Unit,
    onSubjectChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val canSave = name.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Set up your first classroom",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Classrooms group topics, quizzes, live sessions, and assignments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Classroom name") },
            singleLine = true
        )

        OutlinedTextField(
            value = grade,
            onValueChange = onGradeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Grade (optional)") },
            singleLine = true
        )

        OutlinedTextField(
            value = subject,
            onValueChange = onSubjectChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subject (optional)") },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Save classroom",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = canSave
            )
        }
    }
}
