package com.finoria.app.data.model

import kotlinx.serialization.Serializable

/**
 * Gestionnaire des transactions, raccourcis et récurrences pour un compte donné.
 * Mutable — utilisé dans AccountsRepository pour stocker l'état par compte.
 */
@Serializable
data class TransactionManager(
    val accountName: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val widgetShortcuts: MutableList<WidgetShortcut> = mutableListOf(),
    val recurringTransactions: MutableList<RecurringTransaction> = mutableListOf()
) {
    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
    }

    fun removeTransaction(transaction: Transaction) {
        transactions.removeAll { it.id == transaction.id }
    }

    fun updateTransaction(transaction: Transaction) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index >= 0) transactions[index] = transaction
    }

    fun addShortcut(shortcut: WidgetShortcut) {
        widgetShortcuts.add(shortcut)
    }

    fun removeShortcut(shortcut: WidgetShortcut) {
        widgetShortcuts.removeAll { it.id == shortcut.id }
    }

    fun updateShortcut(shortcut: WidgetShortcut) {
        val index = widgetShortcuts.indexOfFirst { it.id == shortcut.id }
        if (index >= 0) widgetShortcuts[index] = shortcut
    }

    fun addRecurring(recurring: RecurringTransaction) {
        recurringTransactions.add(recurring)
    }

    fun removeRecurring(recurring: RecurringTransaction) {
        recurringTransactions.removeAll { it.id == recurring.id }
    }

    fun updateRecurring(recurring: RecurringTransaction) {
        val index = recurringTransactions.indexOfFirst { it.id == recurring.id }
        if (index >= 0) recurringTransactions[index] = recurring
    }
}
