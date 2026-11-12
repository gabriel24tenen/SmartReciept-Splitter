package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import kotlin.math.abs
import kotlin.math.max

internal fun lumaToGrayscale(
    luma: ByteArray,
    width: Int,
    height: Int,
    rowStride: Int,
    pixelStride: Int,
    maxDimension: Int = EDGE_DETECTION_MAX_DIMENSION
): GrayscaleBuffer {
    require(width > 0 && height > 0) { "Empty frame" }
    val step = ((max(width, height) + maxDimension - 1) / maxDimension).coerceAtLeast(1)
    val outWidth = (width / step).coerceAtLeast(1)
    val outHeight = (height / step).coerceAtLeast(1)
    val blockSize = (step * step).toFloat()
    val out = FloatArray(outWidth * outHeight)
    for (oy in 0 until outHeight) {
        for (ox in 0 until outWidth) {
            var sum = 0
            for (dy in 0 until step) {
                val row = (oy * step + dy) * rowStride
                for (dx in 0 until step) sum += luma[row + (ox * step +dx) * pixelStride].toInt() and 0xFF
            }
            out[oy * outWidth + ox] = sum / blockSize
        }
    }
    return GrayscaleBuffer(outWidth, outHeight, out)
}
//corners of a frame rotated clockwise by degrees
internal fun DocumentCorners.rotated(degrees: Int): DocumentCorners = when (val turn = ((degrees % 360) + 360) % 360) {
    0 -> this
    90 -> copy(topLeft = bottomLeft.rotate(turn), topRight = topLeft.rotate(turn), bottomRight = topRight.rotate(turn), bottomLeft = bottomRight.rotate(turn))
    180 -> copy(topLeft = bottomRight.rotate(turn), topRight = bottomLeft.rotate(turn), bottomRight = topLeft.rotate(turn), bottomLeft = topRight.rotate(turn))
    270 -> copy(topLeft = topRight.rotate(turn), topRight = bottomRight.rotate(turn), bottomRight = bottomLeft.rotate(turn), bottomLeft = topLeft.rotate(turn))
    else -> throw IllegalArgumentException("Rotation must be a multiple of 90 degrees, was $degrees")
}
private fun NormalizedPoint.rotate(turn: Int) = when(turn) {
    90 -> NormalizedPoint(1f - y, x)
    180 -> NormalizedPoint(1f - x, 1f - y)
    else -> NormalizedPoint(y, 1f - x)
}
// a quad that hugs the frame's corners is what the detector returns for noise, clutter or a plain surface so the overlay draws only a confident quad that is clearly inside the frame
internal fun DocumentCorners.isReceiptOutline(): Boolean = isConfident && !coversWholeFrame()
private fun DocumentCorners.coversWholeFrame(): Boolean {
    val full = DocumentCorners.fullFrame()
    return listOf(topLeft to full.topLeft, topRight to full.topRight, bottomLeft to full.bottomLeft, bottomRight to full.bottomRight)
        .all { (corner, frameCorner) -> abs(corner.x - frameCorner.x) <= FULL_FRAME_TOLERANCE && abs(corner.y - frameCorner.y) <= FULL_FRAME_TOLERANCE }
}
private const val FULL_FRAME_TOLERANCE = 0.04f