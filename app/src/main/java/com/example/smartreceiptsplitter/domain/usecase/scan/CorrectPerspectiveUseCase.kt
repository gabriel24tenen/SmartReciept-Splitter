package com.example.smartreceiptsplitter.domain.usecase.scan

import android.graphics.Bitmap
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CorrectPerspectiveUseCase @Inject constructor() {
    suspend operator fun invoke(source: Bitmap, corners: DocumentCorners): Bitmap = withContext(
        Dispatchers.Default) {
        val srcPoints = corners.toPixelPoints(source.width, source.height)
        val (outputWidth, outputHeight) = computeOutputSize(srcPoints)

        source.warpPerspective(srcPoints, outputWidth, outputHeight) ?: source
    }
}