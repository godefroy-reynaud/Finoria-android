package com.finoria.app.util

import java.util.Locale
import kotlin.math.abs

/**
 * Fonctions utilitaires de formatage de nombres et monnaies.
 */

/**
 * Formate un montant en devise avec le symbole €.
 * Ex: 1234.56 → "1 234,56 €"
 */
fun Double.formattedCurrency(): String =
    String.format(Locale.FRANCE, "%,.2f €", this)

/**
 * Formate un montant de manière compacte.
 * Ex: 1500.0 → "1.5k", 2300000.0 → "2.3M"
 */
fun Double.compactAmount(): String {
    val absValue = abs(this)
    val sign = if (this < 0) "-" else ""
    return when {
        absValue >= 1_000_000 -> "${sign}${String.format(Locale.FRANCE, "%.1f", absValue / 1_000_000)}M"
        absValue >= 1_000 -> "${sign}${String.format(Locale.FRANCE, "%.1f", absValue / 1_000)}k"
        else -> "${sign}${String.format(Locale.FRANCE, "%.0f", absValue)}"
    }
}

/**
 * Formate un montant signé avec couleur implicite (+ ou -).
 */
fun Double.signedCurrency(): String {
    val prefix = if (this >= 0) "+" else ""
    return "$prefix${String.format(Locale.FRANCE, "%,.2f €", this)}"
}
