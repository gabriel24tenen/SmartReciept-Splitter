package com.example.smartreceiptsplitter.ui.presentation.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorner
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import kotlin.math.hypot
import kotlin.math.min

internal data class FitMapping(
    val imageWidth: Int,
    val imageHeight: Int,
    val viewWidth: Float,
    val viewHeight: Float,
    val inset: Float = 0f
) {
    private val areaWidth = (viewWidth - 2 * inset).coerceAtLeast(1f)
    private val areaHeight = (viewHeight - 2 * inset).coerceAtLeast(1f)
    private val scale = min(areaWidth / imageWidth, areaHeight / imageHeight)
    private val shownWidth = imageWidth * scale
    private val shownHeight = imageHeight * scale
    private val left = (viewWidth - shownWidth) / 2f
    private val top = (viewHeight - shownHeight) / 2f

    fun toView(point: NormalizedPoint): Pair<Float, Float> = left + point.x * shownWidth to top + point.y * shownHeight
    fun toImage(x: Float, y: Float): NormalizedPoint =
        NormalizedPoint((x - left) / shownWidth, (y - top) / shownHeight).clamped()
}

internal fun DocumentCorners.cornerAt(x: Float, y: Float, mapping: FitMapping, maxDistance: Float): DocumentCorner? =
    DocumentCorner.entries
        .map { corner -> corner to mapping.toView(this[corner]).let { (cx, cy) -> hypot(cx - x, cy -y) } }
        .filter { (_, distance) -> distance <= maxDistance }
        .minByOrNull { (_, distance) -> distance }
        ?.first