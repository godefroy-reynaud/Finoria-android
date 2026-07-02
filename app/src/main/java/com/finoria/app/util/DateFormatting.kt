package com.finoria.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Extensions de formatage de dates en français.
 */

private val frenchLocale = Locale.FRENCH

fun LocalDate.dayHeaderFormatted(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "Aujourd'hui"
        today.minusDays(1) -> "Hier"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", frenchLocale)
            this.format(formatter).replaceFirstChar { it.uppercase() }
        }
    }
}

fun LocalDate.shortFormatted(): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(frenchLocale)
    return this.format(formatter)
}

fun monthName(month: Int): String {
    val date = LocalDate.of(2024, month, 1)
    val formatter = DateTimeFormatter.ofPattern("MMMM", frenchLocale)
    return date.format(formatter).replaceFirstChar { it.uppercase() }
}

// ─── Conversions LocalDate ↔ epoch millis (DatePicker Material 3) ────────

fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
