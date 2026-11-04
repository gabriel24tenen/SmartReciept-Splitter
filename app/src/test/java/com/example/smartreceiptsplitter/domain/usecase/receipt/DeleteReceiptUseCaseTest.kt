package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptImageStorage
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DeleteReceiptUseCaseTest {

    private val events = mutableListOf<String>()

    private inner class FakeReceiptRepository(private val failure: Exception? = null) : ReceiptRepository {
        override suspend fun deleteReceipt(receipt: Receipt) {
            failure?.let { throw it }
            events += "row ${receipt.id}"
        }
        override suspend fun getReceiptById(id: Long): Receipt? = null
        override fun observeReceipt(id: Long): Flow<Receipt?> = flowOf(null)
        override suspend fun addReceipt(receipt: Receipt): Long = 0
        override suspend fun updateReceipt(receipt: Receipt) = Unit
        override fun getReceipts(dateFrom: Long?, dateToExclusive: Long?, currency: String?): Flow<List<Receipt>> = flowOf(emptyList())
        override fun getCurrencies(): Flow<List<String>> = flowOf(emptyList())
    }

    private inner class FakeImageStorage(private val failure: Exception? = null) : ReceiptImageStorage {
        override suspend fun save(jpeg: ByteArray): String = error("not used")
        override suspend fun delete(path: String): Boolean {
            failure?.let { throw it }
            events += "image $path"
            return true
        }
    }

    private val scanned = Receipt(
        id = 7, merchantName = "Konzum", date = 1_000, totalAmount = 1250, taxAmount = 250, tipAmount = null,
        currency = "EUR", homeCurrencyAmount = null, exchangeRateUsed = null, categoryId = null,
        imagePath = "/files/receipts/receipt_1.jpg", isManualEntry = false, paidByPersonId = null
    )
    private val manual = scanned.copy(id = 8, imagePath = null, isManualEntry = true)

    private fun delete(
        receipt: Receipt,
        repository: ReceiptRepository = FakeReceiptRepository(),
        storage: ReceiptImageStorage = FakeImageStorage()
    ) = runBlocking { DeleteReceiptUseCase(repository, storage)(receipt) }

    @Test
    fun scannedReceiptLosesRowAndThenImage() {
        delete(scanned)
        assertEquals(listOf("row 7", "image /files/receipts/receipt_1.jpg"), events)
    }

    @Test
    fun manualReceiptHasNoImageToDelete() {
        delete(manual)
        assertEquals(listOf("row 8"), events)
    }

    @Test
    fun failedImageDeleteDoesNotFailTheDelete() {
        delete(scanned, storage = FakeImageStorage(failure = SecurityException("no access")))
        assertEquals(listOf("row 7"), events)
    }

    @Test
    fun imageStaysWhenTheRowCanNotBeDeleted() {
        try {
            delete(scanned, repository = FakeReceiptRepository(failure = IllegalStateException("db closed")))
            fail("the repository error must reach the caller")
        } catch (e: IllegalStateException) {
            assertEquals("db closed", e.message)
        }
        assertTrue(events.isEmpty())
    }

    @Test
    fun cancellationIsNotSwallowed() {
        try {
            delete(scanned, storage = FakeImageStorage(failure = CancellationException("left the screen")))
            fail("cancellation must reach the caller")
        } catch (e: CancellationException) {
            assertEquals("left the screen", e.message)
        }
    }
}