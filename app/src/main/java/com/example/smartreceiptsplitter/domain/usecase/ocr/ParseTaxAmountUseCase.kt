package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.RecognizedText
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToLong

class ParseTaxAmountUseCase @Inject constructor() {

    operator fun invoke(text: RecognizedText): Long? {
        val summaries = mutableListOf<Long>()
        val perRate = linkedSetOf<Pair<Double, Long>>() // (rate, tax); a set, so a breakdown printed twice isn't counted twice
        val withoutRate = linkedSetOf<Long>() // "PDV: 0,50", "PNP 0,30": tax rows that don't print a rate
        var inTaxTable = false

        for (row in text.toRows()) {
            val words = row.words()
            val isTaxRow = words.any { it in TAX_WORDS } || TAX_PARTS.any { it in row.keywordText }
            val rate = RATE.find(row.text)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            val amounts = findAmounts(row.text).filter { !it.isNegative }.map { it.cents }

            when {
                // "Stopa | Osnovica | PDV": header of a tax table (checked first, the header itself may say "Osnovica")
                isTaxRow && amounts.isEmpty() -> inTaxTable = true

                isTaxRow && row.isGrossOrNet(words) -> inTaxTable = false

                isTaxRow || (inTaxTable && rate != null) -> {
                    if (rate == null && words.any { it in SUMMARY_WORDS } && amounts.size == 1) {
                        summaries += amounts.single() // "Ukupno PDV 3,30"
                    } else {
                        // A table row without a tax word must fit its rate: an item row like "Mlijeko 2,8% 1L 1,19"
                        // below a "Naziv | Cijena | PDV" header has a percentage too, but its amount is a price
                        taxFromRow(rate, amounts, requireRateMatch = !isTaxRow)?.let { tax ->
                            if (rate != null) perRate += rate to tax else withoutRate += tax
                        }
                    }
                }

                else -> inTaxTable = false
            }
        }
        return summaries.lastOrNull() ?: combine(perRate.map { it.second }, withoutRate)
    }

    private fun combine(rated: List<Long>, withoutRate: Set<Long>): Long? {
        if(rated.isEmpty() && withoutRate.isEmpty()) return null
        val ratedTotal = rated.sum()
        val extra = withoutRate.filter { it != ratedTotal && it !in rated }
        return ratedTotal + extra.sum()
    }

    //@param RequireRateMatch is true for rows without a tax word, a single amount can't be checked against the rate, so it isn't taken
    private fun taxFromRow(rate: Double?, amounts: List<Long>, requireRateMatch: Boolean): Long? {
        if (amounts.isEmpty()) return null
        if (amounts.size == 1) return if(requireRateMatch) null else amounts.single()
        if (rate == null) return null // "PDV 5,00 3,00": no way to tell which one is the tax
        if (rate == 0.0) return 0L
        // base x rate = tax ("25% 8,00 2,00") or gross x rate / (100 + rate) = tax ("19% 15,96 3,03 18,99")
        return amounts.firstOrNull { tax ->
            amounts.any { other ->
                other != tax && (fits(other * rate / 100, tax) || fits(other * rate / (100 + rate), tax))
            }
        }
    }
    private fun fits(expected: Double, actual: Long) = abs(expected.roundToLong() - actual) <= ROUNDING_CENTS
    private fun ReceiptRow.words(): Set<String> = text.toUppercaseWithoutDiacritics().split(Regex("[^A-Z]+")).toSet()
    private fun ReceiptRow.isGrossOrNet(words: Set<String>): Boolean =
        words.any { it in NET_WORDS } ||
                (words.any { it in INCLUDING_WORDS || it.startsWith("UKLJUC") } && words.any { it in TOTAL_WORDS })

    private companion object {
        const val ROUNDING_CENTS = 2L

        val TAX_WORDS = setOf("PDV", "PNP", "VAT", "TAX", "MWST", "UST", "IVA", "TVA", "BTW")
        val TAX_PARTS = listOf("POREZ", "MEHRWERTSTEUER", "UMSATZSTEUER", "SALESTAX")
        val SUMMARY_WORDS = setOf("UKUPNO", "UKUPAN", "UKUPNI", "TOTAL", "SUMME", "GESAMT", "SUM")
        val NET_WORDS = setOf("BEZ", "NETO", "NETTO", "NET", "OSNOVICA", "EXCL", "EXKL", "BASE")
        val INCLUDING_WORDS = setOf("S", "SA", "INCL", "INKL", "INCLUDING", "INCLUDED")
        val TOTAL_WORDS = setOf("UKUPNO", "UKUPAN", "IZNOS", "TOTAL", "GESAMT", "SUMME", "BETRAG", "AMOUNT")

        // "25%", "25,00 %", "8.875%"
        val RATE = Regex("""(?<![\d.,])(\d{1,2}(?:[.,]\d{1,3})?)\s?%""")
    }
}