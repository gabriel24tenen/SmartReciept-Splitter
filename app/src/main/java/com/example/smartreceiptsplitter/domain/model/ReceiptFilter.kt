package com.example.smartreceiptsplitter.domain.model

data class ReceiptFilter(
    val merchantQuery: String = "",
    val dateFrom: Long? = null,
    val dateToExclusive: Long? = null,
    val currency: String? = null
)