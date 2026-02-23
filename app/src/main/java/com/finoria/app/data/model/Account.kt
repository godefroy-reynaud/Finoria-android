package com.finoria.app.data.model

import com.finoria.app.data.model.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Compte bancaire de l'utilisateur.
 */
@Serializable
data class Account(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val detail: String = "",
    val style: AccountStyle = AccountStyle.guessFrom(name)
)
