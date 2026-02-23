package com.finoria.model

import androidx.compose.ui.graphics.Color
import com.finoria.ui.utils.StylableEnum
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Account(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val detail: String,
    val style: AccountStyle
)

enum class AccountStyle(
    override val icon: String,
    @Serializable(with = ColorSerializer::class)
    override val color: Color,
    override val label: String
) : StylableEnum {
    BANK("account_balance", Color(0xFF1976D2), "Banque"),
    SAVINGS("savings", Color(0xFF388E3C), "Épargne"),
    INVESTMENT("trending_up", Color(0xFF7B1FA2), "Investissement"),
    CARD("credit_card", Color(0xFFC2185B), "Carte"),
    CASH("payments", Color(0xFFFBC02D), "Espèces"),
    PIGGY("savings", Color(0xFFE91E63), "Tirelire"),
    WALLET("account_balance_wallet", Color(0xFF5D4037), "Portefeuille"),
    BUSINESS("business_center", Color(0xFF455A64), "Professionnel")
}
