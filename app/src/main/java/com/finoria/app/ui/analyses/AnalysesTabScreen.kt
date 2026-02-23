package com.finoria.app.ui.analyses

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.finoria.app.viewmodel.MainViewModel

/**
 * Wrapper de l'onglet Analyses.
 * Contient directement l'AnalysesScreen.
 */
@Composable
fun AnalysesTabScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    AnalysesScreen(
        viewModel = viewModel,
        navController = navController
    )
}
