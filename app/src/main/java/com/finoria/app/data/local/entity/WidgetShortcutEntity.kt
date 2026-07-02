package com.finoria.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.data.model.TransactionType

/**
 * Raccourci — enfant d'Account (cascade à la suppression du compte). La catégorie
 * personnalisée est en **nullify** (SET_NULL à la suppression de celle-ci).
 */
@Entity(
    tableName = "widget_shortcuts",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CustomCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["customCategoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("accountId"), Index("customCategoryId")],
)
data class WidgetShortcutEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amount: Double,
    val comment: String,
    val type: TransactionType,
    val category: TransactionCategory,
    val customCategoryId: String? = null,
)
