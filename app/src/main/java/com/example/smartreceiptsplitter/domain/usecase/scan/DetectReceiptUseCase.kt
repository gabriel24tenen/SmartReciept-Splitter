package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.repository.DetectedReceipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageProcessor
import javax.inject.Inject

class DetectReceiptUseCase @Inject constructor(
    private val processor: ReceiptImageProcessor
) {
    suspend operator fun invoke(jpeg: ByteArray, rotationDegrees: Int): DetectedReceipt {
        val detected = processor.detect(jpeg, rotationDegrees)
        val corners = detected.corners.clamped()
        return if (corners.isUsableCrop()) DetectedReceipt(detected.previewJpeg, corners)
        else DetectedReceipt(detected.previewJpeg, DocumentCorners.fullFrame())
    }
}