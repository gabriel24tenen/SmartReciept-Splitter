package com.example.smartreceiptsplitter.domain.usecase.ocr

import com.example.smartreceiptsplitter.domain.model.RecognizedText
import com.example.smartreceiptsplitter.domain.model.RecognizedTextLine
import java.text.Normalizer

data class ReceiptRow(val lines: List<RecognizedTextLine>) {
    val text: String = lines.joinToString(" ") { it.text.trim() }
    val keywordText: String by lazy { text.toKeywordText() }
}

fun RecognizedText.toRows(): List<ReceiptRow> {
    val all = lines.filter { it.text.isNotBlank() }
    if(all.any { it.boundingBox == null }) return all.map { ReceiptRow(listOf(it)) }

    val rows = mutableListOf<MutableList<RecognizedTextLine>>()
    for (line in all.sortedBy { it.boundingBox!!.centerY }) {
        val box = line.boundingBox!!
        val current = rows.lastOrNull()
        val belongsToCurrent = current != null && current.any { other ->
            val otherBox = other.boundingBox!!
            val tolerance = minOf(box.height, otherBox.height) / 2
            kotlin.math.abs(box.centerY - otherBox.centerY) <= tolerance
        }
        if (belongsToCurrent) current.add(line) else rows.add(mutableListOf(line))
    }
    return rows.map { row -> ReceiptRow(row.sortedBy { it.boundingBox!!.left }) }
}

internal fun String.toKeywordText(): String =
    // '0' counts as 'O' only next to letters ("UKUPN0", "T0TAL", "U K U P N 0"), never inside a number ("12,50")
    toUppercaseWithoutDiacritics().replace(ZERO_AS_LETTER_0, "O").filter { it in 'A'..'Z' }

// spaces, digits and punctuation are kept
internal fun String.toUppercaseWithoutDiacritics(): String =
    Normalizer.normalize(uppercase().replace('Đ', 'D'), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
private val ZERO_AS_LETTER_0 = Regex("""(?<=[A-Z]\s?)0(?![\d.,])|0(?=[A-Z])""")