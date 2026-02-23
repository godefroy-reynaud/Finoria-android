package com.finoria.app.data.model

import kotlinx.serialization.Serializable

/**
 * Type de transaction : revenu ou dépense.
 */
@Serializable
enum class TransactionType(val symbol: String, val label: String) {
    INCOME("+", "Revenu"),
    EXPENSE("-", "Dépense")
}
