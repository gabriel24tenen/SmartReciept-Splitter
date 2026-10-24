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
    suspend fun getById(id: Long): ReceiptEntity?
    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long
    @Update
    suspend fun update(receipt: ReceiptEntity)
    @Delete
    suspend fun delete(receipt: ReceiptEntity)
    @Query("SELECT DISTINCT currency FROM receipts ORDER BY currency")
    fun getCurrencies(): Flow<List<String>>
    @Query("""
        SELECT * FROM receipts
        WHERE (:dateFrom IS NULL OR date >= :dateFrom)
            AND (:dateToExclusive IS NULL OR date < :dateToExclusive)
            AND (:currency IS NULL OR currency = :currency)
        ORDER BY date DESC, createdAt DESC, id DESC
    """)
    fun getFiltered(dateFrom: Long?, dateToExclusive: Long?, currency: String?): Flow<List<ReceiptEntity>>
}