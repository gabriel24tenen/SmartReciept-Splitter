package com.example.smartreceiptsplitter.ui.presentation.scan


import androidx.camera.core.ImageProxy


/*
A photo straight from IMageCapture: the compressed the JPEG bytes and how much it has to be rotated to be upright; copying the bytes out og the ImageProxy is cheap,
so it's done in the capture callback (main thread)
decoding happens laters, off the main thread in BitmapReceiptImageProcessor
*/

class CapturedPhoto (val jpeg: ByteArray, val rotationDegrees: Int)

internal fun ImageProxy.toCapturedPhoto(): CapturedPhoto {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    return CapturedPhoto(bytes, imageInfo.rotationDegrees)
}


