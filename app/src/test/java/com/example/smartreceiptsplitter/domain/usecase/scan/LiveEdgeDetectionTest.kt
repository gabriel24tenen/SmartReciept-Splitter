package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

class LiveEdgeDetectionTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun smallFrameIsCopiedPixelByPixel() {
        val gray = lumaToGrayscale(bytes(10, 20, 30, 40, 50, 60), width = 3, height = 2, rowStride = 3, pixelStride = 1, maxDimension = 480)
        assertEquals(3, gray.width)
        assertEquals(2, gray.height)
        assertArrayEquals(floatArrayOf(10f, 20f, 30f, 40f, 50f, 60f), gray.pixels, 0f)
    }

    @Test
    fun largeFrameIsAveragedInBlocksAndRowPaddingIsSkipped() {
        // 4 x 2 frame, each row padded to 6 bytes; at most 2 px -> 2 x 2 blocks -> 2 x 1
        val luma = bytes(
            10, 20, 30, 40, 99, 99,
            30, 40, 50, 60, 99, 99
        )
        val gray = lumaToGrayscale(luma, width = 4, height = 2, rowStride = 6, pixelStride = 1, maxDimension = 2)
        assertEquals(2, gray.width)
        assertEquals(1, gray.height)
        assertArrayEquals(floatArrayOf(25f, 45f), gray.pixels, 0.001f)
    }

    @Test
    fun pixelStrideAndUnsignedBytesAreRespected() {
        // every second byte belongs to another channel; 0xFF is 255, not -1
        val gray = lumaToGrayscale(bytes(0xFF, 7, 0x80, 7), width = 2, height = 1, rowStride = 4, pixelStride = 2, maxDimension = 480)
        assertArrayEquals(floatArrayOf(255f, 128f), gray.pixels, 0f)
    }

    private val quad = DocumentCorners(
        topLeft = NormalizedPoint(0.1f, 0.2f),
        topRight = NormalizedPoint(0.8f, 0.1f),
        bottomRight = NormalizedPoint(0.9f, 0.7f),
        bottomLeft = NormalizedPoint(0.2f, 0.9f),
        isConfident = true
    )

    @Test
    fun quarterTurnMovesBottomLeftToTopLeft() {
        val upright = quad.rotated(90)
        // (x, y) -> (1 - y, x); the old bottom left becomes the top left
        assertEquals(NormalizedPoint(0.1f, 0.2f), upright.topLeft.rounded())
        assertEquals(NormalizedPoint(0.8f, 0.1f), upright.topRight.rounded())
        assertEquals(NormalizedPoint(0.9f, 0.8f), upright.bottomRight.rounded())
        assertEquals(NormalizedPoint(0.3f, 0.9f), upright.bottomLeft.rounded())
        assertTrue(upright.isConfident)
    }

    @Test
    fun halfAndThreeQuarterTurns() {
        val half = quad.rotated(180)
        assertEquals(NormalizedPoint(0.1f, 0.3f), half.topLeft.rounded()) // old bottom right
        assertEquals(NormalizedPoint(0.9f, 0.8f), half.bottomRight.rounded()) // old top left
        val threeQuarters = quad.rotated(270)
        // (x, y) -> (y, 1 - x), old top right becomes top left
        assertEquals(NormalizedPoint(0.1f, 0.2f), threeQuarters.topLeft.rounded())
        assertEquals(NormalizedPoint(0.2f, 0.9f), threeQuarters.bottomLeft.rounded())
    }

    @Test
    fun fourQuarterTurnsGiveTheSameCorners() {
        val turned = quad.rotated(90).rotated(90).rotated(90).rotated(90)
        assertEquals(quad.copy(), turned.roundedAll())
        assertEquals(quad.rotated(270).roundedAll(), quad.rotated(-90).roundedAll())
        assertEquals(quad, quad.rotated(360))
    }

    @Test
    fun rotationThatIsNotAQuarterTurnIsRejected() {
        try {
            quad.rotated(45)
            fail("45 degrees must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("45"))
        }
    }

    @Test
    fun onlyAConfidentQuadInsideTheFrameIsAnOutline() {
        assertTrue(quad.isReceiptOutline())
        assertFalse(quad.copy(isConfident = false).isReceiptOutline())
        assertFalse(DocumentCorners.fullFrame().copy(isConfident = true).isReceiptOutline())
        // almost the whole frame is still the frame
        val nearlyFull = DocumentCorners(
            NormalizedPoint(0.01f, 0.02f), NormalizedPoint(0.99f, 0f), NormalizedPoint(1f, 0.97f), NormalizedPoint(0.03f, 1f), true
        )
        assertFalse(nearlyFull.isReceiptOutline())
    }

    @Test
    fun receiptInALandscapeSensorFrameIsFoundUpright() {
        // sensor frame 160 x 120 (landscape), dark table, bright paper from x 40..119, y 30..89
        val width = 160
        val height = 120
        val luma = ByteArray(width * height) { i ->
            val x = i % width
            val y = i / width
            (if (x in 40..119 && y in 30..89) 230 else 40).toByte()
        }
        val corners = lumaToGrayscale(luma, width, height, rowStride = width, pixelStride = 1)
            .detectDocumentCorners()
            .rotated(90)
        assertTrue(corners.isReceiptOutline())
        // paper's top left in the sensor (0.25, 0.25) is its top right upright: (1 - 0.25, 0.25)
        assertNear(NormalizedPoint(0.75f, 0.25f), corners.topRight)
        assertNear(NormalizedPoint(0.25f, 0.25f), corners.topLeft)
        assertNear(NormalizedPoint(0.25f, 0.75f), corners.bottomLeft)
        assertNear(NormalizedPoint(0.75f, 0.75f), corners.bottomRight)
    }

    private fun assertNear(expected: NormalizedPoint, actual: NormalizedPoint) {
        assertTrue("expected $expected, was $actual", abs(expected.x - actual.x) < 0.05f && abs(expected.y - actual.y) < 0.05f)
    }

    private fun NormalizedPoint.rounded() = NormalizedPoint(Math.round(x * 1000) / 1000f, Math.round(y * 1000) / 1000f)
    private fun DocumentCorners.roundedAll() =
        copy(topLeft = topLeft.rounded(), topRight = topRight.rounded(), bottomRight = bottomRight.rounded(), bottomLeft = bottomLeft.rounded())
}