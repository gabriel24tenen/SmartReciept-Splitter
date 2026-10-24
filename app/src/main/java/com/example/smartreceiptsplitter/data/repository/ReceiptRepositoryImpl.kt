package com.example.smartreceiptsplitter.data.repository

import com.example.smartreceiptsplitter.data.local.dao.ReceiptDao
import com.example.smartreceiptsplitter.data.local.entity.ReceiptEntity
import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao : ReceiptDao
) : ReceiptRepository {
    override fun getAllReceipts(): Flow<List<Receipt>> =
        receiptDao.getAll().map { list -> list.map {it.toDomain()} }

    override suspend fun getReceiptById(id: Long): Receipt? =
        receiptDao.getById(id)?.toDomain()

    override suspend fun addReceipt(receipt: Receipt): Long =
        receiptDao.insert(receipt.toEntity())

    override suspend fun updateReceipt(receipt: Receipt) =
        receiptDao.update(receipt.toEntity())

    override suspend fun deleteReceipt(receipt: Receipt) =
        receiptDao.delete(receipt.toEntity())
    private fun ReceiptEntity.toDomain() = Receipt(
        id = id,
        merchantName = merchantName,
        date = date,
        totalAmount = totalAmount,
        taxAmount = taxAmount,
        tipAmount = tipAmount,
        currency = currency,
        homeCurrencyAmount = homeCurrencyAmount,
        exchangeRateUsed = exchangeRateUsed,
        categoryId = categoryId,
        imagePath = imagePath,
        isManualEntry = isManualEntry,
        paidByPersonId = paidByPersonId,
        createdAt = createdAt
    )

    private fun Receipt.toEntity() = ReceiptEntity(
        id = id,
        merchantName = merchantName,
        date = date,
        totalAmount = totalAmount,
        taxAmount = taxAmount,
        tipAmount = tipAmount,
        currency = currency,
        homeCurrencyAmount = homeCurrencyAmount,
        exchangeRateUsed = exchangeRateUsed,
        categoryId = categoryId,
        imagePath = imagePath,
        isManualEntry = isManualEntry,
        paidByPersonId = paidByPersonId,
        createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis()
    )

    override fun getReceipts(
        dateFrom: Long?,
        dateToExclusive: Long?,
        currency: String?
    ): Flow<List<Receipt>> =
        receiptDao.getFiltered(dateFrom, dateToExclusive, currency).map { list -> list.map { it.toDomain() } }

    override fun getCurrencies(): Flow<List<String>> =
        receiptDao.getCurrencies()
}