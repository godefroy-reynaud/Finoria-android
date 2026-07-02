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
    val isPaused: Boolean = false,
    /** Catégorie personnalisée optionnelle (nullify à la suppression de celle-ci). */
    @Serializable(with = UUIDSerializer::class)
    val customCategoryId: UUID? = null
) {
    /**
     * Date de la n-ième occurrence, **ancrée sur [startDate]**.
     *
     * L'index est toujours appliqué à partir de [startDate] (jamais chaîné depuis
     * l'occurrence précédente). C'est essentiel pour les mois courts : un loyer du
     * 31 reste au 31, simplement clampé quand le mois est plus court
     * (31 janv → 28 févr → 31 mars → 30 avr → 31 mai). Chaîner `plusMonths(1)`
     * depuis une date déjà clampée provoquerait une dérive permanente (→ 28 partout).
     */
    fun occurrenceDate(index: Int): LocalDate = when (frequency) {
        RecurrenceFrequency.DAILY -> startDate.plusDays(index.toLong())
        RecurrenceFrequency.WEEKLY -> startDate.plusWeeks(index.toLong())
        RecurrenceFrequency.MONTHLY -> startDate.plusMonths(index.toLong())
        RecurrenceFrequency.YEARLY -> startDate.plusYears(index.toLong())
    }

    /**
     * Calcul des dates d'occurrence entre [from] et [to] (bornes incluses),
     * ancrées sur [startDate] via [occurrenceDate].
     */
    fun occurrences(from: LocalDate, to: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var index = 0
        var current = occurrenceDate(index)
        while (!current.isAfter(to)) {
            if (!current.isBefore(from)) {
                dates.add(current)
            }
            index++
            current = occurrenceDate(index)
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
                recurringTransactionId = id,
                customCategoryId = customCategoryId
            )
        }
    }
}
