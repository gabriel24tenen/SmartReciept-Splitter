package com.example.smartreceiptsplitter.domain.model

import kotlin.math.abs


data class NormalizedPoint(val x: Float, val y: Float) {
    // same point kept inside the image
    fun clamped() = NormalizedPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}
enum class DocumentCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
}
data class DocumentCorners(
    val topLeft: NormalizedPoint,
    val topRight: NormalizedPoint,
    val bottomRight: NormalizedPoint,
    val bottomLeft: NormalizedPoint,
    val isConfident: Boolean,
) {
    operator fun get(corner: DocumentCorner): NormalizedPoint = when (corner) {
        DocumentCorner.TOP_LEFT -> topLeft
        DocumentCorner.TOP_RIGHT -> topRight
        DocumentCorner.BOTTOM_RIGHT -> bottomRight
        DocumentCorner.BOTTOM_LEFT -> bottomLeft
    }

    // one corner moved by the user, kept inside the image
    fun with(corner: DocumentCorner, point: NormalizedPoint): DocumentCorners {
        val inside = point.clamped()
        return when (corner) {
            DocumentCorner.TOP_LEFT -> copy(topLeft = inside)
            DocumentCorner.TOP_RIGHT -> copy(topRight = inside)
            DocumentCorner.BOTTOM_RIGHT -> copy(bottomRight = inside)
            DocumentCorner.BOTTOM_LEFT -> copy(bottomLeft = inside)
        }
    }

    fun clamped() = copy(topLeft = topLeft.clamped(), topRight = topRight.clamped(), bottomRight = bottomRight.clamped(), bottomLeft = bottomLeft.clamped())

    fun isUsableCrop(): Boolean {
        val points = listOf(topLeft, topRight, bottomRight, bottomLeft)
        val turnsClockwise = points.indices.all { i ->
            val a = points[i]
            val b = points[(i + 1) % 4]
            val c = points[(i + 2) % 4]
            (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x) > 0f
        }
        return turnsClockwise && area(points) >= MIN_CROP_AREA
    }

    private fun area(points: List<NormalizedPoint>): Float {
        var sum = 0f
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += a.x * b.y - b.x * a.y
        }
        return abs(sum) / 2f
    }

    companion object {
        //the crop has to cover at least 2 % of the photo, a smaller one is a slip of the finger
        const val MIN_CROP_AREA = 0.02f
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

