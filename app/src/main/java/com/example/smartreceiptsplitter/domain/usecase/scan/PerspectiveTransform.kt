package com.example.smartreceiptsplitter.domain.usecase.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

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

    val width = maxOf(widthTop, widthBottom).roundToInt().coerceIn(1, MAX_OUTPUT_DIMENSION)
    val height = maxOf(heightLeft, heightRight).roundToInt().coerceIn(1, MAX_OUTPUT_DIMENSION)
    return width to height
}

internal fun Bitmap.warpPerspective(srcPoints: FloatArray, outputWidth: Int, outputHeight: Int): Bitmap? {
    val dstPoints = floatArrayOf(
        0f, 0f, outputWidth.toFloat(), 0f, outputWidth.toFloat(), outputHeight.toFloat(), 0f, outputHeight.toFloat()
    )
    val matrix = Matrix()
    if(!matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)) return null

    val output = createBitmap(outputWidth, outputHeight)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(this, matrix, Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
    return output
}

private const val MAX_OUTPUT_DIMENSION = 2000