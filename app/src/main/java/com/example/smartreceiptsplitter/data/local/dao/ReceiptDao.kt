package com.example.smartreceiptsplitter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.smartreceiptsplitter.data.local.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY date DESC")
    fun getAll(): Flow<List<ReceiptEntity>>
    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): ReceiptEntity
    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long
    @Update
    suspend fun update(receipt: ReceiptEntity)
    @Delete
    suspend fun delete(receipt: ReceiptEntity)
}