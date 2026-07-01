package com.finoria.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.finoria.app.ui.components.StylableEnum
import kotlinx.serialization.Serializable

/**
 * Catégorie de transaction par défaut (fixe, non modifiable).
 *
 * Les 32 catégories, leur ordre d'affichage, leurs libellés et leurs couleurs
 * suivent le portage iOS → Android. Le `name` Kotlin est la **clé stable** stockée
 * en base (via Room) et ne doit pas être renommé une fois publié. Le `labelText`
 * est le libellé affiché **et** écrit dans la colonne « Catégorie » du CSV.
 */
@Serializable
enum class TransactionCategory(
    val iconName: String,
    val colorValue: Long,
    val labelText: String
) : StylableEnum {
    INCOME("arrow_circle_down", CategoryColors.GREEN, "Revenu"),
    EXPENSE("arrow_circle_up", CategoryColors.RED, "Dépense"),
    SALARY("business_center", CategoryColors.GREEN, "Salaire"),
    FREELANCE("laptop", CategoryColors.TEAL, "Freelance"),
    BONUS("star", CategoryColors.YELLOW, "Prime"),
    RENT("home", CategoryColors.ORANGE, "Loyer"),
    UTILITIES("bolt", CategoryColors.YELLOW, "Charges"),
    HOME("handyman", CategoryColors.BROWN, "Maison"),
    SUBSCRIPTION("subscriptions", CategoryColors.PURPLE, "Abonnement"),
    PHONE("smartphone", CategoryColors.INDIGO, "Téléphone"),
    INSURANCE("shield", CategoryColors.BLUE, "Assurance"),
    FOOD("restaurant", CategoryColors.ORANGE, "Restaurant"),
    GROCERY("shopping_cart", CategoryColors.GREEN, "Courses"),
    COFFEE("local_cafe", CategoryColors.BROWN, "Café"),
    FUEL("local_gas_station", CategoryColors.ORANGE, "Carburant"),
    TRANSPORT("directions_bus", CategoryColors.CYAN, "Transport"),
    CAR("directions_car", CategoryColors.BLUE, "Voiture"),
    LOAN("percent", CategoryColors.RED, "Crédit"),
    SAVINGS("payments", CategoryColors.MINT, "Épargne"),
    INVESTMENT("show_chart", CategoryColors.PURPLE, "Investissement"),
    TAX("description", CategoryColors.RED, "Impôts"),
    SHOPPING("shopping_bag", CategoryColors.PINK, "Shopping"),
    PARTY("favorite", CategoryColors.PINK, "Soirée"),
    SPORT("directions_run", CategoryColors.ORANGE, "Sport"),
    TRAVEL("flight", CategoryColors.CYAN, "Voyage"),
    CULTURE("theater_comedy", CategoryColors.INDIGO, "Culture"),
    FAMILY("people", CategoryColors.PURPLE, "Famille"),
    HEALTH("medical_services", CategoryColors.MINT, "Santé"),
    GIFT("card_giftcard", CategoryColors.INDIGO, "Cadeau"),
    EDUCATION("school", CategoryColors.BLUE, "Éducation"),
    PET("pets", CategoryColors.BROWN, "Animaux"),
    OTHER("more_horiz", CategoryColors.GRAY, "Autre");

    override val icon: ImageVector
        get() = when (this) {
            INCOME -> Icons.Outlined.ArrowCircleDown
            EXPENSE -> Icons.Outlined.ArrowCircleUp
            SALARY -> Icons.Outlined.BusinessCenter
            FREELANCE -> Icons.Outlined.Laptop
            BONUS -> Icons.Outlined.Star
            RENT -> Icons.Outlined.Home
            UTILITIES -> Icons.Outlined.Bolt
            HOME -> Icons.Outlined.Handyman
            SUBSCRIPTION -> Icons.Outlined.Subscriptions
            PHONE -> Icons.Outlined.Smartphone
            INSURANCE -> Icons.Outlined.Shield
            FOOD -> Icons.Outlined.Restaurant
            GROCERY -> Icons.Outlined.ShoppingCart
            COFFEE -> Icons.Outlined.LocalCafe
            FUEL -> Icons.Outlined.LocalGasStation
            TRANSPORT -> Icons.Outlined.DirectionsBus
            CAR -> Icons.Outlined.DirectionsCar
            LOAN -> Icons.Outlined.Percent
            SAVINGS -> Icons.Outlined.Payments
            INVESTMENT -> Icons.AutoMirrored.Outlined.ShowChart
            TAX -> Icons.Outlined.Description
            SHOPPING -> Icons.Outlined.ShoppingBag
            PARTY -> Icons.Outlined.Favorite
            SPORT -> Icons.AutoMirrored.Outlined.DirectionsRun
            TRAVEL -> Icons.Outlined.Flight
            CULTURE -> Icons.Outlined.TheaterComedy
            FAMILY -> Icons.Outlined.People
            HEALTH -> Icons.Outlined.MedicalServices
            GIFT -> Icons.Outlined.CardGiftcard
            EDUCATION -> Icons.Outlined.School
            PET -> Icons.Outlined.Pets
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
                text.containsAny("loyer", "appartement") -> RENT
                text.containsAny("maison", "travaux", "bricolage") -> HOME
                text.containsAny("salaire", "paie", "travail") -> SALARY
                text.containsAny("freelance", "prestation", "mission") -> FREELANCE
                text.containsAny("prime", "bonus") -> BONUS
                text.containsAny("netflix", "spotify", "abonnement", "abo") -> SUBSCRIPTION
                text.containsAny("assurance", "mutuelle") -> INSURANCE
                text.containsAny("crédit", "prêt", "emprunt") -> LOAN
                text.containsAny("impôt", "impot", "taxe", "fisc") -> TAX
                text.containsAny("edf", "eau", "gaz", "électricité", "charge") -> UTILITIES
                text.containsAny("épargne", "livret", "économie") -> SAVINGS
                text.containsAny("invest", "bourse", "action", "crypto", "pea") -> INVESTMENT
                text.containsAny("téléphone", "internet", "mobile", "forfait") -> PHONE
                text.containsAny("carburant", "essence", "gasoil") -> FUEL
                text.containsAny("course", "supermarché", "magasin", "leclerc", "carrefour", "lidl") -> GROCERY
                text.containsAny("café", "starbucks", "coffee") -> COFFEE
                text.containsAny("shopping", "vêtement", "habit") -> SHOPPING
                text.containsAny("maman", "papa", "famille") -> FAMILY
                text.containsAny("soirée", "bar", "fête") -> PARTY
                text.containsAny("sport", "salle", "gym", "fitness") -> SPORT
                text.containsAny("resto", "restaurant", "repas") -> FOOD
                text.containsAny("voiture", "auto", "garage", "parking") -> CAR
                text.containsAny("transport", "train", "taxi", "uber", "bus", "métro") -> TRANSPORT
                text.containsAny("voyage", "vacances", "hôtel", "avion") -> TRAVEL
                text.containsAny("cinéma", "musée", "concert", "culture") -> CULTURE
                text.containsAny("médecin", "pharmacie", "santé") -> HEALTH
                text.containsAny("école", "études", "cours", "formation") -> EDUCATION
                text.containsAny("animal", "chien", "chat", "vétérinaire") -> PET
                text.containsAny("cadeau", "anniversaire") -> GIFT
                else -> if (type == TransactionType.INCOME) INCOME else EXPENSE
            }
        }
    }
}

private fun String.containsAny(vararg terms: String): Boolean =
    terms.any { this.contains(it) }
