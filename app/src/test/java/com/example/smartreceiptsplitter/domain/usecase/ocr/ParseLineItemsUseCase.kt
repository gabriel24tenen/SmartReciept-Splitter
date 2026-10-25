package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.ParsedReceiptItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseLineItemsUseCaseTest {

    private val parseItems = ParseLineItemsUseCase()

    private fun parse(vararg rows: String) = parseItems(receiptText(*rows))

    private fun item(name: String, total: Long, quantity: Double = 1.0, unit: Long? = null) =
        ParsedReceiptItem(name = name, quantity = quantity, unitPrice = unit, totalPrice = total)

    @Test
    fun readsItemsFromCroatianFiscalReceipt() {
        val items = parse(
            "KONZUM PLUS d.o.o.",
            "Ulica grada Vukovara 1, Zagreb",
            "OIB: 29955634590",
            "Račun br. 123/1/1",
            "Kruh bijeli | 1,29",
            "Mlijeko 2,8% 1L | 1,19",
            "UKUPNO EUR | 2,48",
            "Gotovina | 5,00",
            "PDV 25% | 1,98 | 0,50",
            "25.10.2026 14:32"
        )
        assertEquals(listOf(item("Kruh bijeli", 129), item("Mlijeko 2,8% 1L", 119)), items)
    }

    @Test
    fun readsQuantityTimesUnitPriceOnTheSameRow() {
        assertEquals(listOf(item("Jogurt", 178, 2.0, 89)), parse("Jogurt 2 x 0,89 | 1,78", "UKUPNO | 1,78"))
        assertEquals(listOf(item("Sok", 300, 2.0, 150)), parse("Sok 2,000 X 1,50 | 3,00", "UKUPNO | 3,00"))
    }

    @Test
    fun readsTableWithQuantityColumn() {
        assertEquals(listOf(item("Kava", 300, 2.0, 150)), parse("Kava | 2 | 1,50 | 3,00", "UKUPNO | 3,00"))
        assertEquals(listOf(item("Kava", 300, 2.0, 150)), parse("Kava | 2,00 | 1,50 | 3,00", "UKUPNO | 3,00"))
    }

    @Test
    fun readsQuantityBeforeTheName() {
        assertEquals(listOf(item("Pivo", 600, 2.0, 300)), parse("2 Pivo | 6,00", "TOTAL | 6,00"))
    }

    @Test
    fun derivesQuantityWhenTotalIsAnExactMultipleOfUnitPrice() {
        assertEquals(listOf(item("Kava", 300, 2.0, 150)), parse("Kava | 1,50 | 3,00", "UKUPNO | 3,00"))
    }

    @Test
    fun joinsNameRowWithQuantityRowBelowIt() {
        val items = parse("Jabuke", "1,250 kg x 1,99 | 2,49", "Banane", "0,850 kg x 1,49", "UKUPNO | 3,76")
        assertEquals(listOf(item("Jabuke", 249, 1.25, 199), item("Banane", 127, 0.85, 149)), items)
    }

    @Test
    fun quantityRowBelowExplainsTheItemAbove() {
        assertEquals(listOf(item("Kruh", 258, 2.0, 129)), parse("Kruh | 2,58", "2 x 1,29", "UKUPNO | 2,58"))
    }

    @Test
    fun quantityRowThatDoesNotMatchIsIgnored() {
        assertEquals(listOf(item("Kruh", 258)), parse("Kruh | 2,58", "3 x 1,29", "UKUPNO | 2,58"))
    }

    @Test
    fun keepsOnlyLineTotalWhenQuantityAndUnitPriceDoNotAddUp() {
        assertEquals(listOf(item("Sok", 400)), parse("Sok 2 x 1,50 | 4,00", "UKUPNO | 4,00"))
    }

    @Test
    fun skipsDiscountVatPaymentAndEverythingBelowTheTotal() {
        val items = parse(
            "Pivo | 3,00",
            "Popust 10% | -0,30",
            "UKUPNO | 2,70",
            "Napojnica | 1,00",
            "Kava | 1,50" // printed below the total, e.g. an ad - not an item of this receipt
        )
        assertEquals(listOf(item("Pivo", 300)), items)
    }

    @Test
    fun withoutTotalRowSkipsPaymentAndVatRows() {
        val items = parse("Pizza | 9,50", "Gotovina | 20,00", "VISA | 9,50", "25% | 1,90 | 0,48")
        assertEquals(listOf(item("Pizza", 950)), items)
    }

    @Test
    fun shortExcludedWordsOnlyMatchWholeWords() {
        assertEquals(listOf(item("Ulje Oliva", 699), item("Taxi Zagreb", 1200)), parse("Ulje Oliva | 6,99", "Taxi Zagreb | 12,00"))
    }

    @Test
    fun removesArticleCodeAndTrailingUnit() {
        assertEquals(listOf(item("Kruh bijeli", 129)), parse("123456 Kruh bijeli kom | 1,29", "UKUPNO | 1,29"))
    }

    @Test
    fun subtotalAlsoEndsTheItems() {
        assertEquals(listOf(item("Burger", 1200)), parse("Burger | 12.00", "SUBTOTAL | 12.00", "TAX | 1.20", "TOTAL | 13.20"))
    }

    @Test
    fun returnsEmptyListWhenThereAreNoItems() {
        assertTrue(parse().isEmpty())
        assertTrue(parse("KONZUM", "UKUPNO | 2,48").isEmpty())
    }
}