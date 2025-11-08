package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun ConnectivityBanner(
    headline: String,
    supportingText: String,
    statusChips: List<StatusChipUi>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = headline, style = MaterialTheme.typography.titleMedium)
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                statusChips.forEach { chip ->
                    StatusIndicator(chip = chip)
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(chip: StatusChipUi) {
    val (bg, icon, iconTint) = when (chip.type) {
        StatusChipType.Lan -> Triple(Color(0xFFDCFCE7), Icons.Default.Wifi, MaterialTheme.colorScheme.primary)
        StatusChipType.Cloud -> Triple(Color(0xFFE0F2FE), Icons.Default.Cloud, MaterialTheme.colorScheme.secondary)
        StatusChipType.Offline -> Triple(Color(0xFFFFE4E6), Icons.Default.CloudOff, MaterialTheme.colorScheme.error)
    }
    Row(
        modifier = Modifier
            .background(bg, shape = CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Text(
            text = chip.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@QuizPreviews
@Composable
private fun ConnectivityBannerPreview() {
    QuizMasterTheme {
        ConnectivityBanner(
            headline = "LAN connected | Cloud syncing",
            supportingText = "Quiz draft saved locally. Cloud retry scheduled in 2 min.",
            statusChips = listOf(
                StatusChipUi("lan", "LAN", StatusChipType.Lan),
                StatusChipUi("cloud", "Cloud", StatusChipType.Cloud),
                StatusChipUi("offline", "Offline-ready", StatusChipType.Offline)
            )
        )
    }
}
