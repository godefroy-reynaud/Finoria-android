package com.finoria.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finoria.data.AppDataStore
import com.finoria.ui.screens.analyses.AnalysesScreen
import com.finoria.ui.screens.analyses.CategoryTransactionsScreen
import com.finoria.ui.screens.calendar.CalendarScreen
import com.finoria.ui.screens.calendar.TransactionsListScreen
import com.finoria.ui.screens.future.FutureScreen
import com.finoria.ui.screens.home.HomeScreen
import com.finoria.ui.screens.transaction.AddTransactionScreen
import com.finoria.ui.screens.recurring.AddRecurringTransactionScreen
import com.finoria.viewmodel.AppViewModel
import com.finoria.viewmodel.AppViewModelFactory

const val CATEGORY_NAME_ARG = "categoryName"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val viewModel: AppViewModel = viewModel(
        factory = AppViewModelFactory(AppDataStore(context))
    )

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
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
    }
}
