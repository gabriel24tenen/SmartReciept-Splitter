package com.example.smartreceipetsplitter.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ])
data class ReceiptItemEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Long?,
    val totalPrice: Long?,
    val vatRate: Double?
)