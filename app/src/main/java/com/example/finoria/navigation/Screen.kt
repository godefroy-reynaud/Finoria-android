package com.example.finoria.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object Profile : Screen("profile", "Profil", Icons.Default.Person)
    object Settings : Screen("settings", "Paramètres", Icons.Default.Settings)
}

val items = listOf(
    Screen.Home,
    Screen.Profile,
    Screen.Settings
)
