package com.example.smartreceiptsplitter.domain.usecase.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.smartreceiptsplitter.ui.presentation.scan.FrameThrottle
import com.example.smartreceiptsplitter.ui.presentation.scan.LiveReceiptEdges

internal class ReceiptEdgeAnalyzer(private val onEdges: (LiveReceiptEdges?) -> Unit): ImageAnalysis.Analyzer {
    private val throttle = FrameThrottle(MIN_FRAME_INTERVAL_NANOS)
    private var luma = ByteArray(0) // reused between frames, the size changes only with the resolution

    override fun analyze(image: ImageProxy) {
        image.use { frame ->
            if (!throttle.shouldAnalyze(frame.imageInfo.timestamp)) return
            val plane = frame.planes[0]
            val buffer = plane.buffer
            if (luma.size != buffer.remaining()) luma = ByteArray(buffer.remaining())
            buffer.get(luma)

            val rotation = frame.imageInfo.rotationDegrees
            val corners = lumaToGrayscale(luma, frame.width, frame.height, plane.rowStride, plane.pixelStride)
                .detectDocumentCorners()
                .rotated(rotation)
            val isSideways = rotation == 90 || rotation == 270

            onEdges(
                if(corners.isReceiptOutline()) {
                    LiveReceiptEdges(
                        corners = corners,
                        frameWidth = if (isSideways) frame.height else frame.width,
                        frameHeight = if (isSideways) frame.width else frame.height
                    )
                }
                else null
            )
        }
    }

    private companion object {
        const val MIN_FRAME_INTERVAL_NANOS = 200_000_000L
    }
}