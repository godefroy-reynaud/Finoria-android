package com.finoria.app.domain.service

import com.finoria.app.data.model.Account
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionManager
import java.time.LocalDate
import java.util.UUID

/**
 * Moteur de récurrences — génère les transactions automatiques.
 * Même logique que RecurrenceEngine.swift.
 */
object RecurrenceEngine {

    /**
     * Traite toutes les récurrences pour tous les comptes.
     * Retourne true si des modifications ont été apportées.
     */
    fun processAll(
        accounts: List<Account>,
        managers: Map<UUID, TransactionManager>
    ): Boolean {
        val today = LocalDate.now()
        val nextMonth = today.plusMonths(1)
        var modified = false

        for (account in accounts) {
            val manager = managers[account.id] ?: continue

            for (i in manager.recurringTransactions.indices) {
                val recurring = manager.recurringTransactions[i]
                if (recurring.isPaused) continue

                val lastGenerated = recurring.lastGeneratedDate
                var latestGenerated = lastGenerated

                // Les occurrences sont ancrées sur startDate (index-based via
                // occurrenceDate), jamais chaînées depuis l'occurrence précédente :
                // cela préserve le jour du mois pour les mois courts (loyer du 31).
                var index = 0
                var dateToProcess = recurring.occurrenceDate(index)

                while (!dateToProcess.isAfter(nextMonth)) {
                    val alreadyGenerated =
                        lastGenerated != null && !dateToProcess.isAfter(lastGenerated)

                    if (!alreadyGenerated) {
                        val exists = manager.transactions.any {
                            it.recurringTransactionId == recurring.id && it.date == dateToProcess
                        }

                        if (!exists) {
                            val isPotential = dateToProcess.isAfter(today)

                            manager.transactions.add(
                                Transaction(
                                    amount = recurring.type.signed(recurring.amount),
                                    comment = recurring.comment,
                                    potentiel = isPotential,
                                    date = dateToProcess,
                                    category = recurring.category,
                                    recurringTransactionId = recurring.id,
                                    // Propage la catégorie perso de la récurrence, sinon
                                    // la transaction générée retombe sur « Autre ». Si la
                                    // catégorie perso est ensuite supprimée, la FK SET_NULL
                                    // remet ce champ à null → bascule sur « Autre ».
                                    customCategoryId = recurring.customCategoryId,
                                )
                            )
                            modified = true
                        }

                        if (latestGenerated == null || dateToProcess.isAfter(latestGenerated)) {
                            latestGenerated = dateToProcess
                        }
                    }

                    index++
                    dateToProcess = recurring.occurrenceDate(index)
                }

                if (latestGenerated != null && latestGenerated != lastGenerated) {
                    manager.recurringTransactions[i] =
                        recurring.copy(lastGeneratedDate = latestGenerated)
                    modified = true
                }
            }
        }

        return modified
    }
}
