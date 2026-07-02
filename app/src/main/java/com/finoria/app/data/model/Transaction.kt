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
    val recurringTransactionId: UUID? = null,
    /**
     * Référence optionnelle vers une catégorie personnalisée du compte.
     * Quand elle est non nulle, [category] vaut `OTHER` par convention et
     * l'affichage utilise le nom/icône/couleur de la catégorie personnalisée.
     */
    @Serializable(with = UUIDSerializer::class)
    val customCategoryId: UUID? = null,
    /**
     * Champ temporaire d'import CSV : libellé de catégorie inconnu, mémorisé à la
     * lecture puis résolu en vraie catégorie personnalisée au commit (remis à null).
     */
    val importedCategoryName: String? = null
) {
    /** Retourne une copie validée (non potentielle avec date) */
    fun validated(at: LocalDate = LocalDate.now()): Transaction =
        copy(potentiel = false, date = at)
}
