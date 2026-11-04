package com.example.smartreceiptsplitter.domain.repository

import com.example.smartreceiptsplitter.domain.model.Receipt
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    suspend fun getReceiptById(id: Long): Receipt?
    fun observeReceipt(id: Long): Flow<Receipt?> // null once the receipt is deleted
    suspend fun addReceipt(receipt: Receipt): Long
    suspend fun updateReceipt(receipt: Receipt)
    suspend fun deleteReceipt(receipt: Receipt)
    fun getReceipts(dateFrom: Long?, dateToExclusive: Long?, currency: String?): Flow<List<Receipt>>
    fun getCurrencies(): Flow<List<String>>
}