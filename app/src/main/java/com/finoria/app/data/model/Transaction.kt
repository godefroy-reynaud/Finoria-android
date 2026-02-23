package com.finoria.app.data.model

import com.finoria.app.data.model.serializers.LocalDateSerializer
import com.finoria.app.data.model.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/**
 * Transaction financière (validée ou potentielle).
 * Immutable — utiliser copy() via validated() ou modified() pour créer des variantes.
 */
@Serializable
data class Transaction(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String = "",
    val potentiel: Boolean = true,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate? = null,
    val category: TransactionCategory = TransactionCategory.OTHER,
    @Serializable(with = UUIDSerializer::class)
    val recurringTransactionId: UUID? = null
) {
    /** Retourne une copie validée (non potentielle avec date) */
    fun validated(at: LocalDate = LocalDate.now()): Transaction =
        copy(potentiel = false, date = at)

    /** Retourne une copie modifiée */
    fun modified(
        amount: Double? = null,
        comment: String? = null,
        potentiel: Boolean? = null,
        date: LocalDate? = this.date,
        category: TransactionCategory? = null
    ): Transaction = copy(
        amount = amount ?: this.amount,
        comment = comment ?: this.comment,
        potentiel = potentiel ?: this.potentiel,
        date = date,
        category = category ?: this.category
    )
}
