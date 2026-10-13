package com.example.smartreceipetsplitter.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.smartreceipetsplitter.data.local.enums.Statuses

@Entity(tableName = "receipt_splits",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ])
data class ReceiptSplitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val personId: Long,
    val groupId: Long?,
    val amountOwed: Long,
    val taxShare: Long?,
    val tipShare: Long,
    val status: Statuses,
    val paidAt: Long
    )