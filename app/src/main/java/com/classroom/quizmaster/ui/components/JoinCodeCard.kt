package com.classroom.quizmaster.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.R
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import com.classroom.quizmaster.util.QrCodeGenerator

@Composable
fun JoinCodeCard(
    code: String,
    expiresIn: String,
    peersConnected: Int,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    qrData: String? = null,
    qrPlaceholder: @Composable () -> Unit = { QRPlaceholder() }
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Join code",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = code,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy code")
                }
            }
            Text(
                text = listOfNotNull(
                    expiresIn.takeIf { it.isNotBlank() }?.let { "Expires in $it" },
                    "$peersConnected peers nearby"
                ).joinToString(" | "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (qrData.isNullOrBlank()) {
                qrPlaceholder()
            } else {
                QrCodeImage(data = qrData)
            }
        }
    }
}

@Composable
fun QRPlaceholder(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(160.dp)
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        QrPlaceholderContents()
    }
}

@Composable
private fun QrCodeImage(
    data: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(160.dp)
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val minDimension = if (maxWidth < maxHeight) maxWidth else maxHeight
            val sizePx = remember(minDimension, density) {
                with(density) { minDimension.roundToPx().coerceAtLeast(1) }
            }
            val qrBitmap = remember(data, sizePx) {
                QrCodeGenerator.encode(data, sizePx)
            }
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = stringResource(R.string.launch_lobby_qr_content_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                QrUnavailableContents()
            }
        }
    }
}

@Composable
private fun QrUnavailableContents() {
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dashedBorderColor = onSurfaceVariantColor.copy(alpha = 0.2f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = "QR code not available",
            modifier = Modifier.size(48.dp),
            tint = onSurfaceVariantColor
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = dashedBorderColor,
                style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
            )
        }
    }
}

@QuizPreviews
@Composable
private fun JoinCodePreview() {
    QuizMasterTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            JoinCodeCard(
                code = "X7Q2",
                expiresIn = "09:20",
                peersConnected = 6,
                onCopy = {},
                qrData = "ws://192.168.0.10:48765/ws?token=demo"
            )
        }
    }
}
