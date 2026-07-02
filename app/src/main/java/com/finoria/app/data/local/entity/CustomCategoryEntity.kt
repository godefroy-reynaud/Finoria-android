package com.finoria.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Catégorie personnalisée — enfant d'Account (**cascade** à la suppression du
 * compte). Elle est la cible FK (**SET_NULL**) de transactions, raccourcis et
 * récurrences : la supprimer ne supprime pas ce qui l'utilisait, la référence
 * est simplement remise à null (retombée sur la catégorie par défaut `Autre`).
 */
@Entity(
    tableName = "custom_categories",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
)
data class CustomCategoryEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val symbol: String,
    val colorHex: String,
)
