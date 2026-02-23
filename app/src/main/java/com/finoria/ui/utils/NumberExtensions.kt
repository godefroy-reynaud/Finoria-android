package com.finoria.ui.utils

import java.text.NumberFormat
import java.util.Locale

fun Double.formattedCurrency(withSymbol: Boolean = true): String {
    val format = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    if (!withSymbol) {
        return format.format(this).replace(Regex("[^\\d.,-]"), "").trim()
    }
    return format.format(this)
}

fun Double.compactAmount(): String {
    return when {
        this >= 1_000_000 || this <= -1_000_000 -> "${(this / 1_000_000).formattedCurrency(false)}M"
        this >= 1_000 || this <= -1_000 -> "${(this / 1_000).formattedCurrency(false)}k"
        else -> this.formattedCurrency(false)
    }
}