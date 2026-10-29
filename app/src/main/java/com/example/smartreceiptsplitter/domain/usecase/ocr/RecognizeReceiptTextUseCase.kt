package com.example.smartreceiptsplitter.domain.usecase.ocr

import android.graphics.Bitmap
import com.example.smartreceiptsplitter.domain.repository.TextRecognitionRepository
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import javax.inject.Inject

class RecognizeReceiptTextUseCase @Inject constructor(
    private val textRecognition: TextRecognitionRepository
) {
    suspend operator fun invoke(correctedImage: Bitmap): RecognizedText = textRecognition.recognize(correctedImage)
}