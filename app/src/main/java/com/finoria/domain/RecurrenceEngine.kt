package com.finoria.domain

import com.finoria.model.AppState
import com.finoria.model.RecurrenceFrequency
import com.finoria.model.Transaction
import com.finoria.model.TransactionType
import java.time.LocalDate
import java.util.UUID

object RecurrenceEngine {
    fun processAll(state: AppState): AppState {
        val today = LocalDate.now()
        val nextMonth = today.plusMonths(1)
        
        val updatedTransactions = state.transactionsByAccount.toMutableMap()
        val updatedRecurring = state.recurringTransactions.map { recurring ->
            if (recurring.isPaused) return@map recurring

            val currentLastGenerated = recurring.lastGeneratedDate ?: recurring.startDate.minusDays(1)
            var tempLastGenerated = currentLastGenerated
            
            // On traite le compte sélectionné ou tous les comptes ? 
            // Pour la parité avec iOS, on utilise le compte sélectionné pour l'instant
            val accountId = state.selectedAccountId ?: return@map recurring
            val accountTransactions = updatedTransactions[accountId]?.toMutableList() ?: mutableListOf()

            var dateToProcess = currentLastGenerated.plusDays(1)
            while (dateToProcess.isBefore(nextMonth)) {
                val exists = accountTransactions.any { 
                    it.recurringTransactionId == recurring.id && it.date == dateToProcess 
                }

                if (!exists) {
                    val isPotential = dateToProcess.isAfter(today)
                    val newTransaction = Transaction(
                        id = UUID.randomUUID(),
                        amount = if (recurring.type == TransactionType.EXPENSE) -Math.abs(recurring.amount) else Math.abs(recurring.amount),
                        comment = recurring.comment,
                        isPotential = isPotential,
                        date = dateToProcess,
                        category = recurring.category,
                        recurringTransactionId = recurring.id
                    )
                    accountTransactions.add(newTransaction)
                }

                tempLastGenerated = dateToProcess
                dateToProcess = nextDate(dateToProcess, recurring.frequency)
            }
            
            updatedTransactions[accountId] = accountTransactions
            recurring.copy(lastGeneratedDate = if (tempLastGenerated.isAfter(currentLastGenerated)) tempLastGenerated else currentLastGenerated)
        }

        return state.copy(
            recurringTransactions = updatedRecurring,
            transactionsByAccount = updatedTransactions
        )
    }

    private fun nextDate(current: LocalDate, frequency: RecurrenceFrequency): LocalDate {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> current.plusDays(1)
            RecurrenceFrequency.WEEKLY -> current.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> current.plusMonths(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }

    fun removePotentialTransactions(recurringId: UUID, transactions: List<Transaction>): List<Transaction> {
        return transactions.filterNot { it.recurringTransactionId == recurringId && it.isPotential }
    }
}
