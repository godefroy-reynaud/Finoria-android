package com.finoria.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finoria.app.data.model.Account
import com.finoria.app.data.model.AnalysisType
import com.finoria.app.data.model.CategoryData
import com.finoria.app.data.model.RecurringTransaction
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionManager
import com.finoria.app.data.model.WidgetShortcut
import com.finoria.app.data.repository.AccountsRepository
import com.finoria.app.domain.service.CalculationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel principal de l'application.
 * Expose les données du Repository en UI State via StateFlow.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AccountsRepository
) : ViewModel() {

    // ─── Observable State ────────────────────────────────────────────

    val accounts: StateFlow<List<Account>> = repository.accounts
    val selectedAccountId: StateFlow<UUID?> = repository.selectedAccountId
    val transactionManagers: StateFlow<Map<UUID, TransactionManager>> = repository.transactionManagers

    val selectedAccount: StateFlow<Account?> = combine(
        accounts,
        selectedAccountId
    ) { accs, id ->
        accs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentManager: StateFlow<TransactionManager?> = combine(
        transactionManagers,
        selectedAccountId
    ) { managers, id ->
        id?.let { managers[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentTransactions: StateFlow<List<Transaction>> = currentManager.map {
        it?.transactions.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentShortcuts: StateFlow<List<WidgetShortcut>> = currentManager.map {
        it?.widgetShortcuts.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentRecurring: StateFlow<List<RecurringTransaction>> = currentManager.map {
        it?.recurringTransactions.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Initialization ──────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repository.init()
        }
    }

    // ─── Computed values ─────────────────────────────────────────────

    fun totalNonPotential(transactions: List<Transaction>): Double =
        CalculationService.totalNonPotential(transactions)

    fun totalPotential(transactions: List<Transaction>): Double =
        CalculationService.totalPotential(transactions)

    fun monthlyChangePercentage(transactions: List<Transaction>): Double? =
        CalculationService.monthlyChangePercentage(transactions)

    fun totalForMonth(month: Int, year: Int, transactions: List<Transaction>): Double =
        CalculationService.totalForMonth(month, year, transactions)

    fun totalForYear(year: Int, transactions: List<Transaction>): Double =
        CalculationService.totalForYear(year, transactions)

    fun availableYears(transactions: List<Transaction>): List<Int> =
        CalculationService.availableYears(transactions)

    fun validatedTransactions(
        transactions: List<Transaction>,
        year: Int? = null,
        month: Int? = null
    ): List<Transaction> =
        CalculationService.validatedTransactions(transactions, year, month)

    fun potentialTransactions(transactions: List<Transaction>): List<Transaction> =
        CalculationService.potentialTransactions(transactions)

    fun getCategoryBreakdown(
        transactions: List<Transaction>,
        type: AnalysisType,
        month: Int,
        year: Int
    ): List<CategoryData> =
        CalculationService.getCategoryBreakdown(transactions, type, month, year)

    fun totalNonPotentialForAccount(accountId: UUID): Double {
        val manager = transactionManagers.value[accountId]
        return manager?.transactions?.let { CalculationService.totalNonPotential(it) } ?: 0.0
    }

    fun totalWithPotentialForAccount(accountId: UUID): Double {
        val manager = transactionManagers.value[accountId]
        return manager?.transactions?.let {
            CalculationService.totalNonPotential(it) + CalculationService.totalPotential(it)
        } ?: 0.0
    }

    // ─── Account actions ─────────────────────────────────────────────

    fun addAccount(account: Account) {
        viewModelScope.launch { repository.addAccount(account) }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch { repository.updateAccount(account) }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }

    fun resetAccount(account: Account) {
        viewModelScope.launch { repository.resetAccount(account) }
    }

    fun selectAccount(id: UUID) {
        viewModelScope.launch { repository.selectAccount(id) }
    }

    // ─── Transaction actions ─────────────────────────────────────────

    fun addTransaction(transaction: Transaction) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.addTransaction(accountId, transaction) }
    }

    fun updateTransaction(transaction: Transaction) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.updateTransaction(accountId, transaction) }
    }

    fun removeTransaction(transaction: Transaction) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.removeTransaction(accountId, transaction) }
    }

    fun validateTransaction(transaction: Transaction) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.validateTransaction(accountId, transaction) }
    }

    // ─── Shortcut actions ────────────────────────────────────────────

    fun addShortcut(shortcut: WidgetShortcut) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.addShortcut(accountId, shortcut) }
    }

    fun updateShortcut(shortcut: WidgetShortcut) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.updateShortcut(accountId, shortcut) }
    }

    fun removeShortcut(shortcut: WidgetShortcut) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.removeShortcut(accountId, shortcut) }
    }

    /**
     * Exécute un raccourci : crée une transaction à partir du shortcut.
     */
    fun executeShortcut(shortcut: WidgetShortcut) {
        val signedAmount = when (shortcut.type) {
            com.finoria.app.data.model.TransactionType.EXPENSE -> -kotlin.math.abs(shortcut.amount)
            com.finoria.app.data.model.TransactionType.INCOME -> kotlin.math.abs(shortcut.amount)
        }
        addTransaction(
            Transaction(
                amount = signedAmount,
                comment = shortcut.comment,
                potentiel = false,
                date = java.time.LocalDate.now(),
                category = shortcut.category
            )
        )
    }

    // ─── Recurring actions ───────────────────────────────────────────

    fun addRecurring(recurring: RecurringTransaction) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.addRecurring(accountId, recurring) }
    }

    fun updateRecurring(recurring: RecurringTransaction) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.updateRecurring(accountId, recurring) }
    }

    fun removeRecurring(recurring: RecurringTransaction) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.removeRecurring(accountId, recurring) }
    }

    fun togglePauseRecurring(recurring: RecurringTransaction) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.togglePauseRecurring(accountId, recurring) }
    }

    // ─── Bulk operations ─────────────────────────────────────────────

    fun importTransactions(transactions: List<Transaction>) {
        val accountId = selectedAccountId.value ?: return
        viewModelScope.launch { repository.importTransactions(accountId, transactions) }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────

    fun processRecurringTransactions() {
        viewModelScope.launch { repository.processRecurrences() }
    }
}
