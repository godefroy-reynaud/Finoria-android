package com.finoria.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finoria.data.AppDataStore
import com.finoria.ui.components.ToastHost
import com.finoria.ui.screens.analyses.AnalysesScreen
import com.finoria.ui.screens.analyses.CategoryTransactionsScreen
import com.finoria.ui.screens.calendar.CalendarScreen
import com.finoria.ui.screens.calendar.TransactionsListScreen
import com.finoria.ui.screens.future.FutureScreen
import com.finoria.ui.screens.home.HomeScreen
import com.finoria.ui.screens.transaction.AddTransactionScreen
import com.finoria.ui.screens.recurring.AddRecurringTransactionScreen
import com.finoria.ui.screens.recurring.RecurringListScreen
import com.finoria.ui.screens.shortcut.AddShortcutScreen
import com.finoria.observeResumed
import com.finoria.viewmodel.AppViewModel
import com.finoria.viewmodel.AppViewModelFactory

const val CATEGORY_NAME_ARG = "categoryName"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val viewModel: AppViewModel = viewModel(
        factory = AppViewModelFactory(AppDataStore(context))
    )

    DisposableEffect(lifecycleOwner) {
        val removeObserver = lifecycleOwner.observeResumed { viewModel.onAppResumed() }
        onDispose { removeObserver() }
    }

    val uiState by viewModel.uiState.collectAsState()
    val toastMessage = uiState.toastMessage

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
            composable(Screen.Home.route) { 
                HomeScreen(viewModel = viewModel, navController = navController) 
            }
            
            composable(Screen.Analyses.route) {
                AnalysesScreen(viewModel = viewModel) { categoryName ->
                    navController.navigate("category_transactions/$categoryName")
                }
            }
            
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = viewModel) { year, month ->
                    navController.navigate("calendar_list/$year/$month")
                }
            }
            
            composable(Screen.Future.route) { 
                FutureScreen(viewModel = viewModel) 
            }

            // --- Formulaires et Détails ---
            
            composable("add_transaction") {
                AddTransactionScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable("add_recurring") {
                AddRecurringTransactionScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable("recurring_list") {
                RecurringListScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("add_shortcut") {
                AddShortcutScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(
                route = "edit_recurring/{recurringId}",
                arguments = listOf(navArgument("recurringId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recurringId = backStackEntry.arguments?.getString("recurringId")
                val recurring = recurringId?.let { id ->
                    uiState.recurringTransactions.find { it.id.toString() == id }
                }
                AddRecurringTransactionScreen(
                    viewModel = viewModel,
                    existingRecurring = recurring,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "edit_shortcut/{shortcutId}",
                arguments = listOf(navArgument("shortcutId") { type = NavType.StringType })
            ) { backStackEntry ->
                val shortcutId = backStackEntry.arguments?.getString("shortcutId")
                val shortcut = shortcutId?.let { id ->
                    uiState.shortcuts.find { it.id.toString() == id }
                }
                AddShortcutScreen(
                    viewModel = viewModel,
                    existingShortcut = shortcut,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("all_transactions") {
                com.finoria.ui.screens.calendar.AllTransactionsFullScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "calendar_month/{year}/{month}",
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: java.time.LocalDate.now().year
                val month = backStackEntry.arguments?.getInt("month") ?: java.time.LocalDate.now().monthValue
                TransactionsListScreen(
                    viewModel = viewModel,
                    year = year,
                    month = month,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "category_transactions/{$CATEGORY_NAME_ARG}",
                arguments = listOf(navArgument(CATEGORY_NAME_ARG) { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString(CATEGORY_NAME_ARG)
                if (categoryName != null) {
                    CategoryTransactionsScreen(
                        viewModel = viewModel,
                        categoryName = categoryName,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = "calendar_list/{year}/{month}",
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: 0
                val month = backStackEntry.arguments?.getInt("month") ?: 0
                TransactionsListScreen(
                    viewModel = viewModel,
                    year = year,
                    month = month,
                    onBack = { navController.popBackStack() }
                )
            }
        }
            ToastHost(
                message = toastMessage,
                onDismiss = { viewModel.clearToast() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
