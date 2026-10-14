package com.example.smartreceiptsplitter.domain.usecase.scan

import android.graphics.Bitmap
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

internal class  GrayscaleBuffer(val width: Int, val height: Int, val pixels: FloatArray) {
    operator fun get(x: Int, y: Int): Float  {
        val cx = x.coerceIn(0, width - 1)
        val cy = y.coerceIn(0, height - 1)
        return pixels[cy * width + cx]
    }
}

internal fun Bitmap.downscale(maxDimension: Int): Bitmap {
    val longSide = max(width, height)
    if (longSide <= maxDimension) return this
    val scale = maxDimension.toFloat() / longSide
    val newWidth = (width * scale).toInt().coerceAtLeast(1)
    val newHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

internal fun Bitmap.toGrayscale(): GrayscaleBuffer {
    val w = width
    val h = height
    val argb = IntArray(w * h)
    getPixels(argb, 0, w, 0, 0, w, h)
    val out = FloatArray(w * h)
    for (i in argb.indices) {
        val p = argb[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        out[i] = 0.299f * r + 0.587f * g + 0.114f * b
    }
    return GrayscaleBuffer(w, h, out)
}

internal fun GrayscaleBuffer.boxBlur3x3(): GrayscaleBuffer {
    val out = FloatArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0f
            for (dy in -1..1) for (dx in -1..1) sum += this[x + dx, y + dy]
            out[y * width + x] = sum / 9f
        }
    }
    return GrayscaleBuffer(width, height, out)
}

internal fun GrayscaleBuffer.sobelMagnitude(): GrayscaleBuffer {
    val out = FloatArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val gx = (this[x + 1, y - 1] + 2 * this[x + 1, y] + this[x + 1, y + 1]) -
                    (this[x - 1, y - 1] + 2 * this[x - 1, y] + this[x - 1, y + 1])
            val gy = (this[x - 1, y + 1] + 2 * this[x, y + 1] + this[x + 1, y + 1]) -
                    (this[x - 1, y - 1] + 2 * this[x, y - 1] + this[x + 1, y - 1])
            out[y * width + x] = hypot(gx, gy)
        }
    }
    return GrayscaleBuffer(width, height, out)
}

internal fun GrayscaleBuffer.adaptiveThreshold(): Float {
    val n = pixels.size
    var sum = 0.0
    for (v in pixels) sum += v
    val mean = sum / n
    var variance = 0.0
    for (v in pixels) variance += (v - mean) * (v - mean)
    val stdDev = sqrt(variance / n)
    return (mean + stdDev).toFloat()
}

internal fun GrayscaleBuffer.strongPixelCoordinates(threshold: Float): List<Pair<Int, Int>> {
    val points = ArrayList<Pair<Int, Int>>()
    for (y in 0 until height) {
        for (x in 0 until width) {
            if(pixels[y * width + x] >= threshold) points.add(x to y)
        }
    }
    return points
}

internal fun List<Pair<Int, Int>>.extremeQuad(imageWidth: Int, imageHeight: Int): DocumentCorners? {
    if(size < MIN_EDGE_PIXELS) return null

    var topLeft = this[0]       // minimizes x + y
    var bottomRight = this[0]   // maximizes x + y
    var topRight = this[0]      // maximizes x - y
    var bottomLeft = this[0]    // minimizes x -

    for (p in this) {
        val sum = p.first + p.second
        val diff = p.first - p.second
        if (sum < topLeft.first + topLeft.second) topLeft = p
        if (sum > bottomRight.first + bottomRight.second) bottomRight = p
        if (diff > topRight.first - topRight.second) topRight = p
        if (diff < bottomLeft.first - bottomLeft.second) bottomLeft = p
    }

    val area = quadArea(topLeft, topRight, bottomRight, bottomLeft)
    val minArea = imageWidth * imageHeight * MIN_AREA_FRACTION
    if (area < minArea) return null

    return DocumentCorners(
        topLeft = topLeft.toNormalized(imageWidth, imageHeight),
        topRight = topRight.toNormalized(imageWidth, imageHeight),
        bottomRight = bottomRight.toNormalized(imageWidth, imageHeight),
        bottomLeft = bottomLeft.toNormalized(imageWidth, imageHeight),
        isConfident = true,
    )
}

private fun quadArea(a: Pair<Int, Int>, b: Pair<Int, Int>, c: Pair<Int, Int>, d: Pair<Int, Int>): Float {
    val points = listOf(a, b, c, d)
    var sum = 0f
    for (i in points.indices) {
        val (x1, y1) = points[i]
        val (x2, y2) = points[(i + 1) % points.size]
        sum += (x1 * y2 - x2 * y1).toFloat()
    }
    return abs(sum) / 2f
}

private fun Pair<Int, Int>.toNormalized(width: Int, height: Int) =
    NormalizedPoint(first / width.toFloat(), second / height.toFloat())

private const val MIN_EDGE_PIXELS = 40
private const val MIN_AREA_FRACTION = 0.12f