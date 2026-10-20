package com.example.smartreceiptsplitter.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidByPersonId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["paidByPersonId"]) ])
data class ReceiptEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantName: String,
    val date: Long,
    val totalAmount: Long,
    val taxAmount: Long?,
    val tipAmount: Long?,
    val currency: String,
    val homeCurrencyAmount: Long?,
    val exchangeRateUsed: Double?,
    val categoryId: Long?,
    val imagePath: String?,
    val isManualEntry: Boolean,
    val createdAt: Long,
    val paidByPersonId: Long?
)