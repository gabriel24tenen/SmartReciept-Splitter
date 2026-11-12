package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.ReceiptLimits
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import javax.inject.Inject

/*
* Rules:
* 1. Only the header is searched: the rows above the first row with an amount (the first item or the total),
*    at most [MAX_HEADER_ROWS] rows.
* 2. A row with a company form ("d.o.o.", "j.d.o.o.", "d.d.", "k.d.", "obrt", "GmbH", "Ltd", "Inc", "LLC") wins.
* 3. Otherwise the first row with at least 3 letters that is not an address, a tax/registration number, contact data,
*    a date or time, or a document title ("RAČUN", "RECEIPT", "BLAGAJNA"...).
* 4. Nothing plausible: null, the user types the merchant. An item name is never taken.
* The text is kept as printed (case, company form), only spaces and trailing separators are cleaned up.
*/
class ParseMerchantNameUseCase @Inject constructor() {

    operator fun invoke(text: RecognizedText): String? {
        val header = text.toRows()
            .takeWhile { row ->  findAmounts(row.text).isEmpty() }
            .take(MAX_HEADER_ROWS)
            .map { it.text.cleanMerchant() }
            .filter { it.isPossibleMerchant() }

        val merchant = header.firstOrNull { COMPANY_FORM.containsMatchIn(it) } ?: header.firstOrNull()
        return merchant?.take(ReceiptLimits.MAX_MERCHANT_LENGTH)?.trim()
    }

    private fun String.isPossibleMerchant(): Boolean {
        if (count { it.isLetter() } < MIN_LETTERS) return false
        if (findDates(this).isNotEmpty() || TIME.containsMatchIn(this)) return false
        if (POSTAL_CODE_AND_CITY.matches(this) || STREET_AND_NUMBER.matches(this)) return false
        val upper = toUppercaseWithoutDiacritics()
        val words = upper.split(Regex("[^A-Z]+")).filter { it.isNotEmpty() }.toSet()
        if (NOT_MERCHANT_WORDS.any { it in words }) return false
        val keyword = upper.filter { it in 'A'..'Z' }
        if (NOT_MERCHANT_PARTS.any { it in keyword }) return false
        if (STREET_WORDS.any { it in words } && any { it.isDigit() }) return false
        return true
    }

    private fun String.cleanMerchant(): String =
        replace(Regex("\\s+"), " ").trim().trimEnd(':', '-', '|', ',', ' ').trimStart('*', '-', ' ')

    private companion object {
        const val MAX_HEADER_ROWS = 8
        const val MIN_LETTERS = 3

        val COMPANY_FORM = Regex(
            """(?i)(?<![\p{L}])(j\.?\s?d\.?\s?o\.?\s?o\.?|d\.?\s?o\.?\s?o\.?|d\.\s?d\.|k\.\s?d\.|obrt|gmbh|ltd\.?|inc\.?|llc|s\.?r\.?l\.?)(?![\p{L}])"""
        )

        val NOT_MERCHANT_WORDS = setOf(
            "OIB", "MB", "MBS", "IBAN", "UID", "TEL", "FAX", "MOB", "EMAIL", "WWW", "HTTP", "HTTPS",
            "RACUN", "RACUNA", "RECEIPT", "INVOICE", "BILL", "RECHNUNG", "BLAGAJNA", "KASA", "OPERATER", "OPERATOR",
            "KONOBAR", "STOL", "TABLE", "FISKALNI", "JIR", "ZKI", "KOPIJA", "COPY", "DATUM", "DATE", "VRIJEME", "TIME"
        )

        val NOT_MERCHANT_PARTS = listOf("PDVID", "VATID", "USTID", "PORESNIBROJ", "TAXID")
        val STREET_WORDS = setOf("ULICA", "UL", "CESTA", "TRG", "PUT", "OBALA", "STREET", "ST", "AVENUE", "AVE", "ROAD", "RD", "STRASSE", "STR")

        val POSTAL_CODE_AND_CITY = Regex("""^(?:[A-Z]{1,2}-)?\d{4,5}\s+\p{L}[\p{L} .-]*$""") // 10000 Zagre, HR-10000 Zagreb
        val STREET_AND_NUMBER = Regex("""^(?:\p{L}[\p{L}. ]*\s\d{1,4}[a-zA-Z]?|\d{1,5}\s+\p{L}[\p{L} .]*)$""") // Ilica 10, Ulica grada Vukovara 1a, 123 Main Street
        val TIME = Regex("""(?<!\d)([01]?\d|2[0-3]):[0-5]\d(?!\d)""")
    }
}