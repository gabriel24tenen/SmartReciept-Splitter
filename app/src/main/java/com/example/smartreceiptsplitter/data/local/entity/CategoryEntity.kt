package com.example.smartreceiptsplitter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.smartreceiptsplitter.data.local.enums.Categories

@Entity( tableName = "categories")
data class CategoryEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: Categories,
    val iconRes: String?,
    val colorHex: String?,
    val isDefault: Boolean
)

// an expense category made for user-defined categories. Default categoires are seeded from the Categories enumeration