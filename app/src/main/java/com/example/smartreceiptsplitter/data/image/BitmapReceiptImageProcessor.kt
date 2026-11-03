package com.example.smartreceiptsplitter.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.example.smartreceiptsplitter.domain.repository.CorrectedReceiptImage
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageProcessor
import com.example.smartreceiptsplitter.domain.usecase.scan.EDGE_DETECTION_MAX_DIMENSION
import com.example.smartreceiptsplitter.domain.usecase.scan.GrayscaleBuffer
import com.example.smartreceiptsplitter.domain.usecase.scan.computeOutputSize
import com.example.smartreceiptsplitter.domain.usecase.scan.detectDocumentCorners
import com.example.smartreceiptsplitter.domain.usecase.scan.toPixelPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max

class BitmapReceiptImageProcessor  @Inject constructor() : ReceiptImageProcessor {
    override suspend fun correct (jpeg: ByteArray, rotationDegrees: Int): CorrectedReceiptImage =
        withContext(Dispatchers.Default) {
            val photo = decodeUpright(jpeg, rotationDegrees, MAX_CAPTURE_DIMENSION)

            val working = photo.downscale(EDGE_DETECTION_MAX_DIMENSION)
            val corners = working.toGrayscale().detectDocumentCorners()
            if (working !== photo) working.recycle()

            val srcPoints = corners.toPixelPoints(photo.width, photo.height)
            val (outputWidth, outputHeight) = computeOutputSize(srcPoints)
            val corrected = photo.warpPerspective(srcPoints, outputWidth, outputHeight) ?: photo

            val bytes = corrected.toJpeg()
            if (corrected !== photo) corrected.recycle()
            photo.recycle()
            CorrectedReceiptImage(bytes, corners.isConfident)

        }

    private fun decodeUpright(jpeg: ByteArray, rotationDegrees: Int, maxDimension: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0 , jpeg.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unable to decode captured image" }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options) ?: error("Unable to decode captured image.")

        val scale = minOf(1f, maxDimension.toFloat() / max(decoded.width, decoded.height))
        if (scale == 1f && rotationDegrees == 0) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotationDegrees.toFloat())
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (result !== decoded) decoded.recycle()
        return result
    }

    private fun Bitmap.downscale(maxDimension: Int): Bitmap {
        val longSide = max(width, height)
        if (longSide <= maxDimension) return this
        val scale = maxDimension.toFloat() / longSide
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    }

    private fun Bitmap.toGrayscale(): GrayscaleBuffer {
        val argb = IntArray(width * height)
        getPixels(argb, 0, width, 0, 0, width, height)
        val out = FloatArray(width * height)
        for (i in argb.indices) {
            val p = argb[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            out[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }
        return GrayscaleBuffer(width, height, out)
    }

    private fun Bitmap.warpPerspective(srcPoints: FloatArray, outputWidth: Int, outputHeight: Int): Bitmap? {
        val dstPoints = floatArrayOf(
            0f, 0f, outputWidth.toFloat(), 0f, outputWidth.toFloat(), outputHeight.toFloat(), 0f, outputHeight.toFloat()
        )
        val matrix = Matrix()
        if (!matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)) return null

        val output = createBitmap(outputWidth, outputHeight)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(this, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return output
    }

    private fun Bitmap.toJpeg(): ByteArray =
        ByteArrayOutputStream().use { output ->
            compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }

    internal companion object {
        const val MAX_CAPTURE_DIMENSION = 3000
        const val JPEG_QUALITY = 85

        fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int{
            val longSide = max(width, height)
            var sample = 1
            while (longSide / (sample * 2) >= maxDimension) sample *= 2
            return sample
        }
    }
}