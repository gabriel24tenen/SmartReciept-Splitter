package com.example.smartreceiptsplitter.data.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.smartreceiptsplitter.domain.repository.TextRecognitionRepository
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import com.example.smartreceiptsplitter.domain.model.RecognizedTextBlock
import com.example.smartreceiptsplitter.domain.model.RecognizedTextLine
import com.example.smartreceiptsplitter.domain.model.TextBoundingBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Text Recognition v2, Latin script.
 * Uses the bundled artifact (com.google.mlkit:text-recognition), so the model ships inside the APK:
 * nothing is downloaded at runtime and recognition runs fully on-device.
 */
@Singleton
class MLKitTextRecognitionRepository @Inject constructor() : TextRecognitionRepository {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) //one client for the whole app lifetime, ML Kit runs on its own background executor
    override suspend fun recognize(bitmap: Bitmap): RecognizedText {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = suspendCancellableCoroutine { continuation ->
            recognizer.process(image).addOnSuccessListener { result -> continuation.resume(result)}.addOnFailureListener { error -> continuation.resumeWithException(error) }
        }
        return text.toDomain()
    }

    private fun Text.toDomain() = RecognizedText(
        fullText = text,
        blocks = textBlocks.map { block ->
            RecognizedTextBlock(
                text = block.text,
                lines = block.lines.map { line ->
                    RecognizedTextLine(
                        text = line.text,
                        boundingBox = line.boundingBox?.toDomain(),
                        confidence = line.confidence
                    )
                },
                boundingBox = block.boundingBox?.toDomain()
            )
        }
    )
    private fun Rect.toDomain() = TextBoundingBox(left = left, top = top, right = right, bottom = bottom)
}