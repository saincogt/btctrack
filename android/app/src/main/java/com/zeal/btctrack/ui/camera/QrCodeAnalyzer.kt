package com.zeal.btctrack.ui.camera

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

class QrCodeAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        val yBuffer = mediaImage.planes[0].buffer
        val yBytes = ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }
        val source = PlanarYUVLuminanceSource(
            yBytes,
            image.width,
            image.height,
            0, 0,
            image.width,
            image.height,
            false,
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result = reader.decode(bitmap)
            onResult(result.text)
        } catch (_: NotFoundException) {
            // No QR in this frame — continue scanning
        } finally {
            image.close()
        }
    }
}
