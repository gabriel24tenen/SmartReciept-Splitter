package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.ParsedReceiptItem
import com.example.smartreceiptsplitter.domain.model.ReceiptExtraction
import com.example.smartreceiptsplitter.domain.model.RecognizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExtractReceiptDataUseCaseTest {

    private val zone = ZoneId.of("Europe/Zagreb")
    private val clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), zone)
    private val extract = ExtractReceiptDataUseCase(
        ParseTotalAmountUseCase(), ParseReceiptDateUseCase(), ParseLineItemsUseCase(), ParseTaxAmountUseCase()
    )

    @Test
    fun readsAllFieldsFromOneReceipt() {
        val result = extract(
            receiptText(
                "KONZUM PLUS d.o.o.",
                "Kruh bijeli | 1,29",
                "Mlijeko 2,8% 1L | 1,19",
                "UKUPNO EUR | 2,48",
                "Gotovina | 5,00",
                "PDV 25% | 1,98 | 0,50",
                "25.10.2026 14:32"
            ),
            clock
        )
        assertEquals(248L, result.totalAmount)
        assertEquals(LocalDate.of(2026, 10, 25), result.date)
        assertEquals(50L, result.taxAmount)
        assertEquals(listOf("Kruh bijeli", "Mlijeko 2,8% 1L"), result.items.map { it.name })
        assertEquals(true, result.itemsMatchTotal)
    }

    @Test
    fun dropsTaxThatIsNotSmallerThanTotal() {
        val result = extract(receiptText("UKUPNO | 2,00", "PDV | 5,00"), clock)
        assertEquals(200L, result.totalAmount)
        assertNull(result.taxAmount)
    }

    @Test
    fun emptyTextGivesEmptyResult() {
        assertEquals(ReceiptExtraction.EMPTY, extract(RecognizedText.EMPTY, clock))
    }

    @Test
    fun reportsWhenItemsDoNotAddUpToTotal() {
        val result = extract(receiptText("Pivo | 3,00", "Popust | -0,30", "UKUPNO | 2,70"), clock)
        assertEquals(false, result.itemsMatchTotal)
        assertEquals(300L, result.itemsTotal)
    }

    @Test
    fun draftTakesStartOfDayInUserZone() {
        val extraction = ReceiptExtraction(
            totalAmount = 1250L,
            date = LocalDate.of(2026, 10, 25),
            taxAmount = 250L,
            items = listOf(ParsedReceiptItem("Kava", 1.0, null, 1250L))
        )
        val draft = extraction.toDraft(zone, "EUR")
        assertEquals(LocalDate.of(2026, 10, 25).atStartOfDay(zone).toInstant().toEpochMilli(), draft.date)
        assertEquals(1250L, draft.totalAmount)
        assertEquals(250L, draft.taxAmount)
        assertNull(draft.tipAmount)
        assertEquals("EUR", draft.currency)
    }
}