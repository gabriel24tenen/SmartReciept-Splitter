package com.example.smartreceiptsplitter.ui.util

import com.example.smartreceiptsplitter.domain.model.Receipt
import com.example.smartreceiptsplitter.domain.model.SplitSummary

fun Receipt.displayName(): String = merchantName?.takeIf { it.isNotBlank() } ?: "Unknown merchant"

fun SplitSummary.label(): String = when {
    isOutdated -> "Split outdated"
    participants == 1 -> "Split · 1 person"
    else -> "Split · $participants people"
}