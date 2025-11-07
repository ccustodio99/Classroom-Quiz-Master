package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.classroom.quizmaster.domain.model.Participant

@Composable
fun LeaderboardSheet(
    participants: List<Participant>,
    hidden: Boolean
) {
    if (hidden) {
        Text("Leaderboard hidden")
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Leaderboard")
        participants.forEachIndexed { index, participant ->
            Text("${index + 1}. ${participant.nickname} – ${participant.totalPoints}")
        }
    }
}
