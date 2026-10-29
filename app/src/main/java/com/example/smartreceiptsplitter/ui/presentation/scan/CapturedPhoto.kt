package com.example.smartreceiptsplitter.ui.presentation.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.Dimension
import androidx.camera.core.ImageProxy
import kotlin.math.max

/*
A photo straight from IMageCapture: the compressed the JPEG bytes and how much it has to be rotated to be upright; copying the bytes out og the ImageProxy is cheap,
so it's done in the capture callback (main thread)
decoding happens laters, off the main thread in [decodeCapturedPhoto]
*/

class CapturedPhoto (val jpeg: ByteArray, val rotationDegrees: Int)

internal fun ImageProxy.toCapturedPhoto(): CapturedPhoto {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    return CapturedPhoto(bytes, imageInfo.rotationDegrees)
}

/**
 * Decodes the photo, scales it is so the longer side it at most [maxDimesion] and rotates it upright
 * Must not run on the main thread, a 12 MP JPEG takes a few hundred milliseconds to decode
 * 1. inSampleSize (a power of two) already shrinks the image while decoding, so a 50 MP photo never becomes a full-size bitmap in memory.
 * 2. The remaining scale and the rotation are applied in one createBitmap call, and the decoded intermediate bitmap is recycled right away.
 *
 */

internal fun decodeCapturedPhoto(photo: CapturedPhoto, maxDimension: Int = MAX_CAPTURED_DIMENSION): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(photo.jpeg, 0, photo.jpeg.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unable to decode captured image." }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
    }
    val decoded = BitmapFactory.decodeByteArray(photo.jpeg, 0, photo.jpeg.size, options)
        ?: error("Unable to decode capture image.")
    val scale = minOf(1f, maxDimension.toFloat() / max(decoded.width, decoded.height))
    if (scale == 1f && photo.rotationDegrees == 0) return decoded
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(photo.rotationDegrees.toFloat())
    }
    val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (result !== decoded) decoded.recycle()
    return result
}

internal fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
    val longSide = max(width, height)
    var sample = 1
    while (longSide / (sample * 2) >= maxDimension) sample *= 2
    return sample
}

internal const val MAX_CAPTURED_DIMENSION = 3000
