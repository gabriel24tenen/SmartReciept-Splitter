package com.example.smartreceiptsplitter.domain.model

data class ParsedReceiptItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Long?,
    val totalPrice: Long
)