package com.finoria.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object Analyses : Screen("analyses", "Analyses", Icons.Default.PieChart)
    object Calendar : Screen("calendar", "Calendrier", Icons.Default.DateRange)
    object Future : Screen("future", "Futur", Icons.Default.Update)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Analyses,
    Screen.Calendar,
    Screen.Future
)