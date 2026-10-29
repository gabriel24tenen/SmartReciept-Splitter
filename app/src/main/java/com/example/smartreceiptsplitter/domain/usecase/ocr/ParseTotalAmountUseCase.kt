package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.RecognizedText
import javax.inject.Inject
/*
* * Rules:
 * 1. A row is a candidate only if it has a total keyword ("ZA PLATITI", "UKUPNO", "TOTAL", ...) and none of the
 *    excluded words (subtotal, VAT, discount, change, quantity...).
 * 2. The amount is the rightmost positive amount in that row; if the row has none, the row right below it is used
 *    (label and amount printed on two lines).
 * 3. "Amount to pay" keywords beat "total" keywords, which beat card payment rows. Between equal keywords the
 *    larger amount wins (a total is never smaller than a partial sum).
 * 4. No keyword, no total: returns null and the user fills it in. The largest number on the receipt is never guessed.
* */

class ParseTotalAmountUseCase @Inject constructor() {

    operator fun invoke(text: RecognizedText): Long? {
        val rows = text.toRows()
        return rows.indices
            .mapNotNull { index -> candidateAt(rows, index) }
            .maxWithOrNull(compareBy<Candidate> { it.priority }.thenBy { it.cents })
            ?.cents
    }

    private fun candidateAt(rows: List<ReceiptRow>, index: Int): Candidate? {
        val label = rows[index].keywordText
        if(EXCLUDED.any { it in label }) return null
        val priority = PRIORITIES.firstOrNull { (keywords, _) -> keywords.any { it in label } }?.second ?: return null

        val amount = rows[index].totalAmount()
            ?: rows.getOrNull(index + 1)
                ?.takeIf { next -> next.keywordText in CURRENCY_ONLY }
                ?.totalAmount()
            ?:return null
        return Candidate(cents = amount, priority = priority)
    }

    //Total is the largest in the row
    private fun ReceiptRow.totalAmount(): Long? {
        val amounts = findAmounts(text).filter { !it.isNegative && it.cents > 0 }.map { it.cents }
        val last = amounts.lastOrNull() ?: return null
        return last.takeIf { it == amounts.max() }
    }
    private data class Candidate(val cents: Long, val priority: Int)

    private companion object {
        val AMOUNT_TO_PAY = listOf(
            "ZAPLATITI", "ZAPLATIT", "ZAUPLATU", "ZANAPLATU", "AMOUNTDUE", "BALANCEDUE", "TOTALDUE", "TOPAY",
            "ZUZAHLEN", "DAPAGARE"
        )
        val TOTAL = listOf(
            "SVEUKUPNO", "UKUPNO", "UKUPANIZNOS", "GRANDTOTAL", "TOTAL", "TOTALE", "GESAMT", "SUMME", "SUMA"
        )
        val CARD_PAYMENT = listOf("KARTICA", "KARTICE", "CARD", "VISA", "MASTERCARD", "MAESTRO", "DINERS", "AMEX")

        val PRIORITIES = listOf(AMOUNT_TO_PAY to 3, TOTAL to 2, CARD_PAYMENT to 1)

        val EXCLUDED = listOf(
            "SUBTOTAL", "MEDUZBROJ", "MEDJUZBROJ", "ZWISCHENSUMME", "SUBTOTALE",
            "PDV", "VAT", "TAX", "POREZ", "MWST", "IVA", "OSNOVICA", "NETO", "NETTO",
            "POPUST", "DISCOUNT", "RABAT", "USTEDA", "SAVING",
            "POVRAT", "OSTATAK", "CHANGE", "WECHSELGELD", "RESTO",
            "KOLICIN", "ARTIK", "STAVK", "ITEMS"
        )

        val CURRENCY_ONLY = setOf("", "EUR", "EURA", "USD", "GBP", "CHF")
    }
}