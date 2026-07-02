package com.finoria.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finoria.app.data.model.TransactionCategory
import java.time.LocalDate

/**
 * Transaction — enfant d'Account (**cascade**), liée à une récurrence source et à
 * une catégorie personnalisée (**nullify** : les supprimer conserve la transaction
 * en remettant la référence à null).
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecurringTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceRecurringId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CustomCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["customCategoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("accountId"), Index("sourceRecurringId"), Index("customCategoryId")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amount: Double,
    val comment: String,
    val potentiel: Boolean,
    val date: LocalDate?,
    val category: TransactionCategory,
    val sourceRecurringId: String?,
    val customCategoryId: String? = null,
    /** Libellé CSV inconnu en attente de résolution (voir import CSV). */
    val importedCategoryName: String? = null,
)
