package com.example.smartreceiptsplitter.domain.usecase.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptAmountsTest {

    private fun cents(text: String) = findAmounts(text).map { it.cents }

    @Test
    fun readsCommaAndDotDecimals() {
        assertEquals(listOf(1250L), cents("12,50"))
        assertEquals(listOf(999L), cents("9.99"))
    }

    @Test
    fun readsThousandsSeparators() {
        assertEquals(listOf(123456L), cents("1.234,56"))
        assertEquals(listOf(123456L), cents("1,234.56"))
        assertEquals(listOf(123456L), cents("1234,56"))
        assertEquals(listOf(100000000L), cents("1.000.000,00"))
    }

    @Test
    fun readsAmountsNextToCurrency() {
        assertEquals(listOf(1250L), cents("12,50€"))
        assertEquals(listOf(1250L), cents("€12,50"))
        assertEquals(listOf(1250L), cents("EUR 12,50"))
    }

    @Test
    fun findsAllAmountsInRowLeftToRight() {
        assertEquals(listOf(200L, 150L, 300L), cents("2 x 2,00 1,50 3,00"))
    }

    @Test
    fun ignoresNumbersThatAreNotMoney() {
        assertTrue(cents("25.10.2026").isEmpty()) // date
        assertTrue(cents("12:30").isEmpty()) // time
        assertTrue(cents("PDV 25,00%").isEmpty()) // percentage
        assertTrue(cents("3,000 kg").isEmpty()) // three decimals
        assertTrue(cents("OIB 12345678901").isEmpty()) // whole number
        assertTrue(cents("Racun br. 123/1/1").isEmpty())
    }

    @Test
    fun marksNegativeAmounts() {
        assertTrue(findAmounts("POPUST -1,50").single().isNegative)
        assertTrue(findAmounts("POPUST 1,50-").single().isNegative)
        assertTrue(!findAmounts("UKUPNO 1,50").single().isNegative)
    }

    @Test
    fun fixesLetterOReadInsteadOfZero() {
        assertEquals(listOf(1250L), cents("12,5O"))
        assertEquals(listOf(10000L), cents("1OO,OO"))
    }
}