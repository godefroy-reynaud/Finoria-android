package com.finoria.app.data.model

import com.finoria.app.data.model.serializers.LocalDateSerializer
import com.finoria.app.data.model.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/**
 * Transaction récurrente automatique.
 */
@Serializable
data class RecurringTransaction(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String = "",
    val type: TransactionType,
    val category: TransactionCategory = TransactionCategory.guessFrom(comment, type),
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate = LocalDate.now(),
    @Serializable(with = LocalDateSerializer::class)
    val lastGeneratedDate: LocalDate? = null,
    val isPaused: Boolean = false
) {
    /**
     * Calcul des dates d'occurrence entre [from] et [to].
     */
    fun occurrences(from: LocalDate, to: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(to)) {
            if (!current.isBefore(from)) {
                dates.add(current)
            }
            current = when (frequency) {
                RecurrenceFrequency.DAILY -> current.plusDays(1)
                RecurrenceFrequency.WEEKLY -> current.plusWeeks(1)
                RecurrenceFrequency.MONTHLY -> current.plusMonths(1)
                RecurrenceFrequency.YEARLY -> current.plusYears(1)
            }
        }
        return dates
    }

    /**
     * Retourne les transactions en attente (non encore générées).
     */
    fun pendingTransactions(): List<Pair<LocalDate, Transaction>> {
        if (isPaused) return emptyList()
        val from = lastGeneratedDate?.plusDays(1) ?: startDate
        val to = LocalDate.now().plusMonths(1)
        return occurrences(from, to).map { date ->
            val signedAmount = if (type == TransactionType.EXPENSE) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
            date to Transaction(
                amount = signedAmount,
                comment = comment,
                potentiel = date.isAfter(LocalDate.now()),
                date = date,
                category = category,
                recurringTransactionId = id
            )
        }
    }
}
