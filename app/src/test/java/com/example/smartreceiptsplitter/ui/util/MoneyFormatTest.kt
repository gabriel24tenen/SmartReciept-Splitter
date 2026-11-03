package com.example.smartreceiptsplitter.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MoneyFormatTest {

    private val croatian = Locale.forLanguageTag("hr-HR")
    private val english = Locale.US

    @Test
    fun displayedAmountFollowsTheLocale() {
        assertEquals("12,50 EUR", formatMoney(1250, "EUR", croatian))
        assertEquals("12.50 EUR", formatMoney(1250, "EUR", english))
        assertEquals("1.234,56 EUR", formatMoney(123456, "EUR", croatian))
        assertEquals("1,234.56 EUR", formatMoney(123456, "EUR", english))
    }

    @Test
    fun inputAmountUsesTheLocaleSeparatorWithoutGrouping() {
        assertEquals("12,50", formatMoneyInput(1250, croatian))
        assertEquals("12.50", formatMoneyInput(1250, english))
        assertEquals("1234,56", formatMoneyInput(123456, croatian))
        assertEquals("0,05", formatMoneyInput(5, croatian))
    }

    @Test
    fun quantityHasNoTrailingZeros() {
        assertEquals("2", formatQuantity(2.0, croatian))
        assertEquals("1,25", formatQuantity(1.25, croatian))
        assertEquals("1.25", formatQuantity(1.25, english))
        assertEquals("0,85", formatQuantity(0.850, croatian))
    }

    @Test
    fun inputInEitherLocaleIsParsedBack() {
        assertEquals(1250L, parseMoney(formatMoneyInput(1250, croatian)))
        assertEquals(1250L, parseMoney(formatMoneyInput(1250, english)))
    }
}