package com.example.smartreceiptsplitter.ui.presentation.receipts

import com.example.smartreceiptsplitter.domain.model.ReceiptFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReceiptListUiStateTest {

    private val zone = ZoneId.of("Europe/Zagreb")
    private fun startOfDay(year: Int, month: Int, day: Int): Long =
            LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun defaultStateIsAnEmptyFilter() {
        assertEquals(ReceiptFilter(), ReceiptListUiState().toFilter(zone))
        assertFalse(ReceiptListUiState().hasActiveFilters)
    }

    @Test
    fun theLastDayOfTheRangeIsIncluded() {
        val filter = ReceiptListUiState(dateFrom = "01.10.2026", dateTo = "20.10.2026").toFilter(zone)
        assertEquals(startOfDay(2026, 10, 1), filter.dateFrom)
        // exclusive end = start of 21 October, so a receipt dated 20.10.2026 (stored as its start of day) is inside
        assertEquals(startOfDay(2026, 10, 21), filter.dateToExclusive)
        assertTrue(startOfDay(2026, 10, 20) < filter.dateToExclusive!!)
    }

    @Test
    fun endOfRangeIsRightWhenTheClocksChange() {
        // 25.10.2026 has 25 hours in Croatia (summer time ends), the next day must still start at its midnight
        val filter = ReceiptListUiState(dateTo = "25.10.2026").toFilter(zone)
        assertEquals(startOfDay(2026, 10, 26), filter.dateToExclusive)
    }

    @Test
    fun otherDateFormatsOfTheFormAreAccepted() {
        val filter = ReceiptListUiState(dateFrom = "2026-10-01", dateTo = "20/10/2026").toFilter(zone)
        assertEquals(startOfDay(2026, 10, 1), filter.dateFrom)
        assertEquals(startOfDay(2026, 10, 21), filter.dateToExclusive)
    }

    @Test
    fun unreadableDatesDoNotLimitTheListButAreReported() {
        val state = ReceiptListUiState(dateFrom = "31.02.2026", dateTo = "abc").withDateErrors(zone)
        val filter = state.toFilter(zone)
        assertNull(filter.dateFrom)
        assertNull(filter.dateToExclusive)
        assertTrue(state.dateFromInvalid)
        assertTrue(state.dateToInvalid)
    }

    @Test
    fun blankDatesAreNotAnError() {
        val state = ReceiptListUiState(dateFrom = "", dateTo = "  ").withDateErrors(zone)
        assertFalse(state.dateFromInvalid)
        assertFalse(state.dateToInvalid)
        assertFalse(state.dateRangeReversed)
    }

    @Test
    fun reversedRangeIsReported() {
        assertTrue(ReceiptListUiState(dateFrom = "20.10.2026", dateTo = "01.10.2026").withDateErrors(zone).dateRangeReversed)
        assertFalse(ReceiptListUiState(dateFrom = "20.10.2026", dateTo = "20.10.2026").withDateErrors(zone).dateRangeReversed)
    }

    @Test
    fun queryAndCurrencyGoToTheFilter() {
        val filter = ReceiptListUiState(query = "konzum", currency = "USD").toFilter(zone)
        assertEquals("konzum", filter.merchantQuery)
        assertEquals("USD", filter.currency)
    }

    @Test
    fun activeFiltersAreCountedWithoutTheSearchText() {
        assertEquals(0, ReceiptListUiState(query = "konzum").activeFilterCount)
        assertTrue(ReceiptListUiState(query = "konzum").hasActiveFilters)
        assertEquals(3, ReceiptListUiState(dateFrom = "01.10.2026", dateTo = "20.10.2026", currency = "EUR").activeFilterCount)
    }
}