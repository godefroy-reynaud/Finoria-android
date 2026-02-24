package com.finoria.app.ui.calendar

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.finoria.app.data.model.Transaction
import com.finoria.app.viewmodel.MainViewModel

/**
 * Wrapper de l'onglet Calendrier.
 */
@Composable
fun CalendarTabScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onEditTransaction: (Transaction) -> Unit = {}
) {
    CalendarContentScreen(
        viewModel = viewModel,
        navController = navController,
        onEditTransaction = onEditTransaction
    )
}
