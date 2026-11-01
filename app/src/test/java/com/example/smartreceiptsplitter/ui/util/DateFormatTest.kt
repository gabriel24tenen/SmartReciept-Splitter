package com.example.smartreceiptsplitter.ui.util

import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateFormatTest {
    private val zone = ZoneId.of("Europe/Zagreb")
    private val expected = LocalDate.of(2026, 10, 20).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun parseDateAcceptsDayFirstFormats() {
        for (text in listOf("20.10.2026", "20.10.2026.", "20. 10. 2026.", "20/10/2026", "2026-10-20", " 20.10.2026 ")) {
            Assert.assertEquals(text, expected, parseDate(text, zone))
        }
    }

    @Test
    fun parseDateAcceptsSingleDigitDayAndMonth() {
        Assert.assertEquals(
            LocalDate.of(2026, 2, 1).atStartOfDay(zone).toInstant().toEpochMilli(),
            parseDate("1.2.2026", zone)
        )
    }

    @Test
    fun parseDateRejectsBlankAndInvalidText() {
        for (text in listOf("", "   ", "abc", "31.02.2026", "29.02.2026", "20.13.2026", "0.10.2026", "20.10.26", "20.10.202", "2026.10.20")) {
            Assert.assertNull(text, parseDate(text, zone))
        }
    }

    @Test
    fun parseDateAcceptsLeapDay() {
        Assert.assertEquals(
            LocalDate.of(2028, 2, 29).atStartOfDay(zone).toInstant().toEpochMilli(),
            parseDate("29.02.2028", zone)
        )
    }

    @Test
    fun parsedDateIsStartOfDayInTheGivenZone() {
        val newYork = ZoneId.of("America/New_York")
        Assert.assertEquals(
            LocalDate.of(2026, 10, 20).atStartOfDay(newYork).toInstant().toEpochMilli(),
            parseDate("20.10.2026", newYork)
        )
    }

    @Test
    fun formatDateInputRoundTripsWithParseDate() {
        Assert.assertEquals("20.10.2026", formatDateInput(expected, zone))
        Assert.assertEquals(expected, parseDate(formatDateInput(expected, zone), zone))
        Assert.assertEquals("01.02.2026", formatDateInput(parseDate("1.2.2026", zone)!!, zone))
    }
}