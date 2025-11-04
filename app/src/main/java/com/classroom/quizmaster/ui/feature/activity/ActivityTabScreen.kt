package com.classroom.quizmaster.ui.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.ActivityTimeline
import com.classroom.quizmaster.domain.model.Badge
import com.classroom.quizmaster.domain.model.Certificate

@Composable
fun ActivityTabScreen(
    viewModel: ActivityTabViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text("Loading badges and streaks…")
        }
        return
    }

    val timeline = state.timeline ?: return

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            StreakCard(timeline)
        }
        if (timeline.badges.isNotEmpty()) {
            item {
                Text(
                    text = "Badges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(timeline.badges, key = { it.id }) { badge ->
                BadgeCard(badge)
            }
        }
        if (timeline.certificates.isNotEmpty()) {
            item {
                Text(
                    text = "Certificates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(timeline.certificates, key = { it.id }) { certificate ->
                CertificateCard(certificate)
            }
        }
    }
}

@Composable
private fun StreakCard(timeline: ActivityTimeline) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Timeline, contentDescription = null)
            Text(
                text = "Streak: ${timeline.streakDays} days",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Keep the streak alive to unlock more engagement bonuses and badges.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BadgeCard(badge: Badge) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.Badge, contentDescription = null)
            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CertificateCard(certificate: Certificate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.EmojiEvents, contentDescription = null)
            Text(
                text = certificate.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = certificate.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

