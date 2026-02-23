package com.finoria.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

enum class RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

@Serializable
data class RecurringTransaction(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String = "",
    val type: TransactionType,
    val category: TransactionCategory,
    val frequency: RecurrenceFrequency,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val lastGeneratedDate: LocalDate? = null,
    val isPaused: Boolean = false
)
