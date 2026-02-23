package com.finoria.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finoria.data.AppDataStore
import com.finoria.data.CsvService
import com.finoria.domain.RecurrenceEngine
import com.finoria.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class AppUiState(
    val accounts: List<Account> = emptyList(),
    val transactionsByAccount: Map<String, List<Transaction>> = emptyMap(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val shortcuts: List<WidgetShortcut> = emptyList(),
    val selectedAccountId: String? = null,
    val isLoading: Boolean = true,
    val toastMessage: String? = null
)

class AppViewModel(private val dataStore: AppDataStore) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            dataStore.appStateFlow.collect { state ->
                _uiState.update { 
                    it.copy(
                        accounts = state.accounts,
                        transactionsByAccount = state.transactionsByAccount,
                        recurringTransactions = state.recurringTransactions,
                        shortcuts = state.shortcuts,
                        selectedAccountId = state.selectedAccountId,
                        isLoading = false
                    )
                }
                processRecurrences()
            }
        }
    }

    private fun processRecurrences() {
        val currentState = getCurrentAppState()
        val updatedState = RecurrenceEngine.processAll(currentState)
        if (updatedState != currentState) {
            saveState(updatedState)
        }
    }

    // --- Actions ---

    fun selectAccount(accountId: String) {
        val newState = getCurrentAppState().copy(selectedAccountId = accountId)
        saveState(newState)
    }

    fun addAccount(account: Account) {
        val currentState = getCurrentAppState()
        val newAccounts = currentState.accounts + account
        val newState = currentState.copy(
            accounts = newAccounts,
            selectedAccountId = currentState.selectedAccountId ?: account.id.toString()
        )
        saveState(newState)
    }

    fun deleteAccount(account: Account) {
        val currentState = getCurrentAppState()
        val newAccounts = currentState.accounts.filter { it.id != account.id }
        val newSelectedId = if (currentState.selectedAccountId == account.id.toString()) {
            newAccounts.firstOrNull()?.id?.toString()
        } else {
            currentState.selectedAccountId
        }
        val newState = currentState.copy(
            accounts = newAccounts,
            selectedAccountId = newSelectedId
        )
        saveState(newState)
    }

    fun addTransaction(transaction: Transaction) {
        val currentState = getCurrentAppState()
        val accountId = currentState.selectedAccountId ?: return
        val accountTransactions = currentState.transactionsByAccount[accountId] ?: emptyList()
        val updatedMap = currentState.transactionsByAccount.toMutableMap()
        updatedMap[accountId] = accountTransactions + transaction
        saveState(currentState.copy(transactionsByAccount = updatedMap))
    }

    fun deleteTransaction(transaction: Transaction) {
        val currentState = getCurrentAppState()
        val accountId = currentState.selectedAccountId ?: return
        val accountTransactions = currentState.transactionsByAccount[accountId] ?: emptyList()
        val updatedMap = currentState.transactionsByAccount.toMutableMap()
        updatedMap[accountId] = accountTransactions.filter { it.id != transaction.id }
        saveState(currentState.copy(transactionsByAccount = updatedMap))
    }

    fun validateTransaction(transaction: Transaction) {
        val currentState = getCurrentAppState()
        val accountId = currentState.selectedAccountId ?: return
        val accountTransactions = currentState.transactionsByAccount[accountId] ?: emptyList()
        
        val updatedTransactions = accountTransactions.map {
            if (it.id == transaction.id) it.validated(java.time.LocalDate.now()) else it
        }
        
        val updatedMap = currentState.transactionsByAccount.toMutableMap()
        updatedMap[accountId] = updatedTransactions
        saveState(currentState.copy(transactionsByAccount = updatedMap))
    }

    fun addRecurringTransaction(recurring: RecurringTransaction) {
        val currentState = getCurrentAppState()
        val newState = currentState.copy(recurringTransactions = currentState.recurringTransactions + recurring)
        saveState(newState)
        processRecurrences()
    }

    fun deleteRecurringTransaction(id: UUID) {
        val currentState = getCurrentAppState()
        val accountId = currentState.selectedAccountId ?: return
        
        val accountTransactions = currentState.transactionsByAccount[accountId] ?: emptyList()
        val filteredTransactions = RecurrenceEngine.removePotentialTransactions(id, accountTransactions)
        
        val updatedMap = currentState.transactionsByAccount.toMutableMap()
        updatedMap[accountId] = filteredTransactions
        
        val newRecurring = currentState.recurringTransactions.filter { it.id != id }
        saveState(currentState.copy(
            recurringTransactions = newRecurring,
            transactionsByAccount = updatedMap
        ))
    }

    fun importTransactionsFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val importedTransactions = CsvService.importCsv(inputStream)
                    val currentState = getCurrentAppState()
                    val accountId = currentState.selectedAccountId ?: return@launch
                    val currentTransactions = currentState.transactionsByAccount[accountId] ?: emptyList()
                    val updatedMap = currentState.transactionsByAccount.toMutableMap()
                    updatedMap[accountId] = currentTransactions + importedTransactions
                    saveState(currentState.copy(transactionsByAccount = updatedMap))
                    showToast("${importedTransactions.size} transactions importées !")
                }
            } catch (e: Exception) {
                showToast("Erreur lors de l'importation.")
            }
        }
    }

    private fun getCurrentAppState(): AppState {
        val s = _uiState.value
        return AppState(
            accounts = s.accounts,
            recurringTransactions = s.recurringTransactions,
            shortcuts = s.shortcuts,
            transactionsByAccount = s.transactionsByAccount,
            selectedAccountId = s.selectedAccountId
        )
    }

    private fun saveState(state: AppState) {
        viewModelScope.launch {
            dataStore.saveAppState(state)
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }
}
