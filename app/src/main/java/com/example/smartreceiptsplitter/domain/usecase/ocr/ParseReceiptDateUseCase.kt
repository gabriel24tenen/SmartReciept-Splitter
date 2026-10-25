package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.RecognizedText
import java.time.Clock
import java.time.DateTimeException
import java.time.LocalDate
import javax.inject.Inject
import kotlin.collections.emptyList

/*
* Rules:
* 1. Only real calendar dates between [MIN_YEAR] and today count (a future date is a misread, not a receipt).
* 2. Rows about other dates (due date, "valuta", valid until, best before) are skipped.
* 3. A row with a date keyword ("Datum", "Date") scores higher, a time next to the date ("14:32") too:
*    fiscal receipts print the issue date together with the time.
* 4. If the best-scoring rows hold different dates, the result is null - the user picks the date.
* 5. With "/" and both numbers up to 12 (05/10/2026) the day comes first, unless the receipt is in dollars,
*    where the US month-first order is used.
*/

class ParseReceiptDateUseCase @Inject constructor() {

    operator fun invoke(text: RecognizedText, clock: Clock = Clock.systemDefaultZone()): LocalDate? {
        val today = LocalDate.now(clock)
        val monthFirstForSlash = '$' in text.fullText || "USD" in text.fullText.uppercase()

        val candidates = text.toRows().flatMapIndexed { index, row ->
            if (EXCLUDED.any { it in row.keywordText }) return@flatMapIndexed emptyList()
            val score = (if (KEYWORDS.any { it in row.keywordText }) 2 else 0) + (if (TIME_REGEX.containsMatchIn(row.text)) 1 else 0)
            findDates(row.text, monthFirstForSlash)
                .filter { it.year >= MIN_YEAR && !it.isAfter(today) }
                .map { Candidate(it, score, index) }
        }
        val bestScore = candidates.maxOfOrNull { it.score } ?: return null
        val best = candidates.filter { it.score == bestScore }.map { it.date }.distinct()
        return best.singleOrNull()
    }

    private data class Candidate(val date: LocalDate, val score: Int, val rowIndex: Int)

    private companion object {
        const val MIN_YEAR = 2000

        // Compared against ReceiptRow.keywordText (letters only, no spaces, no diacritics)
        val KEYWORDS = listOf("DATUM", "DATE", "DAT", "IZDAN", "VRIJEME", "TIME")
        val EXCLUDED = listOf(
            "DOSPIJE", "DOSPIJEC", "VALUTA", "ROKPLAC", "VRIJEDIDO", "VALIDUNTIL", "VALIDTHRU", "EXPIR", "ISTEK",
            "DUEDATE", "UPOTRIJEBITIDO", "UPOTREBITIDO", "BESTBEFORE"
        )
        val TIME_REGEX = Regex("""(?<!\d)([01]?\d|2[0-3]):[0-5]\d(?!\d)""")
    }
}

internal fun findDates(text: String, monthFirstForSlash: Boolean = false): List<LocalDate> {
    val cleaned = text.fixLetterOInNumbers().toUppercaseWithoutDiacritics()
    val found = mutableListOf<Pair<Int, LocalDate>>()

    DAY_FIRST.findAll(cleaned).forEach { match ->
        val (first, separator, second, year) = match.destructured
        val a = first.toInt()
        val b = second.toInt()
        val monthFirst = separator == "/" && monthFirstForSlash && a <= 12
        val date = if (monthFirst) dateOrNull(year.fullYear(), a, b) else dateOrNull(year.fullYear(), b, a)
        date?.let { found += match.range.first to it}
    }
    YEAR_FIRST.findAll(cleaned).forEach { match ->
        val (year, _, month, day) = match.destructured
        dateOrNull(year.toInt(), month.toInt(), day.toInt())?.let { found += match.range.first to it }
    }
    DAY_MONTH_NAME.findAll(cleaned).forEach { match ->
        val (day, month, year) = match.destructured
        dateOrNull(year.toInt(), MONTHS.getValue(month), day.toInt())?.let { found += match.range.first to it }
    }
    MONTH_NAME_DAY.findAll(cleaned).forEach { match ->
        val (month, day, year) = match.destructured
        dateOrNull(year.toInt(), MONTHS.getValue(month), day.toInt())?.let { found += match.range.first to it }
    }
    return found.sortedBy { it.first }.map { it.second }
}

private fun String.fullYear(): Int = toInt().let { if (length == 2) 2000 + it else it }
private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? =
    try {
        LocalDate.of(year, month, day)
    } catch (e: DateTimeException) {
        null
    }
// Upper case, no diacritics; nominative and genitive for Croatian ("listopad", "listopada")
private val MONTHS: Map<String, Int> = buildMap {
    listOf(
        listOf("JAN", "JANUARY", "SIJECANJ", "SIJECNJA"),
        listOf("FEB", "FEBRUARY", "VELJACA", "VELJACE"),
        listOf("MAR", "MARCH", "OZUJAK", "OZUJKA"),
        listOf("APR", "APRIL", "TRAVANJ", "TRAVNJA"),
        listOf("MAY", "SVIBANJ", "SVIBNJA"),
        listOf("JUN", "JUNE", "LIPANJ", "LIPNJA"),
        listOf("JUL", "JULY", "SRPANJ", "SRPNJA"),
        listOf("AUG", "AUGUST", "KOLOVOZ", "KOLOVOZA"),
        listOf("SEP", "SEPT", "SEPTEMBER", "RUJAN", "RUJNA"),
        listOf("OCT", "OCTOBER", "LISTOPAD", "LISTOPADA"),
        listOf("NOV", "NOVEMBER", "STUDENI", "STUDENOG", "STUDENOGA"),
        listOf("DEC", "DECEMBER", "PROSINAC", "PROSINCA")
    ).forEachIndexed { index, names -> names.forEach { put(it, index + 1) } }
}

// Longest names first so "LISTOPADA" is not cut to "LISTOPAD"
private val MONTH_ALTERNATION = MONTHS.keys.sortedByDescending { it.length }.joinToString("|")

// 25.10.2026 / 25. 10. 2026. / 25-10-26 / 25/10/2026 - the same separator twice
private val DAY_FIRST = Regex("""(?<![\d.,/-])(\d{1,2})\s?([./-])\s?(\d{1,2})\s?\2\s?(\d{4}|\d{2})(?![\d,]|[./-]\d)""")

// 2026-10-25 / 2026.10.25
private val YEAR_FIRST = Regex("""(?<![\d.,/-])(\d{4})([./-])(\d{1,2})\2(\d{1,2})(?![\d,]|[./-]\d)""")

// 25 OCT 2026 / 25. LISTOPADA 2026.
private val DAY_MONTH_NAME = Regex("""(?<!\d)(\d{1,2})\.?\s+($MONTH_ALTERNATION)\.?,?\s+(\d{4})(?!\d)""")

// OCT 25, 2026
private val MONTH_NAME_DAY = Regex("""(?<![A-Z])($MONTH_ALTERNATION)\.?\s+(\d{1,2}),?\s+(\d{4})(?!\d)""")