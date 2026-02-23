package com.finoria.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.finoria.app.ui.components.StylableEnum
import kotlinx.serialization.Serializable

/**
 * Catégorie de transaction avec auto-détection depuis le commentaire.
 */
@Serializable
enum class TransactionCategory(
    val iconName: String,
    val colorValue: Long,
    val labelText: String
) : StylableEnum {
    SALARY("business_center", 0xFF4CAF50, "Salaire"),
    INCOME("arrow_circle_down", 0xFF4CAF50, "Revenu"),
    RENT("home", 0xFFFF9800, "Loyer"),
    UTILITIES("bolt", 0xFFFFEB3B, "Charges"),
    SUBSCRIPTION("play_arrow", 0xFF9C27B0, "Abonnement"),
    PHONE("phone_android", 0xFF3F51B5, "Téléphone"),
    INSURANCE("shield", 0xFF2196F3, "Assurance"),
    FOOD("restaurant", 0xFFFFEB3B, "Restaurant"),
    SHOPPING("shopping_cart", 0xFF2196F3, "Courses"),
    FUEL("local_gas_station", 0xFFFF9800, "Carburant"),
    TRANSPORT("directions_car", 0xFF00BCD4, "Transport"),
    LOAN("percent", 0xFFF44336, "Crédit"),
    SAVINGS("savings", 0xFF26A69A, "Épargne"),
    FAMILY("person", 0xFF9C27B0, "Famille"),
    HEALTH("local_hospital", 0xFF26A69A, "Santé"),
    GIFT("card_giftcard", 0xFF3F51B5, "Cadeau"),
    PARTY("favorite", 0xFFE91E63, "Soirée"),
    EXPENSE("arrow_circle_up", 0xFFF44336, "Dépense"),
    OTHER("more_horiz", 0xFF9E9E9E, "Autre");

    override val icon: ImageVector
        get() = when (this) {
            SALARY -> Icons.Outlined.BusinessCenter
            INCOME -> Icons.Outlined.ArrowCircleDown
            RENT -> Icons.Outlined.Home
            UTILITIES -> Icons.Outlined.Bolt
            SUBSCRIPTION -> Icons.Outlined.PlayArrow
            PHONE -> Icons.Outlined.PhoneAndroid
            INSURANCE -> Icons.Outlined.Shield
            FOOD -> Icons.Outlined.Restaurant
            SHOPPING -> Icons.Outlined.ShoppingCart
            FUEL -> Icons.Outlined.LocalGasStation
            TRANSPORT -> Icons.Outlined.DirectionsCar
            LOAN -> Icons.Outlined.Percent
            SAVINGS -> Icons.Outlined.Savings
            FAMILY -> Icons.Outlined.Person
            HEALTH -> Icons.Outlined.LocalHospital
            GIFT -> Icons.Outlined.CardGiftcard
            PARTY -> Icons.Outlined.Favorite
            EXPENSE -> Icons.Outlined.ArrowCircleUp
            OTHER -> Icons.Outlined.MoreHoriz
        }

    override val color: Color
        get() = Color(colorValue)

    override val label: String
        get() = labelText

    companion object {
        fun guessFrom(comment: String, type: TransactionType): TransactionCategory {
            val text = comment.lowercase()
            return when {
                text.containsAny("loyer", "appartement", "maison") -> RENT
                text.containsAny("salaire", "paie", "travail") -> SALARY
                text.containsAny("netflix", "spotify", "abonnement", "abo") -> SUBSCRIPTION
                text.containsAny("assurance", "mutuelle") -> INSURANCE
                text.containsAny("crédit", "prêt", "emprunt") -> LOAN
                text.containsAny("edf", "eau", "gaz", "électricité", "charge") -> UTILITIES
                text.containsAny("épargne", "livret", "économie") -> SAVINGS
                text.containsAny("téléphone", "internet", "mobile", "forfait") -> PHONE
                text.containsAny("carburant", "essence", "gasoil") -> FUEL
                text.containsAny("course", "supermarché", "magasin") -> SHOPPING
                text.containsAny("maman", "papa", "famille") -> FAMILY
                text.containsAny("soirée", "bar", "fête") -> PARTY
                text.containsAny("resto", "restaurant", "repas") -> FOOD
                text.containsAny("voiture", "transport", "train", "taxi", "uber", "bus") -> TRANSPORT
                text.containsAny("médecin", "pharmacie", "santé") -> HEALTH
                text.containsAny("cadeau", "anniversaire") -> GIFT
                else -> if (type == TransactionType.INCOME) INCOME else EXPENSE
            }
        }
    }
}

private fun String.containsAny(vararg terms: String): Boolean =
    terms.any { this.contains(it) }
