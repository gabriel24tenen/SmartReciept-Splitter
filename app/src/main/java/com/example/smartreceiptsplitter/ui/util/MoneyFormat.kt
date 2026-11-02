package com.example.smartreceiptsplitter.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/** Formats an amount stored in cents (Long) for display, e.g. 1250, "EUR" -> "12,50 EUR". */

const val EXAMPLE_AMOUNT = 1250L //example amount for hints and error messages, with local's separator
fun formatMoney(amountInCents: Long, currency: String, locale: Locale = Locale.getDefault()): String {
    val number = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(amountInCents, 2))
    return "$number $currency"
}

// Plain text for an input field, always with a dot: 1250 -> "12.50"
fun formatMoneyInput(amountInCents: Long, locale: Locale = Locale.getDefault()) : String {
    val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return BigDecimal.valueOf(amountInCents, 2).toPlainString().replace('.', separator)
}

// Quantity of an item, at most three decimals and without trailing zeros: 2.0 -> "2", 1.25 -> "1,25" / "1.25"
fun formatQuantity(quantity: Double, locale: Locale = Locale.getDefault()) : String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 3
        isGroupingUsed = false
    }.format(quantity)

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