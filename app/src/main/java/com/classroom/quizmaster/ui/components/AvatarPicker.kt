package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun AvatarPicker(
    avatars: List<AvatarOption>,
    selectedId: String?,
    onAvatarSelected: (AvatarOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Choose an avatar",
            style = MaterialTheme.typography.titleMedium
        )
        avatars.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { option ->
                    AvatarChip(
                        option = option,
                        selected = option.id == selectedId,
                        onClick = { onAvatarSelected(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AvatarChip(
    option: AvatarOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = remember(option.colors) {
        Brush.linearGradient(option.colors.ifEmpty { listOf(Color.LightGray, Color.DarkGray) })
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (selected) 8.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
            Column {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = option.iconName.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@QuizPreviews
@Composable
private fun AvatarPickerPreview() {
    QuizMasterTheme {
        AvatarPicker(
            avatars = listOf(
                AvatarOption("aurora", "Aurora", listOf(Color(0xFF34D399), Color(0xFF3B82F6)), "spark"),
                AvatarOption("zen", "Zen", listOf(Color(0xFFFDE68A), Color(0xFFF97316)), "pencil"),
                AvatarOption("luna", "Luna", listOf(Color(0xFF818CF8), Color(0xFF3730A3)), "atom"),
                AvatarOption("coco", "Coco", listOf(Color(0xFFFECACA), Color(0xFFFB7185)), "flag"),
                AvatarOption("mango", "Mango", listOf(Color(0xFFFFF3BF), Color(0xFFF59E0B)), "compass"),
                AvatarOption("iris", "Iris", listOf(Color(0xFFBBF7D0), Color(0xFF2DD4BF)), "compass")
            ),
            selectedId = "zen",
            onAvatarSelected = {}
        )
    }
}
