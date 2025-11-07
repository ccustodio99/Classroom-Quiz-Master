package com.classroom.quizmaster.ui.teacher.live

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.LanMeta
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun HostLiveScreen(
    session: Session?,
    participants: List<Participant>,
    lanMeta: LanMeta?,
    onReveal: () -> Unit,
    onNext: () -> Unit,
    onKick: (String) -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = session?.let { "Question ${it.currentIndex + 1}" } ?: "No active session",
            style = MaterialTheme.typography.headlineMedium
        )
        lanMeta?.let { LanMetaCard(it, session?.joinCode) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onReveal) { Text("Reveal") }
            Button(onClick = onNext) { Text("Next") }
            Button(onClick = onEnd) { Text("End Session") }
        }
        ParticipantList(participants = participants, onKick = onKick)
    }
}

@Composable
private fun LanMetaCard(lanMeta: LanMeta, joinCode: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("LAN Lobby", style = MaterialTheme.typography.titleMedium)
            if (!joinCode.isNullOrBlank()) {
                Text("Join code: $joinCode")
            }
            Text("Endpoint: ${lanMeta.hostIp}:${lanMeta.port}")
            val qrBitmap = remember(lanMeta) { generateQr(lanMeta) }
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR code for ws://${lanMeta.hostIp}:${lanMeta.port}",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ParticipantList(
    participants: List<Participant>,
    onKick: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Leaderboard", style = MaterialTheme.typography.titleLarge)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(participants, key = { it.uid }) { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${participant.nickname} - ${participant.totalPoints} pts")
                        TextButton(onClick = { onKick(participant.uid) }) {
                            Text("Kick")
                        }
                    }
                }
            }
        }
    }
}

private fun generateQr(meta: LanMeta): Bitmap {
    val content = "ws://${meta.hostIp}:${meta.port}/ws?token=${meta.token}"
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
    val bmp = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until bitMatrix.width) {
        for (y in 0 until bitMatrix.height) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bmp
}


