package com.example.smartreceiptsplitter.domain.usecase.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseTaxAmountUseCaseTest {

    private val parseTax = ParseTaxAmountUseCase()

    private fun parse(vararg rows: String) = parseTax(receiptText(*rows))

    @Test
    fun readsVatFromCroatianFiscalReceipt() {
        val tax = parse(
            "Kruh bijeli | 1,29",
            "Mlijeko 2,8% 1L | 1,19",
            "UKUPNO EUR | 2,48",
            "Gotovina | 5,00",
            "PDV 25% | 1,98 | 0,50",
            "25.10.2026 14:32"
        )
        assertEquals(50L, tax)
    }

    @Test
    fun readsTaxTableWithoutLabelsInRows() {
        val tax = parse(
            "Stopa | Osnovica | PDV",
            "25,00% | 8,00 | 2,00",
            "13,00% | 10,00 | 1,30",
            "UKUPNO | 18,00 | 3,30"
        )
        assertEquals(330L, tax)
    }

    @Test
    fun addsUpSeveralRatesAndConsumptionTax() {
        assertEquals(160L, parse("PDV 13% | 10,00 | 1,30", "PNP 3% | 10,00 | 0,30"))
    }

    @Test
    fun summaryRowWins() {
        assertEquals(330L, parse("PDV 25% | 8,00 | 2,00", "PDV 13% | 10,00 | 1,30", "Ukupno PDV | 3,30"))
        assertEquals(250L, parse("Iznos PDV-a: | 2,50"))
    }

    @Test
    fun readsEnglishAndGermanReceipts() {
        assertEquals(210L, parse("SUBTOTAL | 21.00", "TAX | 2.10", "TOTAL | 23.10"))
        assertEquals(178L, parse("Sales Tax 8.875% | 1.78"))
        assertEquals(400L, parse("VAT 20% | 20.00 | 4.00"))
        assertEquals(303L, parse("MwSt 19% | 15,96 | 3,03 | 18,99"))
        assertEquals(303L, parse("enth. MwSt 19% | 3,03"))
    }

    @Test
    fun readsTaxPartOfGrossAmount() {
        // only gross 18,99 and tax 3,03 printed: 18,99 x 19 / 119 = 3,03
        assertEquals(303L, parse("MwSt 19% | 18,99 | 3,03"))
    }

    @Test
    fun ignoresGrossAndNetRows() {
        assertEquals(250L, parse("Iznos bez PDV-a | 10,00", "Ukupno s PDV-om | 12,50", "PDV 25% | 2,50"))
        assertNull(parse("TOTAL INCL. VAT | 23.40"))
        assertNull(parse("Osnovica PDV | 10,00"))
    }

    @Test
    fun skipsRowWhenNoAmountFitsTheRate() {
        assertNull(parse("PDV 25% | 5,00 | 3,00"))
    }

    @Test
    fun zeroRateGivesZeroTax() {
        assertEquals(0L, parse("PDV 0% | 5,00 | 0,00"))
    }

    @Test
    fun returnsNullWithoutTaxInformation() {
        assertNull(parse("Kava | 1,50", "UKUPNO | 1,50"))
        assertNull(parse("Taxi Zagreb | 12,00")) // "TAX" only as a whole word
        assertNull(parse("PDV ID: HR12345678901"))
        assertNull(parse())
    }

    @Test
    fun toleratesLetterOReadInsteadOfZero() {
        assertEquals(50L, parse("PDV 25% | 2,OO | O,5O"))
    }
}