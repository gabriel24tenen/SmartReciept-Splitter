package com.example.smartreceiptsplitter.domain.repository

interface ReceiptImageProcessor {

    // jpeg - the photo as the camera returned it
    // rotationDegrees - how mucb the photo has to be rotated clockwise to be upright
    suspend fun correct(jpeg: ByteArray, rotationDegrees: Int): CorrectedReceiptImage
}

class CorrectedReceiptImage(val jpeg: ByteArray, val isConfident: Boolean)