package com.example.smartreceiptsplitter.domain.usecase.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentEdgeAlgorithmsTest {

    @Test
    fun findsCornersOfACleanRectangleOfEdgePoints() {
        val points = rectangleOutline(left = 20, top = 20, right = 80, bottom = 80)
        val corners = points.extremeQuad(imageWidth = 100, imageHeight = 100)

        assertTrue(corners != null)
        corners!!
        assertTrue(corners.isConfident)
        assertEquals(0.20f, corners.topLeft.x, 0.01f)
        assertEquals(0.20f, corners.topLeft.y, 0.01f)
        assertEquals(0.80f, corners.bottomRight.x, 0.01f)
        assertEquals(0.80f, corners.bottomRight.y, 0.01f)
        assertEquals(0.80f, corners.topRight.x, 0.01f)
        assertEquals(0.20f, corners.topRight.y, 0.01f)
        assertEquals(0.20f, corners.bottomLeft.x, 0.01f)
        assertEquals(0.80f, corners.bottomLeft.y, 0.01f)
    }

    @Test
    fun returnsNullWhenThereAreTooFewEdgePoints() {
        val points = listOf(10 to 10, 11 to 11, 12 to 12)
        val corners = points.extremeQuad(imageWidth = 100, imageHeight = 100)
        assertNull(corners)
    }

    @Test
    fun rejectsQuadThatIsTooSmallRelativeToImage() {
        val tinyCluster = rectangleOutline(left = 0, top = 0, right = 5, bottom = 5)
        val padding = List(50) { i -> (i % 5) to (i / 5) }
        val corners = (tinyCluster + padding).extremeQuad(imageWidth = 1000, imageHeight = 1000)
        assertNull(corners)
    }

    private fun rectangleOutline(left: Int, top: Int, right: Int, bottom: Int): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        for (x in left..right) {
            points += x to top
            points += x to bottom
        }
        for (y in top..bottom) {
            points += left to y
            points += right to y
        }
        return points
    }
}