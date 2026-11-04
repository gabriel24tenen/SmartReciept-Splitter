package com.example.smartreceiptsplitter.ui.util

import com.example.smartreceiptsplitter.domain.model.Receipt

fun Receipt.displayName(): String = merchantName?.takeIf { it.isNotBlank() } ?: "Unknown merchant"
