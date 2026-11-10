package com.example.smartreceiptsplitter.domain.repository

import com.example.smartreceiptsplitter.domain.model.DocumentCorners

interface ReceiptImageProcessor {
    //upright preview of the photo for the review screen and the receipt's corners found on it
    suspend fun detect(jpeg: ByteArray, rotationDegrees: Int): DetectedReceipt
    // the photo cropped to the corners and straighted, as JPEG bytes
    suspend fun correct(jpeg: ByteArray, rotationDegrees: Int, corners: DocumentCorners): ByteArray
}
class DetectedReceipt(val previewJpeg: ByteArray, val corners: DocumentCorners)

// previewJpeg - the upright photo scaled down for the screen
// corners.isConfident is false when the edges were not found