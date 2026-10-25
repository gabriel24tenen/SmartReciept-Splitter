package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.RecognizedText
import com.example.smartreceiptsplitter.domain.model.RecognizedTextBlock
import com.example.smartreceiptsplitter.domain.model.RecognizedTextLine
import com.example.smartreceiptsplitter.domain.model.TextBoundingBox

/**
 * Builds OCR output the way ML Kit returns it for a receipt.
 * Every string is one printed row; " | " splits it into columns. Each column becomes its own block,
 * so a label and its amount end up in different blocks, like on real receipts:
 *
 *   receiptText("UKUPNO | 12,50", "GOTOVINA | 20,00")
 *   -> block 1: "UKUPNO", "GOTOVINA"   block 2: "12,50", "20,00"
 */
fun receiptText(vararg rows: String): RecognizedText {
    val cells = rows.flatMapIndexed { rowIndex, row ->
        row.split(" | ").mapIndexed { column, text ->
            Triple(column, rowIndex, text)
        }
    }
    val blocks = cells.groupBy { it.first }.toSortedMap().map { (column, columnCells) ->
        val lines = columnCells.map { (_, rowIndex, text) ->
            RecognizedTextLine(
                text = text,
                boundingBox = TextBoundingBox(
                    left = column * COLUMN_WIDTH,
                    top = rowIndex * ROW_HEIGHT,
                    right = column * COLUMN_WIDTH + COLUMN_WIDTH - 20,
                    bottom = rowIndex * ROW_HEIGHT + LINE_HEIGHT
                ),
                confidence = 0.9f
            )
        }
        RecognizedTextBlock(text = lines.joinToString("\n") { it.text }, lines = lines, boundingBox = null)
    }
    return RecognizedText(fullText = blocks.joinToString("\n") { it.text }, blocks = blocks)
}

private const val COLUMN_WIDTH = 300
private const val ROW_HEIGHT = 40
private const val LINE_HEIGHT = 30