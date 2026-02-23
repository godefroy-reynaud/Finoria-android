package com.finoria.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

enum class TransactionType {
    INCOME, EXPENSE
}

@Serializable
data class Transaction(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String = "",
    val isPotential: Boolean = false,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate? = null,
    val category: TransactionCategory = TransactionCategory.OTHER,
    @Serializable(with = UUIDSerializer::class)
    val recurringTransactionId: UUID? = null
) {
    fun validated(atDate: LocalDate): Transaction = copy(
        isPotential = false,
        date = atDate
    )
}
