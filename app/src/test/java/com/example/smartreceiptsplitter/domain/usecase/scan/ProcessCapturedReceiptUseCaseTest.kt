package com.example.smartreceiptsplitter.domain.usecase.scan

import com.example.smartreceiptsplitter.domain.repository.CorrectedReceiptImage
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageProcessor
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessCapturedReceiptUseCaseTest {

    private class FakeProcessor(private val isConfident: Boolean) : ReceiptImageProcessor {
        var receivedRotation: Int? = null
        override suspend fun correct(jpeg: ByteArray, rotationDegrees: Int): CorrectedReceiptImage {
            receivedRotation = rotationDegrees
            return CorrectedReceiptImage(jpeg = byteArrayOf(9, 9), isConfident = isConfident)
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

    @Test
    fun correctedImageIsSavedAndItsPathReturned() {
        val processor = FakeProcessor(isConfident = true)
        val storage = FakeStorage()
        val result = runBlocking { ProcessCapturedReceiptUseCase(processor, storage)(byteArrayOf(1, 2, 3), rotationDegrees = 90) }

        assertEquals(90, processor.receivedRotation)
        assertArrayEquals(byteArrayOf(9, 9), storage.saved.single()) // the corrected image, not the photo
        assertEquals(ScannedReceiptImage("/receipts/receipt_1.jpg", isConfident = true), result)
    }

    @Test
    fun uncertainEdgesAreReported() {
        val result = runBlocking { ProcessCapturedReceiptUseCase(FakeProcessor(isConfident = false), FakeStorage())(byteArrayOf(1), 0) }
        assertFalse(result.isConfident)
    }

    @Test
    fun deleteGoesThroughTheStorage() {
        val storage = FakeStorage()
        assertTrue(runBlocking { DeleteReceiptImageUseCase(storage)("/receipts/receipt_1.jpg") })
        assertEquals(listOf("/receipts/receipt_1.jpg"), storage.deleted)
    }
}