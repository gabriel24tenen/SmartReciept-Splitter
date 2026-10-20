package com.example.smartreceiptsplitter.ui.util

import java.math.BigDecimal
import java.text.NumberFormat

/** Formats an amount stored in cents (Long) for display, e.g. 1250, "EUR" -> "12,50 EUR". */
fun formatMoney(amountInCents: Long, currency: String): String {
    val number = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(amountInCents, 2))
    return "$number $currency"
}
