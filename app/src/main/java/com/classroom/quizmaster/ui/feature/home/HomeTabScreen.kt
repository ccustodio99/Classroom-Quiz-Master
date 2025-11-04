package com.classroom.quizmaster.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.domain.model.HomeFeedItem
import com.classroom.quizmaster.domain.model.HomeFeedType
import com.classroom.quizmaster.domain.model.PersonaBlueprint

@Composable
fun HomeTabScreen(
    viewModel: HomeTabViewModel,
    onOpenModule: (String) -> Unit,
    onGoToClassroom: (String) -> Unit,
    onCreateModule: (String?) -> Unit,
    onJoinLive: () -> Unit,
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
            Text("Preparing your classroom flow…")
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Today's Flow",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pagsusulit Bago ang Aralin → Talakayan / Aralin → Pagsusulit Pagkatapos ng Aralin",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        items(state.homeFeed, key = { it.id }) { feed ->
            HomeFeedCard(
                feed = feed,
                onPrimaryAction = {
                    feed.task?.relatedModuleId?.let(onOpenModule)
                        ?: if (feed.type == HomeFeedType.Streak) onJoinLive()
                        else onCreateModule(null)
                }
            )
        }

        if (state.classrooms.isNotEmpty()) {
            item {
                Text(
                    text = "Classrooms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            items(state.classrooms, key = { it.id }) { classroom ->
                ClassroomSummaryCard(
                    profile = classroom,
                    onOpen = { onGoToClassroom(classroom.id) },
                    onCreateModule = { onCreateModule(classroom.id) }
                )
            }
        }

        state.persona?.let { persona ->
            item {
                PersonaBlueprintCard(persona)
            }
        }

        item {
            AssistChip(
                onClick = onJoinLive,
                label = { Text("Start live session via LAN") },
                leadingIcon = {
                    Icon(Icons.Outlined.Bolt, contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun HomeFeedCard(
    feed: HomeFeedItem,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (feed.type) {
                HomeFeedType.Streak -> MaterialTheme.colorScheme.primaryContainer
                HomeFeedType.Message -> MaterialTheme.colorScheme.secondaryContainer
                HomeFeedType.Reminder -> MaterialTheme.colorScheme.tertiaryContainer
                HomeFeedType.Task -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = feed.accent,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = feed.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = feed.detail,
                style = MaterialTheme.typography.bodyMedium
            )
            feed.task?.objectiveTags?.takeIf { it.isNotEmpty() }?.let { objectives ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    objectives.take(2).forEach { objective ->
                        AssistChip(onClick = {}, label = { Text(objective) })
                    }
                }
            }
            Button(
                onClick = onPrimaryAction,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(feed.task?.actionLabel ?: "Open")
            }
        }
    }
}

@Composable
private fun ClassroomSummaryCard(
    profile: ClassroomProfile,
    onOpen: () -> Unit,
    onCreateModule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = profile.subject,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpen) {
                    Icon(Icons.Outlined.School, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open classroom")
                }
                Button(onClick = onCreateModule) {
                    Text("Create module")
                }
            }
        }
    }
}

@Composable
private fun PersonaBlueprintCard(
    persona: PersonaBlueprint,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${persona.type.name} playbook",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            SectionList(title = "Goals", entries = persona.highlights)
            if (persona.frustrations.isNotEmpty()) {
                SectionList(title = "Avoid", entries = persona.frustrations)
            }
            if (persona.offlineMustHaves.isNotEmpty()) {
                SectionList(title = "Offline-first", entries = persona.offlineMustHaves)
            }
        }
    }
}

@Composable
private fun SectionList(title: String, entries: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        entries.forEach { entry ->
            Text("• $entry", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
