package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class PerspectiveTransformTest {

    @Test
    fun toPixelPointsScalesNormalizedCornersToTheBitmapsActualSize() {
        val corners = DocumentCorners(
            topLeft = NormalizedPoint(0.1f, 0.2f),
            topRight = NormalizedPoint(0.9f, 0.2f),
            bottomRight = NormalizedPoint(0.9f, 0.8f),
            bottomLeft = NormalizedPoint(0.1f, 0.8f),
            isConfident = true
        )

        val pixels = corners.toPixelPoints(width = 1000, height = 500)
        assertEquals(100f, pixels[0], 0.01f) // topLeft.x
        assertEquals(100f, pixels[1], 0.01f) // topLeft.y
        assertEquals(900f, pixels[2], 0.01f) // topRight.x
        assertEquals(100f, pixels[3], 0.01f) // topRight.y
        assertEquals(900f, pixels[4], 0.01f) // bottomRight.x
        assertEquals(400f, pixels[5], 0.01f) // bottomRight.y
        assertEquals(100f, pixels[6], 0.01f) // bottomLeft.x
        assertEquals(400f, pixels[7], 0.01f) // bottomLeft.y
    }

    @Test
    fun computeOutputSizeTakesTheLongerOfEachPairOfOppositeEdges() {
        val srcPoints = floatArrayOf(
            50f, 0f,    // topLeft
            150f, 0f,   // topRight -> top edge = 100px
            200f, 300f, // bottomRight
            0f, 300f   // bottomLeft -> bottom edge = 200px
        )
        val (width, height) = computeOutputSize(srcPoints)
        assertEquals(200, width)
        assertEquals(304, height)
    }

    @Test
    fun computeOutputSizeClampsToTheMaximumOutputDimension() {
        val hugeSquare = floatArrayOf(
            0f, 0f,
            5000f, 0f,
            5000f, 5000f,
            0f, 5000f
        )
        val (width, height) = computeOutputSize(hugeSquare)
        assertEquals(2000, width)
        assertEquals(2000, height)
    }
}