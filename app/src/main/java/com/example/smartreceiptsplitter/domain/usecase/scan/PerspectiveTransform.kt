package com.example.smartreceiptsplitter.domain.usecase.scan


import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import kotlin.math.hypot
import kotlin.math.roundToInt

internal fun DocumentCorners.toPixelPoints(width: Int, height: Int): FloatArray = floatArrayOf(
    topLeft.x * width, topLeft.y * height,
    topRight.x * width, topRight.y * height,
    bottomRight.x * width, bottomRight.y * height,
    bottomLeft.x * width, bottomLeft.y * height
)

internal fun computeOutputSize(srcPoints: FloatArray): Pair<Int, Int> {
    val tlX = srcPoints[0]; val tlY = srcPoints[1]
    val trX = srcPoints[2]; val trY = srcPoints[3]
    val brX = srcPoints[4]; val brY = srcPoints[5]
    val blX = srcPoints[6]; val blY = srcPoints[7]

    val widthTop = hypot(trX - tlX, trY - tlY)
    val widthBottom = hypot(brX - blX, brY - blY)
    val heightLeft = hypot(blX - tlX, blY - tlY)
    val heightRight = hypot(brX - trX, brY - trY)

    val rawWidth = maxOf(widthTop, widthBottom)
    val rawHeight = maxOf(heightLeft, heightRight)

    val scale = minOf(1f, MAX_OUTPUT_DIMENSION / maxOf(rawWidth, rawHeight, 1f))
    val width = (rawWidth * scale).roundToInt().coerceIn(1, MAX_OUTPUT_DIMENSION)
    val height = (rawHeight * scale).roundToInt().coerceIn(1, MAX_OUTPUT_DIMENSION)
    return width to height
}

private const val MAX_OUTPUT_DIMENSION = 2000