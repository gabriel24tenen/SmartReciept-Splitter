package com.example.smartreceiptsplitter.domain.model

data class Receipt(
    val id: Long = 0,
    val merchantName: String?,
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
    val paidByPersonId: Long?,
    val createdAt: Long = 0
)
