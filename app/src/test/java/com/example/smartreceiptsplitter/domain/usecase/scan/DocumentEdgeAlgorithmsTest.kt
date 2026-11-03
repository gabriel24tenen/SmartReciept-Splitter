package com.example.smartreceiptsplitter.domain.usecase.scan

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentEdgeAlgorithmsTest {

    @Test
    fun findsCornersOfACleanRectangleOfEdgePoints() {
        val points = rectangleOutline(left = 20, top = 20, right = 80, bottom = 80)
        val corners = points.extremeQuad(imageWidth = 100, imageHeight = 100)

        Assert.assertTrue(corners != null)
        corners!!
        Assert.assertTrue(corners.isConfident)
        Assert.assertEquals(0.20f, corners.topLeft.x, 0.01f)
        Assert.assertEquals(0.20f, corners.topLeft.y, 0.01f)
        Assert.assertEquals(0.80f, corners.bottomRight.x, 0.01f)
        Assert.assertEquals(0.80f, corners.bottomRight.y, 0.01f)
        Assert.assertEquals(0.80f, corners.topRight.x, 0.01f)
        Assert.assertEquals(0.20f, corners.topRight.y, 0.01f)
        Assert.assertEquals(0.20f, corners.bottomLeft.x, 0.01f)
        Assert.assertEquals(0.80f, corners.bottomLeft.y, 0.01f)
    }

    @Test
    fun returnsNullWhenThereAreTooFewEdgePoints() {
        val points = listOf(10 to 10, 11 to 11, 12 to 12)
        val corners = points.extremeQuad(imageWidth = 100, imageHeight = 100)
        Assert.assertNull(corners)
    }

    @Test
    fun rejectsQuadThatIsTooSmallRelativeToImage() {
        val tinyCluster = rectangleOutline(left = 0, top = 0, right = 5, bottom = 5)
        val padding = List(50) { i -> (i % 5) to (i / 5) }
        val corners = (tinyCluster + padding).extremeQuad(imageWidth = 1000, imageHeight = 1000)
        Assert.assertNull(corners)
    }

    @Test
    fun detectsThePaperOnADarkBackground() {
        // 120 x 160 dark image with a light "receipt" from (20, 30) to (100, 140)
        val width = 120
        val height = 160
        val pixels = FloatArray(width * height) { i ->
            val x = i % width
            val y = i / width
            if (x in 20..100 && y in 30..140) 230f else 40f
        }
        val corners = GrayscaleBuffer(width, height, pixels).detectDocumentCorners()

        assertTrue(corners.isConfident)
        assertEquals(20f / width, corners.topLeft.x, 0.03f)
        assertEquals(30f / height, corners.topLeft.y, 0.03f)
        assertEquals(100f / width, corners.bottomRight.x, 0.03f)
        assertEquals(140f / height, corners.bottomRight.y, 0.03f)
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