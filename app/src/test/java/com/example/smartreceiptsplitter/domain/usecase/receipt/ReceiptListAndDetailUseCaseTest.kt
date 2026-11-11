package com.example.smartreceiptsplitter.domain.usecase.receipt

import com.example.smartreceiptsplitter.domain.model.Person
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.model.SplitSummary
import com.example.smartreceiptsplitter.domain.repository.PersonRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptListAndDetailUseCaseTest {

    private fun receipt(id: Long, total: Long = 2000, tax: Long? = null, tip: Long? = null) = Receipt(
        id = id, merchantName = "Shop $id", date = 1_000, totalAmount = total, taxAmount = tax, tipAmount = tip,
        currency = "EUR", homeCurrencyAmount = null, exchangeRateUsed = null, categoryId = null,
        imagePath = null, isManualEntry = true, paidByPersonId = null
    )

    private val pizza = receipt(id = 1, total = 2000)
    private val coffee = receipt(id = 2, total = 300)
    private val pizzaSplit = listOf(
        ReceiptSplit(receiptId = 1, personId = 10, amountOwed = 1000),
        ReceiptSplit(receiptId = 1, personId = 11, amountOwed = 1000)
    )

    private class FakeReceipts(private val receipts: List<Receipt>) : ReceiptRepository {
        override suspend fun getReceiptById(id: Long): Receipt? = receipts.firstOrNull { it.id == id }
        override fun observeReceipt(id: Long): Flow<Receipt?> = flowOf(receipts.firstOrNull { it.id == id })
        override suspend fun addReceipt(receipt: Receipt): Long = 0
        override suspend fun updateReceipt(receipt: Receipt) = Unit
        override suspend fun deleteReceipt(receipt: Receipt) = Unit
        override fun getReceipts(dateFrom: Long?, dateToExclusive: Long?, currency: String?): Flow<List<Receipt>> = flowOf(receipts)
        override fun getCurrencies(): Flow<List<String>> = flowOf(listOf("EUR"))
    }

    private class FakeSplits(private val splits: List<ReceiptSplit>) : ReceiptSplitRepository {
        override fun observeSplits(receiptId: Long): Flow<List<ReceiptSplit>> = flowOf(splits.filter { it.receiptId == receiptId })
        override fun observeAllSplits(): Flow<List<ReceiptSplit>> = flowOf(splits)
        override suspend fun saveSplits(receiptId: Long, splits: List<ReceiptSplit>, receiptTipAmount: Long?) = Unit
        override suspend fun countSplitsOf(personId: Long): Int = splits.count { it.personId == personId }
    }

    private class FakePersons(private val people: List<Person>) : PersonRepository {
        override fun getAllPerson(): Flow<List<Person>> = flowOf(people)
        override suspend fun getOrCreateSelf(): Person = people.first { it.isSelf }
        override suspend fun addPerson(person: Person): Long = 0
        override suspend fun renamePerson(id: Long, name: String) = Unit
        override suspend fun deletePerson(id: Long) = Unit
    }

    // ---- list

    @Test
    fun listGivesASummaryOnlyForReceiptsWithASplit() {
        val items = runBlocking {
            GetReceiptListItemsUseCase(GetReceiptsUseCase(FakeReceipts(listOf(pizza, coffee))), FakeSplits(pizzaSplit))().first()
        }
        assertEquals(listOf(pizza, coffee), items.receipts)
        assertEquals(mapOf(1L to SplitSummary(participants = 2, isOutdated = false)), items.splitSummaries)
    }

    @Test
    fun listMarksASplitThatNoLongerAddsUp() {
        val edited = pizza.copy(totalAmount = 2500)
        val items = runBlocking {
            GetReceiptListItemsUseCase(GetReceiptsUseCase(FakeReceipts(listOf(edited))), FakeSplits(pizzaSplit))().first()
        }
        assertTrue(items.splitSummaries.getValue(1L).isOutdated)
    }

    // ---- detail

    private val people = listOf(Person(id = 10, name = "Me", isSelf = true), Person(id = 11, name = "Ana"))

    private fun detail(receipts: List<Receipt>, splits: List<ReceiptSplit>, id: Long) = runBlocking {
        ObserveReceiptDetailUseCase(FakeReceipts(receipts), FakeSplits(splits), FakePersons(people))(id).first()
    }

    @Test
    fun detailHasTheSharesWithNamesSortedByName() {
        val result = detail(listOf(pizza), pizzaSplit, id = 1)!!
        assertEquals(pizza, result.receipt)
        assertEquals(listOf("Ana", "Me"), result.shares.map { it.personName })
        assertFalse(result.isSplitOutdated)
    }

    @Test
    fun receiptWithoutSplitHasNoShares() {
        val result = detail(listOf(coffee), pizzaSplit, id = 2)!!
        assertTrue(result.shares.isEmpty())
        assertFalse(result.isSplitOutdated)
    }

    @Test
    fun missingReceiptGivesNull() {
        assertNull(detail(listOf(coffee), emptyList(), id = 99))
    }

    @Test
    fun detailMarksAnOutdatedSplit() {
        assertTrue(detail(listOf(pizza.copy(tipAmount = 200)), pizzaSplit, id = 1)!!.isSplitOutdated)
    }
}