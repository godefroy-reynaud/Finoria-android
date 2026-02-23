package com.finoria.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Update
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Définition des routes de navigation.
 */
sealed class Screen(val route: String) {
    // Onglets principaux
    data object Home : Screen("home")
    data object Analyses : Screen("analyses")
    data object Calendar : Screen("calendar")
    data object Future : Screen("future")

    // Sous-écrans
    data object AllTransactions : Screen("allTransactions")
    data object PotentialTransactions : Screen("potential")

    // Écrans avec arguments
    companion object {
        const val TRANSACTIONS_LIST_ROUTE = "transactions/{month}/{year}"
        const val MONTHS_LIST_ROUTE = "months/{year}"
        const val CATEGORY_TRANSACTIONS_ROUTE = "categoryTx/{category}/{month}/{year}"

        fun transactionsListRoute(month: Int, year: Int) = "transactions/$month/$year"
        fun monthsListRoute(year: Int) = "months/$year"
        fun categoryTransactionsRoute(category: String, month: Int, year: Int) =
            "categoryTx/$category/$month/$year"
    }
}

/**
 * Items de la barre de navigation inférieure.
 */
enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Accueil", Icons.Outlined.Home),
    ANALYSES("analyses", "Analyses", Icons.Outlined.PieChart),
    CALENDAR("calendar", "Calendrier", Icons.Outlined.CalendarMonth),
    FUTURE("future", "Futur", Icons.Outlined.Update)
}
