package com.classroom.quizmaster.ui.student.end

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StudentEndScreen(
    stars: Int,
    rank: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Session complete!")
        Text("Stars earned: $stars")
        Text("Rank: $rank")
    }
}
