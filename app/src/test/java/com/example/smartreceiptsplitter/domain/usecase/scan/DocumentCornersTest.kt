package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorner
import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentCornersTest {

    private val receipt = DocumentCorners(
        topLeft = NormalizedPoint(0.2f, 0.1f),
        topRight = NormalizedPoint(0.8f, 0.15f),
        bottomRight = NormalizedPoint(0.75f, 0.9f),
        bottomLeft = NormalizedPoint(0.25f, 0.85f),
        isConfident = true
    )

    @Test
    fun movedCornerChangesOnlyThatCornerAndStaysInThePhoto() {
        val moved = receipt.with(DocumentCorner.BOTTOM_RIGHT, NormalizedPoint(0.7f, 0.95f))
        assertEquals(NormalizedPoint(0.7f, 0.95f), moved[DocumentCorner.BOTTOM_RIGHT])
        assertEquals(receipt.topLeft, moved.topLeft)
        assertEquals(receipt.topRight, moved.topRight)
        assertEquals(receipt.bottomLeft, moved.bottomLeft)
        assertTrue(moved.isConfident) // isConfident describes the detection, not the user's corners

        assertEquals(NormalizedPoint(1f, 0f), receipt.with(DocumentCorner.TOP_RIGHT, NormalizedPoint(1.4f, -0.3f)).topRight)
    }

    @Test
    fun getReturnsEveryCorner() {
        assertEquals(receipt.topLeft, receipt[DocumentCorner.TOP_LEFT])
        assertEquals(receipt.topRight, receipt[DocumentCorner.TOP_RIGHT])
        assertEquals(receipt.bottomRight, receipt[DocumentCorner.BOTTOM_RIGHT])
        assertEquals(receipt.bottomLeft, receipt[DocumentCorner.BOTTOM_LEFT])
    }

    @Test
    fun convexQuadAndWholePhotoAreUsable() {
        assertTrue(receipt.isUsableCrop())
        assertTrue(DocumentCorners.fullFrame().isUsableCrop())
    }

    @Test
    fun crossedSidesAreNotUsable() {
        // two corners swapped: an hourglass instead of a receipt
        assertFalse(receipt.copy(topRight = receipt.bottomRight, bottomRight = receipt.topRight).isUsableCrop())
        // corners given counterclockwise: the photo would come out mirrored
        assertFalse(receipt.copy(topRight = receipt.bottomLeft, bottomLeft = receipt.topRight).isUsableCrop())
    }

    @Test
    fun cornerPushedInsideTheQuadIsNotUsable() {
        // bottom right dragged inside: concave "arrowhead"
        assertFalse(receipt.with(DocumentCorner.BOTTOM_RIGHT, NormalizedPoint(0.45f, 0.45f)).isUsableCrop())
    }

    @Test
    fun tinyOrFlatQuadIsNotUsable() {
        val tiny = DocumentCorners(
            NormalizedPoint(0.5f, 0.5f), NormalizedPoint(0.6f, 0.5f), NormalizedPoint(0.6f, 0.6f), NormalizedPoint(0.5f, 0.6f), true
        )
        assertFalse(tiny.isUsableCrop()) // 1 % of the photo
        val flat = DocumentCorners(
            NormalizedPoint(0.1f, 0.5f), NormalizedPoint(0.9f, 0.5f), NormalizedPoint(0.9f, 0.5f), NormalizedPoint(0.1f, 0.5f), true
        )
        assertFalse(flat.isUsableCrop())
    }
}
