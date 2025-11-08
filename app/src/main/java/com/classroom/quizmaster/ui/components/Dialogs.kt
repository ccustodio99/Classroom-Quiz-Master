package com.classroom.quizmaster.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun ConfirmStartDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Start now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Start live game?") },
        text = { Text("Players will be locked in and timers begin immediately.") }
    )
}

@Composable
fun ConfirmEndDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("End session", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep hosting")
            }
        },
        title = { Text("End session for everyone?") },
        text = { Text("Results are saved locally and sync when online again.") }
    )
}

@Composable
fun SaveChangesDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onSave) { Text("Save changes") }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) { Text("Discard") }
        },
        title = { Text("Save quiz draft?") },
        text = { Text("Unsaved questions will be lost if you leave now.") }
    )
}

@Composable
fun KickPlayerDialog(
    open: Boolean,
    playerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Stay")
            }
        },
        title = { Text("Remove $playerName?") },
        text = { Text("$playerName will lose their current progress.") }
    )
}

@QuizPreviews
@Composable
private fun DialogPreview() {
    QuizMasterTheme {
        ConfirmStartDialog(open = true, onDismiss = {}, onConfirm = {})
    }
}

@Composable
fun DiscardDraftDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Discard") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep editing") }
        },
        title = { Text("Discard changes?") },
        text = { Text("Your unsaved edits for this quiz will be lost.") }
    )
}
