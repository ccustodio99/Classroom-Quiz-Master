package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConnectivityBanner(
    lanConnected: Boolean,
    cloudSyncing: Boolean
) {
    val color = when {
        lanConnected && cloudSyncing -> MaterialTheme.colorScheme.secondaryContainer
        lanConnected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Red.copy(alpha = 0.2f)
    }
    val status = when {
        lanConnected && cloudSyncing -> "LAN + Cloud"
        lanConnected -> "LAN only"
        else -> "Offline"
    }
    Text(
        text = "Connectivity: $status",
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(12.dp)
    )
}
