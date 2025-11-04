package com.classroom.quizmaster.ui.feature.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.CourseSummary
import com.classroom.quizmaster.domain.model.LearningUnit
import com.classroom.quizmaster.domain.model.LearningUnitType

@Composable
fun LearnCatalogScreen(
    viewModel: LearnCatalogViewModel,
    onOpenCourse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Catalog",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            label = { Text("Search modules, objectives, or topics") },
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(state.courses, key = { it.id }) { course ->
                CourseCard(course = course, onOpen = { onOpenCourse(course.id) })
            }
        }
    }
}

@Composable
private fun CourseCard(
    course: CourseSummary,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyMedium
            )
            AssistChip(
                onClick = {},
                label = { Text("${course.units.size} micro-units • ${course.category}") }
            )
            course.units.forEach { unit ->
                UnitRow(unit = unit)
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onOpen) {
                Text("Open module")
            }
        }
    }
}

@Composable
private fun UnitRow(unit: LearningUnit) {
    RowWithIcon(
        label = unit.title,
        detail = "${unit.estimatedMinutes} min • ${unit.type.label()}",
        iconType = unit.type
    )
}

@Composable
private fun RowWithIcon(
    label: String,
    detail: String,
    iconType: LearningUnitType,
    modifier: Modifier = Modifier
) {
    val icon = when (iconType) {
        LearningUnitType.PreTest, LearningUnitType.PostTest -> Icons.Outlined.Assessment
        LearningUnitType.Lesson -> Icons.Outlined.MenuBook
        LearningUnitType.Live -> Icons.Outlined.LiveTv
        LearningUnitType.Resource -> Icons.Outlined.MenuBook
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun LearningUnitType.label(): String = when (this) {
    LearningUnitType.PreTest -> "Pagsusulit Bago ang Aralin"
    LearningUnitType.Lesson -> "Talakayan / Aralin"
    LearningUnitType.PostTest -> "Pagsusulit Pagkatapos ng Aralin"
    LearningUnitType.Live -> "Live session"
    LearningUnitType.Resource -> "Resource"
}
