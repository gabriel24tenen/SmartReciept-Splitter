package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.ParsedReceiptItem
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToLong

/* * Supported layouts:
 *   Kruh bijeli          1,29     name and line total
 *   Jogurt 2 x 0,89      1,78     quantity x unit price, line total
 *   Kava   2   1,50      3,00     table: quantity, unit price, line total
 *   2 Pivo               6,00     quantity before the name (restaurants)
 *   Jabuke                        name on its own row...
 *   1,250 kg x 1,99      2,49     ...quantity row below it
 *   Kruh                 2,58     name and line total...
 *   2 x 1,29                      ...quantity row below it
 *
 * Quantity and unit price are kept only when quantity x unit price matches the line total (1 cent rounding allowed);
 * otherwise only the line total is kept, quantity 1 and unit price null.
 */

class ParseLineItemsUseCase @Inject constructor() {

    operator fun invoke(text: RecognizedText): List<ParsedReceiptItem> {
        val rows = text.toRows()
        val end = rows.indexOfFirst { row -> ITEMS_END.any { it in row.keywordText } }.takeIf { it >= 0 } ?: rows.size

        val items = mutableListOf<ParsedReceiptItem>()
        var pendingName: String? = null // a name row without an amount, waiting for its quantity row
        var lastRowWasItem = false

        for (row in rows.subList(0, end)) {
            if (row.isNeverAnItem()) {
                pendingName = null
                lastRowWasItem = false
                continue
            }
            val parsed = parseRow(row.text)
            when {
                parsed == null && row.hasName() -> {
                    pendingName = row.text.cleanName()
                    lastRowWasItem = false
                }
                parsed == null -> {
                    pendingName = null
                    lastRowWasItem = false
                }
                parsed.name.hasLetters() -> {
                    items += parsed.toItem(parsed.name)
                    pendingName = null
                    lastRowWasItem = true
                }
                pendingName != null -> {
                    // "Jabuke" / "1,250 kg x 1,99 2,49"
                    items += parsed.toItem(pendingName)
                    pendingName = null
                    lastRowWasItem = true
                }
                lastRowWasItem && parsed.quantity != null && parsed.unitPrice != null -> {
                    // "Kruh 2,58" / "2 x 1,29": the quantity row explains the item above it
                    val previous = items.last()
                    val expected = (parsed.quantity * parsed.unitPrice).roundToLong()
                    if (abs(expected - previous.totalPrice) <= ROUNDING_CENTS && parsed.total in setOf(null, previous.totalPrice)) {
                        items[items.lastIndex] = previous.copy(quantity = parsed.quantity, unitPrice = parsed.unitPrice)
                    }
                    lastRowWasItem = false
                }
                else -> lastRowWasItem = false // amounts without any name: nothing to attach them to
            }
        }
        return items
    }

    private data class RowParse(val name: String, val quantity: Double?, val unitPrice: Long?, val total: Long?) {
        fun toItem(itemName: String): ParsedReceiptItem {
            val lineTotal = total ?: (quantity!! * unitPrice!!).roundToLong()
            return ParsedReceiptItem(name = itemName, quantity = quantity ?: 1.0, unitPrice = unitPrice, totalPrice = lineTotal)
        }
    }

    private fun parseRow(rawText: String): RowParse? {
        val text = rawText.fixLetterOInNumbers()
        val amounts = findAmounts(text)
        if (amounts.none { !it.isNegative }) return null
        val positive = amounts.filter { !it.isNegative && it.cents > 0 }
        if (positive.isEmpty()) return null

        // 1) "2 x 1,29 2,58" / "1,250 kg x 1,99"
        TIMES.find(text)?.let { times ->
            val quantity = times.groupValues[1].toQuantity()
            val after = positive.filter { it.range.first > times.range.last }
            if (quantity != null && after.isNotEmpty()) {
                val unit = after.first().cents
                val total = if (after.size >= 2) after.last().cents else null
                return checked(text.substring(0, times.range.first), quantity, unit, total)
            }
        }

        // 2) "2 Pivo 6,00"
        LEADING_QUANTITY.find(text)?.let { leading ->
            val quantity = leading.groupValues[1].toDouble()
            val total = positive.last().cents
            val unit = if (positive.size >= 2) positive[positive.size - 2].cents
            else total.takeIf { it % quantity.toLong() == 0L }?.div(quantity.toLong())
            val name = text.substring(leading.range.last + 1, positive.first().range.first)
            return checked(name, quantity, unit, total)
        }

        val first = positive.first()
        val total = positive.last().cents
        if (positive.size >= 2) {
            // 3a) "Kava 2,00 1,50 3,00": quantity printed with two decimals too
            if (positive.size >= 3) {
                val quantity = positive[0].cents / 100.0
                val unit = positive[1].cents
                if (matches(quantity, unit, total)) return RowParse(text.substring(0, first.range.first).cleanName(), quantity, unit, total)
            }
            // 3b) "Kava 2 1,50 3,00": plain number right before the unit price
            QUANTITY_BEFORE_AMOUNT.find(text.substring(0, first.range.first))?.let { token ->
                val quantity = token.groupValues[1].toQuantity()
                if (quantity != null && matches(quantity, first.cents, total)) {
                    return RowParse(text.substring(0, token.range.first).cleanName(), quantity, first.cents, total)
                }
            }
            // 3c) "Kava 1,50 3,00": total is an exact multiple of the unit price
            if (total % first.cents == 0L && total / first.cents in 2..99) {
                return RowParse(text.substring(0, first.range.first).cleanName(), (total / first.cents).toDouble(), first.cents, total)
            }
        }
        // 4) "Kruh bijeli 1,29": only the line total is known
        return RowParse(text.substring(0, first.range.first).cleanName(), null, null, total)
    }
    private fun checked(name: String, quantity: Double, unit: Long?, total: Long?): RowParse =
        if (unit != null && total != null && !matches(quantity, unit, total)) RowParse(name.cleanName(), null, null, total)
        else RowParse(name.cleanName(), quantity, unit, total)

    private fun matches(quantity: Double, unit: Long, total: Long) = abs((quantity * unit).roundToLong() - total) <= ROUNDING_CENTS
    private fun ReceiptRow.isNeverAnItem(): Boolean {
        val words = text.toUppercaseWithoutDiacritics().split(Regex("[^A-Z]+")).toSet()
        return NOT_ITEMS.any { it in keywordText } || NOT_ITEM_WORDS.any { it in words }
    }
    private fun ReceiptRow.hasName() = text.hasLetters() && findDates(text).isEmpty()
    private fun String.hasLetters() = count { it.isLetter() } >= 2
    private fun String.toQuantity(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it < 10_000 }

    private fun String.cleanName(): String =
        trim()
            .replace(PLU_CODE, "")
            .trimEnd(' ', ':', '*', '-', 'x', 'X', '×', '@')
            .replace(Regex("\\s+(KOM|kom|Kom|KG|kg|Kg)\\.?$"), "")
            .trim()
    private companion object {
        const val ROUNDING_CENTS = 1L

        // First row of the totals section: items are above it (letters only, no spaces, no diacritics)
        val ITEMS_END = listOf(
            "UKUPNO", "SVEUKUPNO", "UKUPANIZNOS", "ZAPLATITI", "ZAPLATIT", "MEDUZBROJ", "MEDJUZBROJ",
            "TOTAL", "AMOUNTDUE", "BALANCEDUE", "GESAMT", "SUMME", "ZWISCHENSUMME", "ZUZAHLEN"
        )
        // Matched as part of the row's keyword text
        val NOT_ITEMS = listOf(
            "PDV", "POREZ", "MWST", "OSNOVICA", "POPUST", "DISCOUNT", "RABAT", "POVRAT", "OSTATAK", "WECHSELGELD",
            "GOTOVINA", "GOTOVINSKI", "KARTICA", "KARTICE", "MASTERCARD", "MAESTRO", "DINERS", "NAPOJNIC", "TRINKGELD"
        )
        // Short words matched only as whole words, so "Ulje Oliva" or "Taxi" are not thrown away
        val NOT_ITEM_WORDS = setOf("VAT", "TAX", "IVA", "CASH", "CHANGE", "CARD", "VISA", "AMEX", "TIP", "TIPS")

        // "2 x", "2,000 x", "1,250 kg x", "3 kom *"
        val TIMES = Regex("""(?<![\d.,])(\d{1,4}(?:[.,]\d{1,3})?)\s*(?:[A-Za-z]{1,3}\.?)?\s*[xX×*]\s*(?=\d)""")
        // "2 Pivo ..." - small whole number, then a word
        val LEADING_QUANTITY = Regex("""^(\d{1,2})\s+(?=\p{L})""")
        // "Kava 2 " right before the first amount
        val QUANTITY_BEFORE_AMOUNT = Regex("""(?<=\s)(\d{1,4}(?:[.,]\d{1,3})?)\s*$""")
        // Article code at the start of the name: "123456 Kruh"
        val PLU_CODE = Regex("""^\d{4,}\s+""")
    }
}