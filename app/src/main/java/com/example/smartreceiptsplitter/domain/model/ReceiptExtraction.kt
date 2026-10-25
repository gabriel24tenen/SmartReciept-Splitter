package com.example.smartreceiptsplitter.domain.model

import java.time.LocalDate
import java.time.ZoneId

data class ReceiptExtraction(
    val totalAmount: Long?,
    val date: LocalDate?,
    val taxAmount: Long?,
    val items: List<ParsedReceiptItem>
) {
    val itemsTotal: Long get() = items.sumOf { it.totalPrice }
    val itemsMatchTotal: Boolean?
        get() = if(items.isEmpty() || totalAmount == null) null else itemsTotal == totalAmount

    fun toDraft(zone: ZoneId, currency: String) = ReceiptDraft(
        date = date?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(),
        totalAmount = totalAmount,
        taxAmount = taxAmount,
        currency = currency
    )

    companion object {
        val EMPTY = ReceiptExtraction(totalAmount = null, date = null, taxAmount = null, items = emptyList())
    }
}