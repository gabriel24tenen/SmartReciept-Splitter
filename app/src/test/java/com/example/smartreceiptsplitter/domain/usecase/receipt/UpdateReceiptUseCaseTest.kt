package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.ValidatedReceipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateReceiptUseCaseTest {

    private class FakeReceiptRepository : ReceiptRepository {
        val updated = mutableListOf<Receipt>()
        val deleted = mutableListOf<Receipt>()
        override fun getAllReceipts(): Flow<List<Receipt>> = flowOf(emptyList())
        override fun getReceipts(dateFrom: Long?, dateToExclusive: Long?, currency: String?): Flow<List<Receipt>> = flowOf(emptyList())
        override fun getCurrencies(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getReceiptById(id: Long): Receipt? = null
        override suspend fun addReceipt(receipt: Receipt): Long = 0
        override suspend fun updateReceipt(receipt: Receipt) { updated += receipt }
        override suspend fun deleteReceipt(receipt: Receipt) { deleted += receipt }
    }

    private class FakeSplitRepository(private val saved: List<ReceiptSplit>) : ReceiptSplitRepository {
        override fun observeSplits(receiptId: Long): Flow<List<ReceiptSplit>> = flowOf(saved)
        override suspend fun saveSplits(receiptId: Long, splits: List<ReceiptSplit>, receiptTipAmount: Long?) = Unit
    }

    private val existing = Receipt(
        id = 7, merchantName = "Konzum", date = 1_000, totalAmount = 1250, taxAmount = 250, tipAmount = null,
        currency = "USD", homeCurrencyAmount = 1150, exchangeRateUsed = 0.92, categoryId = 3,
        imagePath = "/files/receipts/receipt_1.jpg", isManualEntry = false, paidByPersonId = 5, createdAt = 555
    )

    private val edited = ValidatedReceipt(
        merchantName = "Konzum", date = 1_000, totalAmount = 1250, taxAmount = 250, tipAmount = null, currency = "USD"
    )

    private val oneSplit = listOf(ReceiptSplit(receiptId = 7, personId = 1, amountOwed = 1250))

    private fun update(
        edited: ValidatedReceipt,
        splits: List<ReceiptSplit> = emptyList(),
        repository: FakeReceiptRepository = FakeReceiptRepository()
    ): Pair<Boolean, Receipt> {
        val review = runBlocking { UpdateReceiptUseCase(repository, FakeSplitRepository(splits))(existing, edited) }
        return review to repository.updated.single()
    }

    @Test
    fun editedFieldsAreSaved() {
        val (_, saved) = update(edited.copy(merchantName = "Tommy", date = 2_000, totalAmount = 2000, taxAmount = null, tipAmount = 100, currency = "EUR"))
        assertEquals("Tommy", saved.merchantName)
        assertEquals(2_000L, saved.date)
        assertEquals(2000L, saved.totalAmount)
        assertNull(saved.taxAmount)
        assertEquals(100L, saved.tipAmount)
        assertEquals("EUR", saved.currency)
    }

    @Test
    fun everythingTheFormDoesNotShowStaysAsItWas() {
        val (_, saved) = update(edited.copy(merchantName = "Tommy"))
        assertEquals(7L, saved.id)
        assertEquals(555L, saved.createdAt)
        assertEquals("/files/receipts/receipt_1.jpg", saved.imagePath)
        assertFalse(saved.isManualEntry)
        assertEquals(3L, saved.categoryId)
        assertEquals(5L, saved.paidByPersonId)
    }

    @Test
    fun conversionStaysWhenOnlyTheMerchantOrDateChanged() {
        val (_, saved) = update(edited.copy(merchantName = "Tommy", date = 9_000))
        assertEquals(1150L, saved.homeCurrencyAmount)
        assertEquals(0.92, saved.exchangeRateUsed!!, 0.0)
    }

    @Test
    fun conversionIsClearedWhenTotalOrCurrencyChanged() {
        val (_, byTotal) = update(edited.copy(totalAmount = 1300))
        assertNull(byTotal.homeCurrencyAmount)
        assertNull(byTotal.exchangeRateUsed)

        val (_, byCurrency) = update(edited.copy(currency = "GBP"))
        assertNull(byCurrency.homeCurrencyAmount)
        assertNull(byCurrency.exchangeRateUsed)
    }

    @Test
    fun splitNeedsReviewOnlyWhenAmountsChangedAndASplitExists() {
        assertTrue(update(edited.copy(totalAmount = 1300), oneSplit).first)
        assertTrue(update(edited.copy(taxAmount = 100), oneSplit).first)
        assertTrue(update(edited.copy(tipAmount = 50), oneSplit).first)
        // nothing to review without a saved split
        assertFalse(update(edited.copy(totalAmount = 1300), emptyList()).first)
        // a change of name, date or currency doesn't touch the amounts of the split
        assertFalse(update(edited.copy(merchantName = "Tommy", date = 9_000, currency = "EUR"), oneSplit).first)
    }

    @Test
    fun deleteUseCaseDeletesTheGivenReceipt() {
        val repository = FakeReceiptRepository()
        runBlocking { DeleteReceiptUseCase(repository)(existing) }
        assertEquals(listOf(existing), repository.deleted)
    }
}
