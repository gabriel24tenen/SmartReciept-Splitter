package com.example.smartreceiptsplitter.domain.usecase.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseMerchantNameUseCaseTest {

    private val parseMerchant = ParseMerchantNameUseCase()

    private fun parse(vararg rows: String) = parseMerchant(receiptText(*rows))

    @Test
    fun readsMerchantFromCroatianFiscalReceipt() {
        assertEquals(
            "KONZUM PLUS d.o.o.",
            parse(
                "KONZUM PLUS d.o.o.",
                "Ulica grada Vukovara 1, Zagreb",
                "OIB: 29955634590",
                "Račun br. 123/1/1",
                "Kruh bijeli | 1,29",
                "UKUPNO EUR | 1,29"
            )
        )
    }

    @Test
    fun companyFormWinsOverAStoreNameAbove() {
        assertEquals(
            "LIDL Hrvatska d.o.o. k.d.",
            parse("PRODAVAONICA 105", "LIDL Hrvatska d.o.o. k.d.", "Velika Gorica", "Mlijeko | 0,99", "UKUPNO | 0,99")
        )
        assertEquals("Obrt Pekara Dubravica", parse("Račun br. 5/1/1", "Obrt Pekara Dubravica", "Kifla | 0,40"))
    }

    @Test
    fun withoutCompanyFormTakesTheFirstNameRow() {
        assertEquals(
            "CAFFE BAR CORSO",
            parse("CAFFE BAR CORSO", "Ilica 10", "10000 Zagreb", "OIB 12345678901", "Kava | 1,50", "UKUPNO | 1,50")
        )
    }

    @Test
    fun skipsDocumentTitlesDatesAndContactRows() {
        assertEquals(
            "Pekara Dubravica",
            parse("RAČUN BR. 12/1/1", "25.10.2026 14:32", "Tel: 01 234 5678", "Pekara Dubravica", "Kifla | 0,40")
        )
    }

    @Test
    fun readsForeignHeaders() {
        assertEquals("Starbucks Coffee", parse("Starbucks Coffee", "123 Main Street", "Latte | 4.50", "TOTAL | 4.50"))
        assertEquals(
            "Bäckerei Müller GmbH",
            parse("Bäckerei Müller GmbH", "Hauptstraße 5", "USt-IdNr. DE123456789", "Brezel | 0,90", "SUMME | 0,90")
        )
    }

    @Test
    fun noHeaderMeansNoMerchant() {
        // the first row is already an item, which is never taken as the merchant
        assertNull(parse("Kava | 1,50", "UKUPNO | 1,50"))
    }

    @Test
    fun headerWithOnlyAddressAndNumbersMeansNoMerchant() {
        assertNull(parse("Ilica 10", "10000 Zagreb", "OIB: 29955634590", "Kava | 1,50"))
    }

    @Test
    fun cleansSpacesAndTrailingSeparators() {
        assertEquals("Tommy d.o.o.", parse("  Tommy   d.o.o. :", "Kava | 1,50"))
    }

    @Test
    fun emptyTextHasNoMerchant() {
        assertNull(parse())
    }
}