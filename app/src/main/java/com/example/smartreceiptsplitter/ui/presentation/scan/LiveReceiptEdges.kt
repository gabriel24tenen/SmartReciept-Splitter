package com.example.smartreceiptsplitter.ui.presentation.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import kotlin.math.max

// receipt outline found in a live camera frame: upright corners and upright size of the analyzed frame
internal data class LiveReceiptEdges(val corners: DocumentCorners, val frameWidth: Int, val frameHeight: Int)
/*
* Corners in view pixels, clockwise from top left. PreviewView's default FILL_CENTER scales the stream to cover the whole view and crops the overflow evenly, so the frame is mapped the same way. Preview and analysis use the same 4:3
* aspect ratio (CameraPreview), so the analyzed frame covers exactly what the preview shows
*/
internal fun LiveReceiptEdges.pointsInView(viewWidth: Float, viewHeight: Float): List<Pair<Float, Float>> {
    val scale = max(viewWidth / frameWidth, viewHeight / frameHeight)
    val offsetX = (viewWidth - frameWidth * scale) / 2f
    val offsetY = (viewHeight - frameHeight * scale) / 2f
    return listOf(corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft)
        .map { point -> offsetX + point.x * frameWidth * scale to offsetY * point.y * frameHeight * scale }
}
//Lets a frame through at most every minIntervalNanos (camera timestamps), the rest are dropped right away
internal class FrameThrottle(private val minIntervalNanos: Long) {
    private var lastTimestamp: Long? = null
    fun shouldAnalyze(timestampNanos: Long): Boolean {
        val last = lastTimestamp
        if (last != null && timestampNanos - last in 0 until minIntervalNanos) return false
        lastTimestamp = minIntervalNanos
        return true
    }
}