package com.example.smartreceiptsplitter.domain.model


data class NormalizedPoint(val x: Float, val y: Float)

data class DocumentCorners(
    val topLeft: NormalizedPoint,
    val topRight: NormalizedPoint,
    val bottomRight: NormalizedPoint,
    val bottomLeft: NormalizedPoint,
    val isConfident: Boolean,
) {
    companion object {
        // Detection failed or found nothing plausible — assume the whole frame is the document
        fun fullFrame() = DocumentCorners(
            topLeft = NormalizedPoint(0f, 0f),
            topRight = NormalizedPoint(1f, 0f),
            bottomRight = NormalizedPoint(1f, 1f),
            bottomLeft = NormalizedPoint(0f, 1f),
            isConfident = false,
        )
    }
}

