package com.example.smartreceiptsplitter.domain.usecase.scan

import android.graphics.Bitmap
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

//Custom implementation, no OPenCV / ML Kit Document Scanner
// downscale -> grayscale -> 3x3 box blur -> Sobel gradient magnitude -> adaptive threshold -> extreme-point corner search
class DetectDocumentEdgesUseCase @Inject constructor() {

    suspend operator fun invoke(source: Bitmap): DocumentCorners = withContext(Dispatchers.Default) {
        val working = source.downscale(WORKING_MAX_DIMENSION)
        val gradient = working.toGrayscale().boxBlur3x3().sobelMagnitude()
        val threshold = gradient.adaptiveThreshold()
        val edgePixels = gradient.strongPixelCoordinates(threshold)

        edgePixels.extremeQuad(working.width, working.height) ?: DocumentCorners.fullFrame()
    }

    private companion object {
        /** Longer side of the downscaled working copy — detection doesn't need full resolution. */
        const val WORKING_MAX_DIMENSION = 480
    }
}