package com.classroom.quizmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.theme.ClassroomQuizMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassroomQuizMasterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuizDashboard()
                }
            }
        }
    }
}

enum class Role(val title: String, val description: String) {
    Facilitator(
        title = "Facilitator",
        description = "Create classes, schedule live sessions, and monitor attendance in real time."
    ),
    Student(
        title = "Student",
        description = "Join a classroom, respond to quick polls, and track your quiz history."
    ),
    Observer(
        title = "Observer",
        description = "Follow along in spectator mode to evaluate engagement quality."
    )
}

@Composable
fun QuizDashboard(
    modifier: Modifier = Modifier,
    onStartSession: () -> Unit = {}
) {
    var selectedRole by rememberSaveable { mutableStateOf(Role.Facilitator) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Classroom Quiz Master",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Align every role before your next classroom session.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RolePicker(
                selectedRole = selectedRole,
                onRoleSelected = { selectedRole = it }
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartSession
        ) {
            Text(text = "Start as ${'$'}{selectedRole.title}")
        }
    }
}

@Composable
private fun RolePicker(
    selectedRole: Role,
    onRoleSelected: (Role) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Role.entries.forEach { role ->
            RoleCard(
                role = role,
                selected = role == selectedRole,
                onSelect = { onRoleSelected(role) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleCard(
    role: Role,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = role.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = role.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuizDashboardPreview() {
    ClassroomQuizMasterTheme {
        QuizDashboard()
    }
}
