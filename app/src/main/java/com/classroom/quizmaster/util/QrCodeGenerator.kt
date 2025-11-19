package com.classroom.quizmaster.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeGenerator {
    private val writer = QRCodeWriter()
    private val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1
    )

    fun encode(content: String, sizePx: Int): ImageBitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return runCatching {
            val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height) { index ->
                val x = index % width
                val y = index / width
                if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }.asImageBitmap()
        }.getOrNull()
    }
}
