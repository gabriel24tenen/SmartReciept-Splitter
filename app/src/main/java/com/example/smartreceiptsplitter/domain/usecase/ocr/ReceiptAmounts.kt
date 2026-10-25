package com.example.smartreceiptsplitter.domain.usecase.ocr

//cents is absolute value in cents, isNegative is written with a minus before or after number -> discount
data class AmountMatch(val cents: Long, val isNegative: Boolean, val range: IntRange)

private val AMOUNT_REGEX = Regex(
    """(?<![\d.,])(-\s?)?(\d{1,3}(?:[.,]\d{3})+|\d+)[.,](\d{2})(?![\d%])(?![.,]\d)(\s?-)?"""
)

fun findAmounts(text: String): List<AmountMatch> {
    val cleaned = text.fixLetterOInNumbers()
    return AMOUNT_REGEX.findAll(cleaned).mapNotNull { match ->
        val integerPart = match.groupValues[2].filter { it.isDigit() }
        val cents = "$integerPart${match.groupValues[3]}".toLongOrNull() ?: return@mapNotNull null
        val isNegative = match.groupValues[1].isNotEmpty() || match.groupValues[4].isNotEmpty()
        AmountMatch(cents = cents, isNegative = isNegative, range = match.range)
    }.toList()
}

internal fun String.fixLetterOInNumbers(): String =
    split(' ').joinToString(" ") { token ->
        val isNumberLike = token.any { it.isDigit() } && token.all { it.isDigit() || it in ".,-/:Oo€" }
        if (isNumberLike) token.replace('O', '0').replace('o', '0') else token
    }