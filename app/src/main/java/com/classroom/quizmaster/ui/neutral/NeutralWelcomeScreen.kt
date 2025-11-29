package com.classroom.quizmaster.ui.neutral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun NeutralWelcomeScreen(
    onLogin: () -> Unit,
    onOfflineDemo: () -> Unit,
    modifier: Modifier = Modifier,
    isOfflineDemoLoading: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 3.dp
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null,
                modifier = Modifier
                    .padding(20.dp)
                    .size(48.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Classroom Quiz Master",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Create classroom quizzes, schedule practice, and review reports.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PrimaryButton(
                text = "Login / Create Account",
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = onOfflineDemo,
                enabled = !isOfflineDemoLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (isOfflineDemoLoading) "Preparing demo..." else "Explore offline demo")
            }
            Text(
                text = "Try the app with sample data. Progress won't be saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@QuizPreviews
@Composable
private fun NeutralWelcomeScreenPreview() {
    QuizMasterTheme {
        NeutralWelcomeScreen(
            onLogin = {},
            onOfflineDemo = {}
        )
    }
}
