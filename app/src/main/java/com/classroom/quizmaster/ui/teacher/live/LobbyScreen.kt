package com.classroom.quizmaster.ui.teacher.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session

@Composable
fun LobbyScreen(
    session: Session?,
    participants: List<Participant>,
    onStart: () -> Unit,
    onToggleLeaderboard: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Lobby - Join code: ${session?.joinCode ?: "---"}")
        Switch(
            checked = session?.hideLeaderboard ?: false,
            onCheckedChange = onToggleLeaderboard
        )
        LazyColumn {
            items(participants) { participant ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = participant.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        Button(onClick = onStart) {
            Text("Start Game")
        }
    }
}

