package com.finoria.ui.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun LocalDate.dayHeaderFormatted(): String {
    val today = LocalDate.now()
    return when {
        this.isEqual(today) -> "Aujourd'hui"
        this.isEqual(today.minusDays(1)) -> "Hier"
        else -> this.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH))
    }
}

fun LocalDate.monthName(): String {
    return this.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }
}
