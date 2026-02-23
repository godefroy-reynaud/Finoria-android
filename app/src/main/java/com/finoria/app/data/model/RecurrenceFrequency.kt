package com.finoria.app.data.model

import kotlinx.serialization.Serializable

/**
 * Fréquence de récurrence d'une transaction.
 */
@Serializable
enum class RecurrenceFrequency(val label: String, val shortLabel: String) {
    DAILY("Tous les jours", "Quotidien"),
    WEEKLY("Toutes les semaines", "Hebdo"),
    MONTHLY("Tous les mois", "Mensuel"),
    YEARLY("Tous les ans", "Annuel")
}
