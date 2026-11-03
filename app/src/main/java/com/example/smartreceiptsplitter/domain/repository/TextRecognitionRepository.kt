package com.example.smartreceiptsplitter.domain.repository


import com.example.smartreceiptsplitter.domain.model.RecognizedText

interface TextRecognitionRepository {
    suspend fun recognize(imagePath: String): RecognizedText
}