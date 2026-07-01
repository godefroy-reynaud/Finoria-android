package com.finoria.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.finoria.app.ui.components.StylableEnum
import kotlinx.serialization.Serializable

/**
 * Style visuel d'un compte (icône, couleur, label). Fixe, non modifiable.
 *
 * Les 10 styles suivent le portage iOS → Android. Le `name` Kotlin est la **clé
 * stable** stockée en base (via Room) et ne doit pas être renommé une fois publié.
 */
@Serializable
enum class AccountStyle(
    val iconName: String,
    val colorValue: Long,
    val labelText: String
) : StylableEnum {
    BANK("account_balance", CategoryColors.BLUE, "Courant"),
    SAVINGS("payments", CategoryColors.ORANGE, "Épargne"),
    INVESTMENT("show_chart", CategoryColors.PURPLE, "Investissement"),
    BUSINESS("business_center", CategoryColors.INDIGO, "Professionnel"),
    TRAVEL("flight", CategoryColors.TEAL, "Voyage"),
    GROCERY("shopping_cart", CategoryColors.GREEN, "Courses"),
    STUDENT("school", CategoryColors.CYAN, "Étudiant"),
    FAMILY("people", CategoryColors.PINK, "Famille"),
    PROPERTY("home", CategoryColors.BROWN, "Immobilier"),
    ENTERTAINMENT("sports_esports", CategoryColors.RED, "Loisirs");

    override val icon: ImageVector
        get() = when (this) {
            BANK -> Icons.Outlined.AccountBalance
            SAVINGS -> Icons.Outlined.Payments
            INVESTMENT -> Icons.AutoMirrored.Outlined.ShowChart
            BUSINESS -> Icons.Outlined.BusinessCenter
            TRAVEL -> Icons.Outlined.Flight
            GROCERY -> Icons.Outlined.ShoppingCart
            STUDENT -> Icons.Outlined.School
            FAMILY -> Icons.Outlined.People
            PROPERTY -> Icons.Outlined.Home
            ENTERTAINMENT -> Icons.Outlined.SportsEsports
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
                text.containsAny("livret", "épargne", "ldd", "pel", "économie") -> SAVINGS
                text.containsAny("invest", "pea", "crypto", "bourse", "action") -> INVESTMENT
                text.containsAny("pro", "entreprise", "business", "auto-entrepreneur") -> BUSINESS
                text.containsAny("voyage", "vacances", "travel") -> TRAVEL
                text.containsAny("course", "alimentation", "supermarché") -> GROCERY
                text.containsAny("étudiant", "student", "école", "université") -> STUDENT
                text.containsAny("famille", "enfant", "commun") -> FAMILY
                text.containsAny("immobilier", "immo", "loyer", "appartement") -> PROPERTY
                text.containsAny("loisir", "jeu", "gaming", "divertissement") -> ENTERTAINMENT
                else -> BANK
            }
        }
    }
}

private fun String.containsAny(vararg terms: String): Boolean =
    terms.any { this.contains(it) }
