package com.example.smartreceiptsplitter.domain.usecase.split

import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveReceiptSplitsUseCaseTest {

    private class FakeSplitRepository : ReceiptSplitRepository {
        var savedSplits: List<ReceiptSplit>? = null
        var savedTip: Long? = -1
        override fun observeSplits(receiptId: Long): Flow<List<ReceiptSplit>> = flowOf(emptyList())
        override suspend fun saveSplits(receiptId: Long, splits: List<ReceiptSplit>, receiptTipAmount: Long?) {
            savedSplits = splits
            savedTip = receiptTipAmount
        }
    }

    private fun save(vararg splits: ReceiptSplit): FakeSplitRepository {
        val repository = FakeSplitRepository()
        runBlocking { SaveReceiptSplitsUseCase(repository)(receiptId = 7, splits = splits.toList()) }
        return repository
    }

    @Test
    fun receiptTipIsTheSumOfTheTipShares() {
        val repository = save(
            ReceiptSplit(personId = 1, amountOwed = 1150, tipShare = 150),
            ReceiptSplit(personId = 2, amountOwed = 1150, tipShare = 150)
        )
        assertEquals(300L, repository.savedTip)
        assertEquals(2, repository.savedSplits?.size)
    }

    @Test
    fun receiptTipIsNullWithoutATip() {
        assertNull(save(ReceiptSplit(personId = 1, amountOwed = 1000)).savedTip)
        assertNull(save(ReceiptSplit(personId = 1, amountOwed = 1000, tipShare = 0)).savedTip)
    }
}