package com.classroom.quizmaster.ui.student.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StudentEntryScreen(
    onJoinLan: () -> Unit,
    onJoinCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Join a classroom session")
        Button(onClick = onJoinLan) { Text("Join via LAN") }
        Button(onClick = onJoinCode) { Text("Join with Code") }
    }
}
