package com.example.smartreceiptsplitter.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

private val DATE_PARSERS = listOf("d.M.uuuu", "d.M.uuuu.", "d/M/uuuu", "uuuu-M-d").map {
    DateTimeFormatter.ofPattern(it, Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
}

private val DATE_INPUT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT)

/**
 * Parses text typed by the user into a timestamp: the start of that day in [zone].
 * Accepts 20.10.2026, 20.10.2026., 20/10/2026 and 2026-10-20 (spaces are ignored). Returns null for blank or invalid text.
 */
fun parseDate(text: String, zone: ZoneId = ZoneId.systemDefault()): Long? {
    val compact = text.filterNot { it.isWhitespace() }
    if(compact.isEmpty()) return null
    for(parser in DATE_PARSERS) {
        val date = try {
            LocalDate.parse(compact, parser)
        } catch (e: DateTimeParseException) {
            continue
        }
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }
    return null
}

// Plain text for the date input field: 1792486800000 -> "20.10.2026"
fun formatDateInput(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().format(DATE_INPUT_FORMAT)
