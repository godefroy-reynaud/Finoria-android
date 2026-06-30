package com.finoria.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finoria.app.data.model.AccountStyle

/**
 * Entité racine. La suppression d'un compte supprime en **cascade** ses transactions,
 * récurrences et raccourcis (voir les ForeignKey des entités enfants).
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val detail: String,
    val style: AccountStyle,
)
