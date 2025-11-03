package com.classroom.quizmaster.ui.feature.classroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun ClassroomEditorDialog(
    state: ClassroomEditorState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdate: (ClassroomEditorState) -> Unit
) {
    var name by remember(state.id) { mutableStateOf(state.name) }
    var subject by remember(state.id) { mutableStateOf(state.subject) }
    var grade by remember(state.id) { mutableStateOf(state.gradeLevel) }
    var section by remember(state.id) { mutableStateOf(state.section) }
    var description by remember(state.id) { mutableStateOf(state.description) }
    var archived by remember(state.id) { mutableStateOf(state.archived) }

    fun emitUpdatedState(
        nameValue: String = name,
        subjectValue: String = subject,
        gradeValue: String = grade,
        sectionValue: String = section,
        descriptionValue: String = description,
        archivedValue: Boolean = archived
    ) {
        onUpdate(
            state.copy(
                name = nameValue,
                subject = subjectValue,
                gradeLevel = gradeValue,
                section = sectionValue,
                description = descriptionValue,
                archived = archivedValue
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (state.name.isBlank()) "New class" else "Edit class") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        emitUpdatedState(nameValue = it)
                    },
                    label = { Text("Class name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        emitUpdatedState(subjectValue = it)
                    },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = {
                        grade = it
                        emitUpdatedState(gradeValue = it)
                    },
                    label = { Text("Grade level") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = {
                        section = it
                        emitUpdatedState(sectionValue = it)
                    },
                    label = { Text("Section") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        emitUpdatedState(descriptionValue = it)
                    },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                FilterChip(
                    selected = archived,
                    onClick = {
                        archived = !archived
                        emitUpdatedState(archivedValue = archived)
                    },
                    label = { Text(text = if (archived) "Archived" else "Active") }
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
