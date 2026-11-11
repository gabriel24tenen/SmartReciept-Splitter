package com.example.smartreceiptsplitter.domain.usecase.split

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.SplitSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitConsistencyTest {

    private val receipt = Receipt(
        id = 7, merchantName = "Pizzeria", date = 1_000, totalAmount = 2000, taxAmount = 400, tipAmount = 200,
        currency = "EUR", homeCurrencyAmount = null, exchangeRateUsed = null, categoryId = null,
        imagePath = null, isManualEntry = true, paidByPersonId = null
    )

    // 2000 total (400 tax) + 200 tip, split evenly between two people
    private val split = listOf(
        ReceiptSplit(receiptId = 7, personId = 1, amountOwed = 1100, taxShare = 200, tipShare = 100),
        ReceiptSplit(receiptId = 7, personId = 2, amountOwed = 1100, taxShare = 200, tipShare = 100)
    )

    @Test
    fun splitThatAddsUpToTheReceiptIsNotOutdated() {
        assertFalse(split.isOutdatedFor(receipt))
    }

    @Test
    fun noSplitIsNeverOutdated() {
        assertFalse(emptyList<ReceiptSplit>().isOutdatedFor(receipt))
    }

    @Test
    fun changedTotalTaxOrTipMakesTheSplitOutdated() {
        assertTrue(split.isOutdatedFor(receipt.copy(totalAmount = 2500)))
        assertTrue(split.isOutdatedFor(receipt.copy(taxAmount = 300)))
        assertTrue(split.isOutdatedFor(receipt.copy(tipAmount = null)))
    }

    @Test
    fun missingTaxAndTipCountAsZero() {
        val plain = receipt.copy(taxAmount = null, tipAmount = null)
        val plainSplit = listOf(ReceiptSplit(personId = 1, amountOwed = 1000), ReceiptSplit(personId = 2, amountOwed = 1000))
        assertFalse(plainSplit.isOutdatedFor(plain))
    }

    @Test
    fun withAmountsOfRebuildsTheAmountsTheSplitWasMadeFor() {
        val old = receipt.copy(totalAmount = 9999, taxAmount = 1, tipAmount = null).withAmountsOf(split)
        assertEquals(2000L, old.totalAmount)
        assertEquals(400L, old.taxAmount)
        assertEquals(200L, old.tipAmount)
    }

    @Test
    fun summariesCountPeopleAndSkipReceiptsWithoutASplit() {
        val other = receipt.copy(id = 8)
        val summaries = splitSummaries(listOf(receipt, other), split)
        assertEquals(mapOf(7L to SplitSummary(participants = 2, isOutdated = false)), summaries)
    }

    @Test
    fun summariesIgnoreSplitsOfReceiptsNotInTheList() {
        assertTrue(splitSummaries(listOf(receipt.copy(id = 8)), split).isEmpty())
    }
}