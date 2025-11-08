package com.classroom.quizmaster.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import com.classroom.quizmaster.ui.preview.QuizPreviews

private val bannedWords = setOf("bad", "offensive", "dummy")

@Composable
fun NickNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Nickname"
) {
    val trimmed = value.trim()
    val (error, sanitized) = remember(trimmed) {
        val sanitized = trimmed.replace(Regex("""\s+"""), " ")
        val badWord = bannedWords.firstOrNull { sanitized.contains(it, ignoreCase = true) }
        val tooShort = sanitized.length < 3
        val message = when {
            badWord != null -> "Please choose a school-appropriate nickname."
            tooShort -> "Nickname must be 3+ characters."
            else -> null
        }
        message to sanitized
    }
    OutlinedTextField(
        value = sanitized.take(24),
        onValueChange = { onValueChange(it.take(24)) },
        modifier = modifier,
        label = { Text(label) },
        isError = error != null,
        supportingText = {
            error?.let { Text(text = it) }
        }
    )
}

@QuizPreviews
@Composable
private fun NickNameFieldPreview() {
    QuizMasterTheme {
        var value by rememberSaveable { mutableStateOf("Quiz Wiz") }
        NickNameField(value = value, onValueChange = { value = it })
    }
}
