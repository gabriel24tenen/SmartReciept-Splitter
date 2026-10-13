package com.example.smartreceiptsplitter.domain.repository

import com.example.smartreceiptsplitter.domain.model.Receipt
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    fun getAllReceipts(): Flow<List<Receipt>>
    suspend fun getReceiptById(id: Long): Receipt?
    suspend fun addReceipt(receipt: Receipt): Long
    suspend fun updateReceipt(receipt: Receipt)
    suspend fun deleteReceipt(receipt: Receipt)
}