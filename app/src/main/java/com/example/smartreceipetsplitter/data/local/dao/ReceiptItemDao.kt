package com.example.smartreceipetsplitter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.smartreceipetsplitter.data.local.entity.ReceiptItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptItemDao {
    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId")
    fun getForReceipt(receiptId: Long): Flow<List<ReceiptItemEntity>>
    @Insert
    suspend fun insertAll(items: List<ReceiptItemEntity>): List<Long>
    @Update
    suspend fun update(item: ReceiptItemEntity)
    @Delete
    suspend fun delete(item: ReceiptItemEntity)
}