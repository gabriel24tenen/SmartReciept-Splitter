package com.example.smartreceiptsplitter.domain.repository

import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import kotlinx.coroutines.flow.Flow

interface ReceiptSplitRepository {
    fun observeSplits(receiptId: Long): Flow<List<ReceiptSplit>>
    fun observeAllSplits(): Flow<List<ReceiptSplit>>
    suspend fun saveSplits(receiptId: Long, splits: List<ReceiptSplit>, receiptTipAmount: Long?)
}