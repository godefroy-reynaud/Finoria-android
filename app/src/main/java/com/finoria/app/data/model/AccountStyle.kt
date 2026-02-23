package com.finoria.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.finoria.app.ui.components.StylableEnum
import kotlinx.serialization.Serializable

/**
 * Style visuel d'un compte (icône, couleur, label).
 * Utilisé dans AccountCard et StylePickerGrid.
 */
@Serializable
enum class AccountStyle(
    val iconName: String,
    val colorValue: Long,
    val labelText: String
) : StylableEnum {
    BANK("account_balance", 0xFF2196F3, "Compte courant"),
    SAVINGS("savings", 0xFFFF9800, "Épargne"),
    INVESTMENT("show_chart", 0xFF9C27B0, "Investissements"),
    CARD("credit_card", 0xFF4CAF50, "Carte"),
    CASH("payments", 0xFF00BCD4, "Espèces"),
    PIGGY("card_giftcard", 0xFFE91E63, "Tirelire"),
    WALLET("account_balance_wallet", 0xFF795548, "Portefeuille"),
    BUSINESS("business_center", 0xFF3F51B5, "Professionnel");

    override val icon: ImageVector
        get() = when (this) {
            BANK -> Icons.Outlined.AccountBalance
            SAVINGS -> Icons.Outlined.Savings
            INVESTMENT -> Icons.AutoMirrored.Outlined.ShowChart
            CARD -> Icons.Outlined.CreditCard
            CASH -> Icons.Outlined.Payments
            PIGGY -> Icons.Outlined.CardGiftcard
            WALLET -> Icons.Outlined.AccountBalanceWallet
            BUSINESS -> Icons.Outlined.BusinessCenter
        }

    override val color: Color
        get() = Color(colorValue)

    override val label: String
        get() = labelText

    companion object {
        fun guessFrom(name: String): AccountStyle {
            val text = name.lowercase()
            return when {
                text.containsAny("courant", "principal", "bnp", "société générale", "crédit") -> BANK
                text.containsAny("livret", "épargne", "ldd", "pel") -> SAVINGS
                text.containsAny("invest", "pea", "crypto", "bourse", "action") -> INVESTMENT
                text.containsAny("carte", "revolut", "n26", "lydia") -> CARD
                text.containsAny("espèce", "cash", "liquide") -> CASH
                text.containsAny("tirelire", "économie") -> PIGGY
                text.containsAny("portefeuille", "wallet") -> WALLET
                text.containsAny("pro", "entreprise", "business") -> BUSINESS
                else -> BANK
            }
        }
    }
}

private fun String.containsAny(vararg terms: String): Boolean =
    terms.any { this.contains(it) }
