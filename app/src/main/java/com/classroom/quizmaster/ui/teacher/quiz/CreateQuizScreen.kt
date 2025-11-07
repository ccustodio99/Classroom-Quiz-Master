package com.classroom.quizmaster.ui.teacher.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CreateQuizRoute(
    onDone: () -> Unit,
    viewModel: QuizEditorViewModel = hiltViewModel()
) {
    CreateQuizScreen(
        onSave = { title, time, shuffle ->
            viewModel.saveQuiz(title, time, shuffle, onDone)
        }
    )
}

@Composable
fun CreateQuizScreen(
    onSave: (String, Int, Boolean) -> Unit
) {
    val title = remember { mutableStateOf("") }
    val timePerQuestion = remember { mutableStateOf("30") }
    val shuffle = remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create or edit a quiz")
        OutlinedTextField(
            value = title.value,
            onValueChange = { title.value = it },
            label = { Text("Title") }
        )
        OutlinedTextField(
            value = timePerQuestion.value,
            onValueChange = { timePerQuestion.value = it.filter { c -> c.isDigit() } },
            label = { Text("Default time per question (s)") }
        )
        Switch(
            checked = shuffle.value,
            onCheckedChange = { shuffle.value = it }
        )
        Button(onClick = {
            onSave(
                title.value,
                timePerQuestion.value.toIntOrNull() ?: 30,
                shuffle.value
            )
        }) {
            Text("Save Quiz")
        }
    }
}
