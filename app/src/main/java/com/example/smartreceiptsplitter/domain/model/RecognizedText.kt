package com.example.smartreceiptsplitter.domain.model

// Raw OCR output of the receipt, independent of the OCR library (ML Kit types are in data layer)
// @property fullText - the whole recognized text, blocks are separated by new lines
// @property blocks text blocks in the order the recognizer returned them (top - bottom)

data class RecognizedText(
    val fullText: String,
    val blocks: List<RecognizedTextBlock>
) {
    val lines: List<RecognizedTextLine> get() = blocks.flatMap { it.lines }  // all lines of all blocks, flattened
    val isEmpty: Boolean get() = blocks.isEmpty() || fullText.isBlank()

    companion object {
        val EMPTY = RecognizedText(fullText = "", blocks = emptyList())
    }
}

data class RecognizedTextBlock(
    val text: String,
    val lines: List<RecognizedTextLine>,
    val boundingBox: TextBoundingBox?
)

data class RecognizedTextLine(
    val text: String,
    val boundingBox: TextBoundingBox?, // position in pixels of the corrected image; used later to put a name and its price into the same row
    val confidence: Float?  //recognizer's confidence for the line (0 / 1); null if the recognizer doesn't report it
)

data class TextBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = bottom - top
}