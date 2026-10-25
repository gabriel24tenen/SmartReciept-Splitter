package com.example.smartreceiptsplitter.domain.usecase.ocr


import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ParseReceiptDateUseCaseTest {
    private val parseReceiptDate = ParseReceiptDateUseCase()
    private val clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneId.of("Europe/Zagreb"))
    private val oct25 = LocalDate.of(2026, 10, 25)

    private fun parse(vararg rows: String) = parseReceiptDate(receiptText(*rows), clock)

    @Test
    fun readsDateFromCroatianFiscalReceipt() {
        val date = parse(
            "KONZUM PLUS d.o.o.",
            "UKUPNO EUR | 2,48",
            "PDV 25% | 1,98 | 0,50",
            "Datum i vrijeme: 25.10.2026 14:32:05",
            "JIR: 1a2b3c4d-5e6f"
        )
        assertEquals(oct25, date)
    }

    @Test
    fun readsDayFirstNumericFormats() {
        assertEquals(oct25, parse("25.10.2026"))
        assertEquals(oct25, parse("25.10.2026."))
        assertEquals(oct25, parse("25. 10. 2026."))
        assertEquals(oct25, parse("25.10.26"))
        assertEquals(oct25, parse("25-10-2026"))
        assertEquals(oct25, parse("25/10/2026"))
        assertEquals(LocalDate.of(2026, 3, 5), parse("5.3.2026"))
    }

    @Test
    fun readsYearFirstFormat() {
        assertEquals(oct25, parse("2026-10-25 09:12"))
        assertEquals(oct25, parse("2026.10.25"))
    }

    @Test
    fun readsMonthNames() {
        assertEquals(oct25, parse("25. listopada 2026."))
        assertEquals(LocalDate.of(2026, 3, 1), parse("1. ožujka 2026"))
        assertEquals(oct25, parse("25 Oct 2026"))
        assertEquals(oct25, parse("Oct 25, 2026"))
        assertEquals(LocalDate.of(2026, 9, 3), parse("Sept. 3, 2026"))
    }

    @Test
    fun slashDateIsDayFirstUnlessReceiptIsInDollars() {
        assertEquals(LocalDate.of(2026, 10, 5), parse("05/10/2026", "UKUPNO | 3,00"))
        assertEquals(LocalDate.of(2026, 5, 10), parse("05/10/2026", "TOTAL | $3.00"))
        // 25 can't be a month, so the order is clear even in dollars
        assertEquals(oct25, parse("25/10/2026", "TOTAL | $3.00"))
    }

    @Test
    fun skipsDueDateAndBestBeforeRows() {
        assertEquals(
            LocalDate.of(2026, 10, 20),
            parse("Datum izdavanja: 20.10.2026", "Datum dospijeća: 22.10.2026")
        )
        assertEquals(LocalDate.of(2026, 10, 20), parse("Valuta: 24.10.2026", "20.10.2026 11:05"))
    }

    @Test
    fun labelledOrTimedDateWinsOverOtherDates() {
        assertEquals(LocalDate.of(2026, 10, 20), parse("Član od 01.01.2020", "20.10.2026 10:15"))
        assertEquals(LocalDate.of(2026, 10, 20), parse("Akcija 01.10.2026", "Datum: 20.10.2026"))
    }

    @Test
    fun returnsNullWhenEquallyLikelyDatesDisagree() {
        assertNull(parse("01.10.2026", "20.10.2026"))
    }

    @Test
    fun sameDatePrintedTwiceIsFine() {
        assertEquals(oct25, parse("25.10.2026", "Datum: 25.10.2026", "Vrijeme: 25.10.2026 14:00"))
    }

    @Test
    fun rejectsImpossibleFutureAndAncientDates() {
        assertNull(parse("31.02.2026"))
        assertNull(parse("26.10.2026")) // tomorrow: a misread, not the issue date
        assertNull(parse("12.05.1998"))
    }

    @Test
    fun doesNotReadAmountsOrOtherNumbersAsDates() {
        assertNull(parse("UKUPNO | 12.50"))
        assertNull(parse("Kolicina 1.234.567"))
        assertNull(parse("Racun br. 12/1/1"))
        assertNull(parse("Tel. 01-234-5678"))
    }

    @Test
    fun toleratesLetterOReadInsteadOfZero() {
        assertEquals(oct25, parse("25.1O.2O26"))
    }

    @Test
    fun findDatesListsAllDatesLeftToRight() {
        val dates = findDates("od 01.10.2026 do 2026-10-25")
        assertEquals(listOf(LocalDate.of(2026, 10, 1), oct25), dates)
        assertTrue(findDates("12:30 2,50 EUR").isEmpty())
    }
}