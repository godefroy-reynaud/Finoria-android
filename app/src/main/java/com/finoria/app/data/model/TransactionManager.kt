package com.finoria.app.data.model

import kotlinx.serialization.Serializable

/**
 * Vue regroupée des données d'un compte (transactions, raccourcis, récurrences,
 * catégories personnalisées). Reconstruite par AccountsRepository à chaque
 * émission Room ; les listes ne sont mutées que par le RecurrenceEngine.
 * Également désérialisée depuis l'ancienne persistance JSON (migration one-shot).
 */
@Serializable
data class TransactionManager(
    val accountName: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val widgetShortcuts: MutableList<WidgetShortcut> = mutableListOf(),
    val recurringTransactions: MutableList<RecurringTransaction> = mutableListOf(),
    val customCategories: MutableList<CustomCategory> = mutableListOf()
)
