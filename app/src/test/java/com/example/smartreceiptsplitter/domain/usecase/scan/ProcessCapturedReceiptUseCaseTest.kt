package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.model.DocumentCorners
import com.example.smartreceiptsplitter.domain.model.NormalizedPoint
import com.example.smartreceiptsplitter.domain.repository.DetectedReceipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageProcessor
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProcessCapturedReceiptUseCaseTest {
    private class FakeProcessor(private val detectedCorners: DocumentCorners = DocumentCorners.fullFrame()) : ReceiptImageProcessor {
        var receivedRotation: Int? = null
        var receivedCorners: DocumentCorners? = null
        override suspend fun detect(jpeg: ByteArray, rotationDegrees: Int): DetectedReceipt {
            receivedRotation = rotationDegrees
            return DetectedReceipt(previewJpeg = byteArrayOf(7), corners = detectedCorners)
        }
        override suspend fun correct(jpeg: ByteArray, rotationDegrees: Int, corners: DocumentCorners): ByteArray {
            receivedRotation = rotationDegrees
            receivedCorners = corners
            return byteArrayOf(9, 9)
        }
    }

    private class FakeStorage : ReceiptImageStorage {
        val saved = mutableListOf<ByteArray>()
        val deleted = mutableListOf<String>()
        override suspend fun save(jpeg: ByteArray): String {
            saved += jpeg
            return "/receipts/receipt_${saved.size}.jpg"
        }
        override suspend fun delete(path: String): Boolean {
            deleted += path
            return true
        }
    }

    // a receipt lying a bit tilted in the middle of the photo, as the user left it on the review
    private val userCorners = DocumentCorners(
        topLeft = NormalizedPoint(0.2f, 0.1f),
        topRight = NormalizedPoint(0.8f, 0.15f),
        bottomRight = NormalizedPoint(0.75f, 0.9f),
        bottomLeft = NormalizedPoint(0.25f, 0.85f),
        isConfident = false
    )

    @Test
    fun photoIsCroppedToTheCornersFromTheReviewAndSaved() {
        val processor = FakeProcessor()
        val storage = FakeStorage()
        val path = runBlocking { ProcessCapturedReceiptUseCase(processor, storage)(byteArrayOf(1, 2, 3), 90, userCorners) }

        assertEquals(userCorners, processor.receivedCorners)
        assertEquals(90, processor.receivedRotation)
        assertArrayEquals(byteArrayOf(9, 9), storage.saved.single()) // the corrected image, not the photo
        assertEquals("/receipts/receipt_1.jpg", path)
    }

    @Test
    fun cornersOutsideThePhotoAreKeptInside() {
        val processor = FakeProcessor()
        val dragged = userCorners.copy(topLeft = NormalizedPoint(-0.2f, -0.1f), bottomRight = NormalizedPoint(1.3f, 1.05f))
        runBlocking { ProcessCapturedReceiptUseCase(processor, FakeStorage())(byteArrayOf(1), 0, dragged) }
        assertEquals(NormalizedPoint(0f, 0f), processor.receivedCorners?.topLeft)
        assertEquals(NormalizedPoint(1f, 1f), processor.receivedCorners?.bottomRight)
    }

    @Test
    fun foldedCornersAreRejectedAndNothingIsSaved() {
        val processor = FakeProcessor()
        val storage = FakeStorage()
        // top right and bottom right swapped: the sides cross
        val folded = userCorners.copy(topRight = userCorners.bottomRight, bottomRight = userCorners.topRight)
        try {
            runBlocking { ProcessCapturedReceiptUseCase(processor, storage)(byteArrayOf(1), 0, folded) }
            fail("folded corners must be rejected")
        } catch (e: IllegalArgumentException) {
            assertNull(processor.receivedCorners)
            assertTrue(storage.saved.isEmpty())
        }
    }

    @Test
    fun detectionReturnsThePreviewAndCornersWithoutSaving() {
        val processor = FakeProcessor(detectedCorners = userCorners.copy(isConfident = true))
        val result = runBlocking { DetectReceiptUseCase(processor)(byteArrayOf(1), 270) }
        assertEquals(270, processor.receivedRotation)
        assertArrayEquals(byteArrayOf(7), result.previewJpeg)
        assertEquals(userCorners.copy(isConfident = true), result.corners)
    }

    @Test
    fun unusableDetectionStartsFromTheWholePhoto() {
        val tiny = DocumentCorners(
            NormalizedPoint(0.5f, 0.5f),
            NormalizedPoint(0.52f, 0.5f),
            NormalizedPoint(0.52f, 0.52f),
            NormalizedPoint(0.5f, 0.52f),
            true
        )
        val result = runBlocking { DetectReceiptUseCase(FakeProcessor(detectedCorners = tiny))(byteArrayOf(1), 0) }
        assertEquals(DocumentCorners.fullFrame(), result.corners)
        assertFalse(result.corners.isConfident)
    }

    @Test
    fun deleteGoesThroughTheStorage() {
        val storage = FakeStorage()
        assertTrue(runBlocking { DeleteReceiptImageUseCase(storage)("/receipts/receipt_1.jpg") })
        assertEquals(listOf("/receipts/receipt_1.jpg"), storage.deleted)
    }
}