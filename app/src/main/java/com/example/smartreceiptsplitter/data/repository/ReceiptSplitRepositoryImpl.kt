package com.example.smartreceiptsplitter.data.repository

import androidx.room.withTransaction
import com.example.smartreceiptsplitter.data.local.dao.ReceiptSplitDao
import com.example.smartreceiptsplitter.data.local.database.AppDatabase
import com.example.smartreceiptsplitter.data.local.entity.ReceiptSplitEntity
import com.example.smartreceiptsplitter.domain.model.ReceiptSplit
import com.example.smartreceiptsplitter.domain.repository.ReceiptSplitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReceiptSplitRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val splitDao: ReceiptSplitDao
): ReceiptSplitRepository {
    override fun observeSplits(receiptId: Long): Flow<List<ReceiptSplit>> =
        splitDao.getForReceipt(receiptId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveSplits(receiptId: Long, splits: List<ReceiptSplit>) {
        require(splits.map { it.personId }.distinct().size == splits.size) {
            "A person can only appear once in a receipt split."
        }
        database.withTransaction {
            val previous = splitDao.getForReceiptOnce(receiptId).associateBy { it.personId }
            splitDao.deleteForReceipt(receiptId)
            splitDao.insertAll(splits.map { split ->
                val old = previous[split.personId]
                split.copy(
                    id = 0,
                    receiptId = receiptId,
                    status = old?.status ?: split.status,
                    paidAt = old?.paidAt ?: split.paidAt
                ).toEntity()
            })
        }
    }

    private fun ReceiptSplitEntity.toDomain() = ReceiptSplit(
        id = id,
        receiptId = receiptId,
        personId = personId,
        groupId = groupId,
        amountOwed = amountOwed,
        taxShare = taxShare,
        tipShare = tipShare,
        status = status,
        paidAt = paidAt
    )

    private fun ReceiptSplit.toEntity() = ReceiptSplitEntity(
        id = id,
        receiptId = receiptId,
        personId = personId,
        groupId = groupId,
        amountOwed = amountOwed,
        taxShare = taxShare,
        tipShare = tipShare,
        status = status,
        paidAt = paidAt
    )
}