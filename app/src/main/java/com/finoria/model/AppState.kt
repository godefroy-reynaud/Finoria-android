package com.finoria.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AppState(
    val accounts: List<Account> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val shortcuts: List<WidgetShortcut> = emptyList(),
    // Map<AccountId, List<Transaction>>
    val transactionsByAccount: Map<String, List<Transaction>> = emptyMap(),
    val selectedAccountId: String? = null,
    val schemaVersion: Int = 1
)
