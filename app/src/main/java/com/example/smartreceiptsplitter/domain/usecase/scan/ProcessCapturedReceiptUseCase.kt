package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.repository.ReceiptImageProcessor
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageStorage
import javax.inject.Inject

class ProcessCapturedReceiptUseCase  @Inject constructor(
    private val processor: ReceiptImageProcessor,
    private val storage: ReceiptImageStorage
) {
    suspend operator fun invoke (jpeg: ByteArray, rotationDegrees: Int): ScannedReceiptImage {
        val corrected = processor.correct(jpeg, rotationDegrees)
        val path = storage.save(corrected.jpeg)
        return ScannedReceiptImage(path, corrected.isConfident)
    }
}

data class ScannedReceiptImage(val path: String, val isConfident: Boolean) //isConfident - false when the paper's edges were not found and the whole photo was kept