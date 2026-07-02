package com.finoria.app.data.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Type de transaction : revenu ou dépense.
 */
@Serializable
enum class TransactionType(val symbol: String, val label: String) {
    INCOME("+", "Revenu"),
    EXPENSE("-", "Dépense");

    /**
     * Applique le signe du type à un montant saisi en valeur absolue.
     * Convention de tout le projet : dépense < 0, revenu ≥ 0.
     */
    fun signed(amount: Double): Double =
        if (this == EXPENSE) -abs(amount) else abs(amount)
}
