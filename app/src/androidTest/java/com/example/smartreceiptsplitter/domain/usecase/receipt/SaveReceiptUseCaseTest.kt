package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ValidatedReceipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveReceiptUseCaseTest {

    private class FakeReceiptRepository(private val newId: Long = 42) : ReceiptRepository {
        val added = mutableListOf<Receipt>()
        override fun getAllReceipts(): Flow<List<Receipt>> = flowOf(added.toList())
        override suspend fun getReceiptById(id: Long): Receipt? = null
        override suspend fun addReceipt(receipt: Receipt): Long {
            added += receipt
            return newId
        }
        override suspend fun updateReceipt(receipt: Receipt) = Unit
        override suspend fun deleteReceipt(receipt: Receipt) = Unit
        override fun getCurrencies(): Flow<List<String>> = flowOf(added.map { it.currency }.distinct())
        override fun getReceipts(
            dateFrom: Long?,
            dateToExclusive: Long?,
            currency: String?
        ): Flow<List<Receipt>> = flowOf(added.toList())
    }

    private val validated = ValidatedReceipt(
        merchantName = "Konzum",
        date = 1_792_000_000_000,
        totalAmount = 1250,
        taxAmount = 250,
        tipAmount = 100,
        currency = "EUR"
    )

    private fun save(repository: FakeReceiptRepository, isManualEntry: Boolean, imagePath: String? = null): Long =
        runBlocking { SaveReceiptUseCase(repository)(validated, isManualEntry, imagePath) }

    @Test
    fun manualEntryIsMarkedAsManual() {
        val repository = FakeReceiptRepository()
        save(repository, isManualEntry = true)
        assertTrue(repository.added.single().isManualEntry)
        assertNull(repository.added.single().imagePath)
    }

    @Test
    fun correctedOcrResultIsNotMarkedAsManualAndKeepsItsImage() {
        val repository = FakeReceiptRepository()
        save(repository, isManualEntry = false, imagePath = "/files/receipts/receipt_1.jpg")
        assertFalse(repository.added.single().isManualEntry)
        assertEquals("/files/receipts/receipt_1.jpg", repository.added.single().imagePath)
    }

    @Test
    fun allValidatedFieldsAreSaved() {
        val repository = FakeReceiptRepository()
        save(repository, isManualEntry = true)
        val receipt = repository.added.single()
        assertEquals(0L, receipt.id) // the database assigns the real id
        assertEquals("Konzum", receipt.merchantName)
        assertEquals(validated.date, receipt.date)
        assertEquals(1250L, receipt.totalAmount)
        assertEquals(250L, receipt.taxAmount)
        assertEquals(100L, receipt.tipAmount)
        assertEquals("EUR", receipt.currency)
    }

    @Test
    fun fieldsOfLaterFeaturesStayEmpty() {
        val repository = FakeReceiptRepository()
        save(repository, isManualEntry = true)
        val receipt = repository.added.single()
        assertNull(receipt.homeCurrencyAmount)
        assertNull(receipt.exchangeRateUsed)
        assertNull(receipt.categoryId)
        assertNull(receipt.paidByPersonId)
    }

    @Test
    fun optionalFieldsMayBeMissing() {
        val repository = FakeReceiptRepository()
        runBlocking {
            SaveReceiptUseCase(repository)(validated.copy(merchantName = "", taxAmount = null, tipAmount = null), isManualEntry = true)
        }
        val receipt = repository.added.single()
        assertNull(receipt.merchantName)
        assertNull(receipt.taxAmount)
        assertNull(receipt.tipAmount)
    }

    @Test
    fun returnsTheIdFromTheRepository() {
        assertEquals(7L, save(FakeReceiptRepository(newId = 7), isManualEntry = true))
    }
}