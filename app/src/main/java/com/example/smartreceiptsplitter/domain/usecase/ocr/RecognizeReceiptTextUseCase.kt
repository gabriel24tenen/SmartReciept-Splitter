package com.example.smartreceiptsplitter.domain.usecase.ocr


import com.example.smartreceiptsplitter.domain.repository.TextRecognitionRepository
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import javax.inject.Inject

class RecognizeReceiptTextUseCase @Inject constructor(
    private val textRecognition: TextRecognitionRepository
) {
    suspend operator fun invoke(imagePath: String): RecognizedText = textRecognition.recognize(imagePath)
}