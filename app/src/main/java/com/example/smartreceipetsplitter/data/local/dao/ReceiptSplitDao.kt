package com.example.smartreceipetsplitter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.smartreceipetsplitter.data.local.entity.ReceiptSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptSplitDao {
    @Query("SELECT * FROM receipt_splits WHERE receiptId = :receiptId")
    fun getForReceipt(receiptId: Long): Flow<List<ReceiptSplitEntity>>
    @Query("SELECT * FROM receipt_splits WHERE personId = :personId")
    fun getForPerson(personId: Long): Flow<List<ReceiptSplitEntity>>
    @Insert
    suspend fun insertAll(splits: List<ReceiptSplitEntity>): List<Long>
    @Update
    suspend fun update(split: ReceiptSplitEntity)
    @Delete
    suspend fun delete(split: ReceiptSplitEntity)
    @Query("DELETE FROM receipt_splits WHERE receiptId = :receiptId")
    suspend fun deleteForReceipt(receiptId: Long)
}