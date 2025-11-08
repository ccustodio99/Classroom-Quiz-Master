package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.R
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun LeaderboardList(
    rows: List<LeaderboardRowUi>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp
    ) {
        Column {
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows, key = { it.rank }) { row ->
                    LeaderboardRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(row: LeaderboardRowUi) {
    val gradientBrush = remember(row.avatar.colors) {
        Brush.horizontalGradient(row.avatar.colors.ifEmpty { listOf(Color.Gray, Color.DarkGray) })
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (row.rank <= 3) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "${row.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                modifier = Modifier.height(40.dp),
                shape = MaterialTheme.shapes.small,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .background(gradientBrush)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "${row.displayName} avatar"
                    )
                    Text(
                        text = row.avatar.label,
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Column {
                Text(
                    text = row.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${row.score} pts",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (row.delta >= 0) "+${row.delta}" else row.delta.toString(),
                color = if (row.delta >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
            if (row.isYou) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "You",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@QuizPreviews
@Composable
private fun LeaderboardPreview() {
    QuizMasterTheme {
        LeaderboardList(
            rows = listOf(
                LeaderboardRowUi(1, "Ava Flores", 980, 12, AvatarOption("ava", "AF", listOf(Color(0xFF6EE7B7), Color(0xFF3B82F6)), "spark"), true),
                LeaderboardRowUi(2, "Liam Chen", 930, -5, AvatarOption("liam", "LC", listOf(Color(0xFFFDE68A), Color(0xFFFB7185)), "atom")),
                LeaderboardRowUi(3, "Maya Neri", 910, 8, AvatarOption("maya", "MN", listOf(Color(0xFFA5B4FC), Color(0xFF6366F1)), "pencil")),
                LeaderboardRowUi(4, "Noah Cruz", 880, 3, AvatarOption("noah", "NC", listOf(Color(0xFFFECDD3), Color(0xFFFECACA)), "flag"))
            )
        )
    }
}
