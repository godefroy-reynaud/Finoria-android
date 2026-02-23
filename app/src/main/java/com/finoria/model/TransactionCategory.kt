package com.finoria.model

import androidx.compose.ui.graphics.Color
import com.finoria.ui.utils.StylableEnum
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionCategory(
    override val icon: String,
    @Serializable(with = ColorSerializer::class)
    override val color: Color,
    override val label: String
) : StylableEnum {
    SALARY("payments", Color(0xFF4CAF50), "Salaire"),
    INCOME("add_chart", Color(0xFF81C784), "Revenu"),
    RENT("home", Color(0xFFF44336), "Loyer"),
    UTILITIES("bolt", Color(0xFFFF9800), "Charges"),
    SUBSCRIPTION("subscriptions", Color(0xFFE91E63), "Abonnement"),
    PHONE("smartphone", Color(0xFF9C27B0), "Téléphone"),
    INSURANCE("shield", Color(0xFF607D8B), "Assurance"),
    FOOD("restaurant", Color(0xFFFFC107), "Alimentation"),
    SHOPPING("shopping_bag", Color(0xFFE91E63), "Shopping"),
    FUEL("local_gas_station", Color(0xFFFF5722), "Carburant"),
    TRANSPORT("directions_bus", Color(0xFF03A9F4), "Transport"),
    LOAN("account_balance", Color(0xFF795548), "Prêt"),
    SAVINGS("savings", Color(0xFF00BCD4), "Épargne"),
    FAMILY("family_restroom", Color(0xFFFF4081), "Famille"),
    HEALTH("medical_services", Color(0xFFF44336), "Santé"),
    GIFT("redeem", Color(0xFFFF5252), "Cadeau"),
    PARTY("celebration", Color(0xFF9C27B0), "Loisirs"),
    EXPENSE("receipt_long", Color(0xFF9E9E9E), "Dépense"),
    OTHER("more_horiz", Color(0xFF607D8B), "Autre")
}
