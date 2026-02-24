package com.finoria.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.ui.analyses.AnalysesTabScreen
import com.finoria.app.ui.analyses.CategoryTransactionsScreen
import com.finoria.app.ui.calendar.AllTransactionsScreen
import com.finoria.app.ui.calendar.CalendarTabScreen
import com.finoria.app.ui.calendar.MonthsScreen
import com.finoria.app.ui.calendar.TransactionsListScreen
import com.finoria.app.ui.future.FutureTabScreen
import com.finoria.app.ui.home.HomeTabScreen
import com.finoria.app.viewmodel.MainViewModel

/**
 * NavHost principal de l'application Finoria.
 */
@Composable
fun FinoriaNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    onShowAddTransaction: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onShowAccountPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // ─── Onglets principaux ──────────────────────────────────

        composable(Screen.Home.route) {
            HomeTabScreen(
                viewModel = viewModel,
                navController = navController,
                onShowAccountPicker = onShowAccountPicker,
                onEditTransaction = onEditTransaction
            )
        }

        composable(Screen.Analyses.route) {
            AnalysesTabScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable(Screen.Calendar.route) {
            CalendarTabScreen(
                viewModel = viewModel,
                navController = navController,
                onEditTransaction = onEditTransaction
            )
        }

        composable(Screen.Future.route) {
            FutureTabScreen(
                viewModel = viewModel,
                onEditTransaction = onEditTransaction
            )
        }

        // ─── Sous-écrans ─────────────────────────────────────────

        composable(Screen.AllTransactions.route) {
            AllTransactionsScreen(
                viewModel = viewModel,
                navController = navController,
                embedded = false,
                onEditTransaction = onEditTransaction
            )
        }

        composable(Screen.PotentialTransactions.route) {
            com.finoria.app.ui.future.PotentialTransactionsScreen(
                viewModel = viewModel,
                navController = navController,
                onEditTransaction = onEditTransaction
            )
        }

        // ─── Écrans paramétrés ───────────────────────────────────

        composable(
            route = Screen.TRANSACTIONS_LIST_ROUTE,
            arguments = listOf(
                navArgument("month") { type = NavType.IntType },
                navArgument("year") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val month = backStackEntry.arguments?.getInt("month") ?: return@composable
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            TransactionsListScreen(
                viewModel = viewModel,
                month = month,
                year = year,
                navController = navController,
                onEditTransaction = onEditTransaction
            )
        }

        composable(
            route = Screen.MONTHS_LIST_ROUTE,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            MonthsScreen(
                viewModel = viewModel,
                year = year,
                navController = navController
            )
        }

        composable(
            route = Screen.CATEGORY_TRANSACTIONS_ROUTE,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("month") { type = NavType.IntType },
                navArgument("year") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val categoryStr = backStackEntry.arguments?.getString("category") ?: return@composable
            val category = try {
                TransactionCategory.valueOf(categoryStr)
            } catch (_: Exception) {
                return@composable
            }
            val month = backStackEntry.arguments?.getInt("month") ?: return@composable
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            CategoryTransactionsScreen(
                viewModel = viewModel,
                category = category,
                month = month,
                year = year,
                navController = navController,
                onEditTransaction = onEditTransaction
            )
        }
    }
}
