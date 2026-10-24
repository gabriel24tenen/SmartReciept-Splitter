package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptFilter
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetReceiptsUseCaseTest {

    private class FakeReceiptRepository(private val stored: List<Receipt>) : ReceiptRepository {
        var lastArgs: Triple<Long?, Long?, String?>? = null
        override fun getAllReceipts(): Flow<List<Receipt>> = flowOf(stored)
        override fun getReceipts(dateFrom: Long?, dateToExclusive: Long?, currency: String?): Flow<List<Receipt>> {
            lastArgs = Triple(dateFrom, dateToExclusive, currency)
            return flowOf(stored)
        }
        override fun getCurrencies(): Flow<List<String>> = flowOf(stored.map { it.currency }.distinct().sorted())
        override suspend fun getReceiptById(id: Long): Receipt? = stored.firstOrNull { it.id == id }
        override suspend fun addReceipt(receipt: Receipt): Long = 0
        override suspend fun updateReceipt(receipt: Receipt) = Unit
        override suspend fun deleteReceipt(receipt: Receipt) = Unit
    }

    private fun receipt(id: Long, merchant: String?, currency: String = "EUR") = Receipt(
        id = id, merchantName = merchant, date = 0, totalAmount = 1000, taxAmount = null, tipAmount = null,
        currency = currency, homeCurrencyAmount = null, exchangeRateUsed = null, categoryId = null,
        imagePath = null, isManualEntry = true, paidByPersonId = null
    )

    private val stored = listOf(receipt(1, "Konzum"), receipt(2, "Žabac"), receipt(3, null), receipt(4, "Konzum Plus"))

    private fun ids(repository: FakeReceiptRepository, filter: ReceiptFilter? = null): List<Long> = runBlocking {
        val useCase = GetReceiptsUseCase(repository)
        (if (filter == null) useCase() else useCase(filter)).first().map { it.id }
    }

    @Test
    fun withoutFilterEverythingIsListedInTheRepositoryOrder() {
        val repository = FakeReceiptRepository(stored)
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(repository))
        assertEquals(Triple<Long?, Long?, String?>(null, null, null), repository.lastArgs)
    }

    @Test
    fun merchantQueryNarrowsTheList() {
        assertEquals(listOf(1L, 4L), ids(FakeReceiptRepository(stored), ReceiptFilter(merchantQuery = "konzum")))
        assertEquals(listOf(2L), ids(FakeReceiptRepository(stored), ReceiptFilter(merchantQuery = "zabac")))
    }

    @Test
    fun receiptsWithoutAMerchantAreHiddenBySearchButShownWithoutIt() {
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(FakeReceiptRepository(stored), ReceiptFilter(merchantQuery = " ")))
        assertEquals(emptyList<Long>(), ids(FakeReceiptRepository(stored), ReceiptFilter(merchantQuery = "xyz")))
    }

    @Test
    fun dateRangeAndCurrencyAreHandedToTheRepository() {
        val repository = FakeReceiptRepository(stored)
        ids(repository, ReceiptFilter(dateFrom = 100, dateToExclusive = 200, currency = "USD"))
        assertEquals(Triple<Long?, Long?, String?>(100L, 200L, "USD"), repository.lastArgs)
    }
}