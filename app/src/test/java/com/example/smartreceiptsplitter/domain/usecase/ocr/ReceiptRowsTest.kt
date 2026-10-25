package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.RecognizedText
import com.example.smartreceiptsplitter.domain.model.RecognizedTextBlock
import com.example.smartreceiptsplitter.domain.model.RecognizedTextLine
import com.example.smartreceiptsplitter.domain.model.TextBoundingBox
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptRowsTest {

    @Test
    fun joinsLinesFromDifferentBlocksIntoRows() {
        val rows = receiptText("Kava | 2,50", "UKUPNO | 12,50").toRows()
        assertEquals(listOf("Kava 2,50", "UKUPNO 12,50"), rows.map { it.text })
    }

    @Test
    fun toleratesSlightlyTiltedRows() {
        val label = line("UKUPNO", left = 0, top = 100)
        val amount = line("12,50", left = 400, top = 108) // a few pixels lower, still the same row
        val next = line("Gotovina", left = 0, top = 140)
        val rows = text(listOf(next, amount, label)).toRows()
        assertEquals(listOf("UKUPNO 12,50", "Gotovina"), rows.map { it.text })
    }

    @Test
    fun keepsRecognizerOrderWhenBoundingBoxesAreMissing() {
        val rows = text(listOf(line("A", null), line("B", null))).toRows()
        assertEquals(listOf("A", "B"), rows.map { it.text })
    }

    @Test
    fun keywordTextIgnoresCaseDiacriticsSpacesAndZeroForO() {
        assertEquals("MEDUZBROJ", "Međuzbroj:".toKeywordText())
        assertEquals("UKUPNO", "U K U P N 0".toKeywordText())
        assertEquals("ZAPLATITI", "Za platiti:".toKeywordText())
    }

    private fun line(text: String, left: Int, top: Int) =
        RecognizedTextLine(text, TextBoundingBox(left, top, left + 200, top + 30), 0.9f)

    private fun line(text: String, box: TextBoundingBox?) = RecognizedTextLine(text, box, null)

    private fun text(lines: List<RecognizedTextLine>) =
        RecognizedText(
            fullText = lines.joinToString("\n") { it.text },
            blocks = lines.map { RecognizedTextBlock(it.text, listOf(it), it.boundingBox) }
        )
}