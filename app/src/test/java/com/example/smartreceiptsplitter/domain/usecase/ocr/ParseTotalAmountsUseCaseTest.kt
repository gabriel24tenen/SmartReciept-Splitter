package com.example.smartreceiptsplitter.domain.usecase.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseTotalAmountUseCaseTest {

    private val parseTotal = ParseTotalAmountUseCase()

    @Test
    fun readsTotalFromCroatianFiscalReceipt() {
        val text = receiptText(
            "KONZUM PLUS d.o.o.",
            "OIB: 29955634590",
            "Kruh bijeli | 1,29",
            "Mlijeko 2,8% 1L | 1,19",
            "UKUPNO EUR | 2,48",
            "Gotovina | 5,00",
            "Povrat | 2,52",
            "Stopa | Osnovica | PDV",
            "PDV 25% | 1,98 | 0,50",
            "25.10.2026 14:32"
        )
        assertEquals(248L, parseTotal(text))
    }

    @Test
    fun readsLabelAndAmountFromDifferentBlocksOnTheSameRow() {
        // receiptText puts "UKUPNO" and "12,50" into separate blocks, like ML Kit does
        assertEquals(1250L, parseTotal(receiptText("Kava | 2,50", "UKUPNO | 12,50")))
    }

    @Test
    fun readsAmountPrintedOnTheRowBelowTheLabel() {
        assertEquals(1250L, parseTotal(receiptText("UKUPNO:", "12,50 EUR")))
    }

    @Test
    fun doesNotTakeAnotherLabelledRowBelowTheLabel() {
        assertNull(parseTotal(receiptText("UKUPNO:", "PDV 25% 2,50")))
    }

    @Test
    fun amountToPayWinsOverTotalBeforeDiscount() {
        val text = receiptText("UKUPNO | 15,00", "POPUST | -1,50", "ZA PLATITI | 13,50")
        assertEquals(1350L, parseTotal(text))
    }

    @Test
    fun ignoresSubtotalVatAndChangeRows() {
        val text = receiptText(
            "MEĐUZBROJ | 10,00",
            "PDV 25% | 2,50",
            "UKUPNO | 12,50",
            "GOTOVINA | 20,00",
            "POVRAT | 7,50"
        )
        assertEquals(1250L, parseTotal(text))
    }

    @Test
    fun returnsNullWhenOnlySubtotalAndVatArePrinted() {
        assertNull(parseTotal(receiptText("SUBTOTAL | 10,00", "VAT 20% | 2,00")))
    }

    @Test
    fun returnsNullWithoutAnyTotalKeywordInsteadOfGuessing() {
        assertNull(parseTotal(receiptText("Pizza | 9,50", "Coca-Cola | 3,00", "Gotovina | 20,00")))
    }

    @Test
    fun usesCardPaymentRowWhenThereIsNoTotalKeyword() {
        assertEquals(2340L, parseTotal(receiptText("Pizza | 20,40", "Pivo | 3,00", "VISA | 23,40")))
    }

    @Test
    fun totalKeywordWinsOverCardPayment() {
        assertEquals(1250L, parseTotal(receiptText("UKUPNO | 12,50", "MAESTRO | 12,50", "Napojnica | 2,00")))
    }

    @Test
    fun readsEnglishAndGermanReceipts() {
        assertEquals(2310L, parseTotal(receiptText("SUBTOTAL | 21.00", "TAX | 2.10", "TOTAL | 23.10")))
        assertEquals(1899L, parseTotal(receiptText("Zwischensumme | 15,96", "MwSt 19% | 3,03", "GESAMT EUR | 18,99")))
        assertEquals(4250L, parseTotal(receiptText("BALANCE DUE | $42.50")))
    }

    @Test
    fun readsThousandsSeparator() {
        assertEquals(123456L, parseTotal(receiptText("UKUPNO | 1.234,56")))
        assertEquals(123456L, parseTotal(receiptText("TOTAL | 1,234.56")))
    }

    @Test
    fun toleratesOcrNoise() {
        assertEquals(1250L, parseTotal(receiptText("UKUPN0 | 12,5O"))) // O and 0 swapped
        assertEquals(1250L, parseTotal(receiptText("U K U P N O | 12,50"))) // double-width print
        assertEquals(720L, parseTotal(receiptText("Sveukupno: | 7,20 €"))) // mixed case, colon, currency sign
    }

    @Test
    fun takesRightmostAmountInTotalRow() {
        // VAT table total row: base, VAT, total
        assertEquals(1250L, parseTotal(receiptText("UKUPNO | 10,00 | 2,50 | 12,50")))
    }

    @Test
    fun ignoresDateInTotalRow() {
        assertEquals(1250L, parseTotal(receiptText("TOTAL 12,50 25.10.2026")))
    }

    @Test
    fun returnsNullForEmptyText() {
        assertNull(parseTotal(receiptText()))
    }
}