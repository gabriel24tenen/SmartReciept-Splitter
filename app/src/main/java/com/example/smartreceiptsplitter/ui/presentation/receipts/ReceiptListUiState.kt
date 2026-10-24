package com.example.smartreceiptsplitter.ui.presentation.receipts

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.ReceiptFilter
import com.example.smartreceiptsplitter.ui.util.parseDate
import java.time.Instant
import java.time.ZoneId

data class ReceiptListUiState(
    val receipts: List<Receipt> = emptyList(),
    val isLoading: Boolean = true,
    //filter section
    val query: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val currency: String? = null,
    val currencies: List<String> = emptyList(),
    val dateFromInvalid: Boolean = false,
    val dateToInvalid: Boolean = false,
    val dateRangeReversed: Boolean = false,
    val pendingDelete: Receipt? = null,
    val errorMessage: String? = null
    ) {
    val activeFilterCount: Int
        get() = listOf(dateFrom.isNotBlank(), dateTo.isNotBlank(), currency != null).count { it }
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || activeFilterCount > 0
}
    internal fun ReceiptListUiState.toFilter(zone: ZoneId) = ReceiptFilter(
        merchantQuery = query,
        dateFrom = parseDate(dateFrom, zone),
        dateToExclusive = parseDate(dateTo, zone)?.let { startOfNextDay(it, zone) },
        currency = currency
    )

    internal fun ReceiptListUiState.withDateErrors(zone: ZoneId): ReceiptListUiState {
        val from = parseDate(dateFrom, zone)
        val to = parseDate(dateTo, zone)
        return copy(
            dateFromInvalid = dateFrom.isNotBlank() && from == null,
            dateToInvalid = dateTo.isNotBlank() && from == null,
            dateRangeReversed = from != null && to != null && to < from
        )
    }
    private fun startOfNextDay(startOfDay: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(startOfDay).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
