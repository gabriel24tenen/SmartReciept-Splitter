package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageProcessor
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageStorage
import javax.inject.Inject

class ProcessCapturedReceiptUseCase  @Inject constructor(
    private val processor: ReceiptImageProcessor,
    private val storage: ReceiptImageStorage
) {
    suspend operator fun invoke (jpeg: ByteArray, rotationDegrees: Int, corners: DocumentCorners): String {
        val crop = corners.clamped()
        require(crop.isUsableCrop()) { "The corners don't outline a receipt."}
        val corrected = processor.correct(jpeg, rotationDegrees, crop)
        return storage.save(corrected)
    }
}