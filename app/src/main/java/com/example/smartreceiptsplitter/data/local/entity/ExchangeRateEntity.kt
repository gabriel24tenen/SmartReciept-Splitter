package com.example.smartreceiptsplitter.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exchange_rates",
    indices = [Index(value = ["fromCurrency", "toCurrency"], unique = true)]
)
data class ExchangeRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val lastUpdated: Long
)