package com.finoria.app.data.repository

import com.finoria.app.data.local.StorageService
import com.finoria.app.data.model.Account
import com.finoria.app.data.model.RecurringTransaction
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionManager
import com.finoria.app.data.model.WidgetShortcut
import com.finoria.app.domain.service.RecurrenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository central — remplace AccountsManager iOS.
 * Singleton injecté par Hilt. Gère tout le CRUD + persistance.
 */
@Singleton
class AccountsRepository @Inject constructor(
    private val storage: StorageService
) {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _transactionManagers = MutableStateFlow<Map<UUID, TransactionManager>>(emptyMap())
    val transactionManagers: StateFlow<Map<UUID, TransactionManager>> = _transactionManagers.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<UUID?>(null)
    val selectedAccountId: StateFlow<UUID?> = _selectedAccountId.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // ─── Initialization ──────────────────────────────────────────────

    suspend fun init() {
        val (loadedAccounts, loadedManagers) = storage.load()
        _accounts.value = loadedAccounts
        _transactionManagers.value = loadedManagers
        _selectedAccountId.value = storage.loadSelectedAccountId()
            ?: loadedAccounts.firstOrNull()?.id

        // Process recurring transactions on startup
        processRecurrences()

        _isInitialized.value = true
    }

    // ─── Account CRUD ────────────────────────────────────────────────

    suspend fun addAccount(account: Account) {
        _accounts.value = _accounts.value + account
        _transactionManagers.value = _transactionManagers.value +
                (account.id to TransactionManager(accountName = account.name))
        if (_selectedAccountId.value == null) {
            selectAccount(account.id)
        }
        persist()
    }

    suspend fun updateAccount(account: Account) {
        _accounts.value = _accounts.value.map {
            if (it.id == account.id) account else it
        }
        persist()
    }

    suspend fun deleteAccount(account: Account) {
        _accounts.value = _accounts.value.filter { it.id != account.id }
        _transactionManagers.value = _transactionManagers.value - account.id
        if (_selectedAccountId.value == account.id) {
            selectAccount(_accounts.value.firstOrNull()?.id)
        }
        persist()
    }

    suspend fun resetAccount(account: Account) {
        _transactionManagers.value = _transactionManagers.value +
                (account.id to TransactionManager(accountName = account.name))
        persist()
    }

    suspend fun selectAccount(id: UUID?) {
        _selectedAccountId.value = id
        storage.saveSelectedAccountId(id)
    }

    // ─── Transaction CRUD ────────────────────────────────────────────

    suspend fun addTransaction(accountId: UUID, transaction: Transaction) {
        updateManager(accountId) { it.addTransaction(transaction) }
    }

    suspend fun updateTransaction(accountId: UUID, transaction: Transaction) {
        updateManager(accountId) { it.updateTransaction(transaction) }
    }

    suspend fun removeTransaction(accountId: UUID, transaction: Transaction) {
        updateManager(accountId) { it.removeTransaction(transaction) }
    }

    suspend fun validateTransaction(accountId: UUID, transaction: Transaction) {
        updateManager(accountId) {
            it.updateTransaction(transaction.validated())
        }
    }

    // ─── Shortcut CRUD ───────────────────────────────────────────────

    suspend fun addShortcut(accountId: UUID, shortcut: WidgetShortcut) {
        updateManager(accountId) { it.addShortcut(shortcut) }
    }

    suspend fun updateShortcut(accountId: UUID, shortcut: WidgetShortcut) {
        updateManager(accountId) { it.updateShortcut(shortcut) }
    }

    suspend fun removeShortcut(accountId: UUID, shortcut: WidgetShortcut) {
        updateManager(accountId) { it.removeShortcut(shortcut) }
    }

    // ─── Recurring CRUD ──────────────────────────────────────────────

    suspend fun addRecurring(accountId: UUID, recurring: RecurringTransaction) {
        updateManager(accountId) { it.addRecurring(recurring) }
        processRecurrences()
    }

    suspend fun updateRecurring(accountId: UUID, recurring: RecurringTransaction) {
        updateManager(accountId) { manager ->
            // Remove old potential transactions
            RecurrenceEngine.removePotentialTransactions(recurring.id, manager.transactions)
            manager.updateRecurring(recurring)
        }
        processRecurrences()
    }

    suspend fun removeRecurring(accountId: UUID, recurring: RecurringTransaction) {
        updateManager(accountId) { manager ->
            RecurrenceEngine.removePotentialTransactions(recurring.id, manager.transactions)
            manager.removeRecurring(recurring)
        }
        persist()
    }

    suspend fun togglePauseRecurring(accountId: UUID, recurring: RecurringTransaction) {
        val updated = recurring.copy(isPaused = !recurring.isPaused)
        updateManager(accountId) { manager ->
            if (updated.isPaused) {
                RecurrenceEngine.removePotentialTransactions(recurring.id, manager.transactions)
            }
            manager.updateRecurring(updated)
        }
        if (!updated.isPaused) {
            processRecurrences()
        } else {
            persist()
        }
    }

    // ─── Bulk operations ─────────────────────────────────────────────

    suspend fun importTransactions(accountId: UUID, transactions: List<Transaction>) {
        updateManager(accountId) { manager ->
            transactions.forEach { manager.addTransaction(it) }
        }
    }

    // ─── Recurring processing ────────────────────────────────────────

    suspend fun processRecurrences() {
        // Create deep copies first so originals stay unchanged for StateFlow comparison
        val managersCopy = _transactionManagers.value.mapValues { (_, manager) ->
            manager.copy(
                transactions = manager.transactions.toMutableList(),
                widgetShortcuts = manager.widgetShortcuts.toMutableList(),
                recurringTransactions = manager.recurringTransactions.toMutableList()
            )
        }
        val modified = RecurrenceEngine.processAll(
            _accounts.value,
            managersCopy
        )
        if (modified) {
            _transactionManagers.value = managersCopy
            persist()
        }
    }

    // ─── Internal helpers ────────────────────────────────────────────

    private suspend fun updateManager(accountId: UUID, action: (TransactionManager) -> Unit) {
        val manager = _transactionManagers.value[accountId] ?: return
        // Create a deep copy BEFORE mutation so the original stays unchanged.
        // This ensures StateFlow detects the change (old != new by equals).
        val newManager = manager.copy(
            transactions = manager.transactions.toMutableList(),
            widgetShortcuts = manager.widgetShortcuts.toMutableList(),
            recurringTransactions = manager.recurringTransactions.toMutableList()
        )
        action(newManager)
        _transactionManagers.value = _transactionManagers.value.toMutableMap().apply {
            put(accountId, newManager)
        }
        persist()
    }

    private suspend fun persist() {
        storage.save(_accounts.value, _transactionManagers.value)
    }
}
