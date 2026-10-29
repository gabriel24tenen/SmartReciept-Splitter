package com.example.smartreceiptsplitter.domain.repository

import android.graphics.Bitmap
import com.example.smartreceiptsplitter.domain.model.RecognizedText

interface TextRecognitionRepository {
    suspend fun recognize(bitmap: Bitmap): RecognizedText
}