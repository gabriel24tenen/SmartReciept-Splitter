package com.example.smartreceiptsplitter.domain.usecase.receipt

import java.text.Normalizer

private val COMBINING_MARKS = Regex("\\p{Mn}+")

internal fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text.trim().lowercase().replace('đ', 'd'), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")//đ has no decomposistion in Unicode so it has to be mapped by hand to d

fun matchesMerchant(merchantName: String?, query: String): Boolean {
    val needle = normalizeForSearch(query)
    if (needle.isEmpty()) return true
    return normalizeForSearch(merchantName.orEmpty()).contains(needle)
}