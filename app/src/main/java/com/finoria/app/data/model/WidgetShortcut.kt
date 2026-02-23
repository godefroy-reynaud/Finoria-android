package com.finoria.app.data.model

import com.finoria.app.data.model.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Raccourci rapide pour ajouter une transaction fréquente en un tap.
 */
@Serializable
data class WidgetShortcut(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String,
    val type: TransactionType,
    val category: TransactionCategory = TransactionCategory.guessFrom(comment, type)
)
