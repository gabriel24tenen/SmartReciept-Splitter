package com.example.smartreceiptsplitter.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

/** Formats an amount stored in cents (Long) for display, e.g. 1250, "EUR" -> "12,50 EUR". */
fun formatMoney(amountInCents: Long, currency: String): String {
    val number = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(amountInCents, 2))
    return "$number $currency"
}

// Plain text for an input field, always with a dot: 1250 -> "12.50"
fun formatMoneyInput(amountInCents: Long): String =
    BigDecimal.valueOf(amountInCents, 2).toPlainString()

// parsing text typed by the user into cents, accepting both dot or comma. Blank text counts as 0
fun parseMoney(text: String): Long? {
    val normalized = text.trim().replace(',', '.')
    if(normalized.isEmpty()) return 0L
    val value = normalized.toBigDecimalOrNull() ?: return null
    if (value.signum() < 0) return null
    return try {
        value.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
    } catch (e: ArithmeticException) {
        null // more than 2 decimals, or too large for a Long
    }
}