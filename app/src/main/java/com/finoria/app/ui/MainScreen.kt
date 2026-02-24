package com.finoria.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finoria.app.data.model.Account
import com.finoria.app.data.model.Transaction
import com.finoria.app.navigation.BottomNavItem
import com.finoria.app.navigation.FinoriaNavHost
import com.finoria.app.navigation.Screen
import com.finoria.app.ui.account.AccountPickerSheet
import com.finoria.app.ui.account.AddAccountSheet
import com.finoria.app.ui.transaction.AddTransactionScreen
import com.finoria.app.viewmodel.MainViewModel

/**
 * CompositionLocal pour le SnackbarHostState — remplace les toasts iOS.
 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState not provided")
}

/**
 * Écran principal de l'application.
 * Scaffold + NavigationBar (4 items) + FAB + SnackbarHost + NavHost.
 * Gère l'affichage des modales (ajout transaction, sélection compte, ajout compte).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()

    // ─── Modal state ─────────────────────────────────────────────
    var showAddTransaction by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showAddAccount by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    // Navigation bottom bar state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar on main tabs
    val isMainTab = BottomNavItem.entries.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    // Process recurring on resume (equivalent to iOS scenePhase == .active)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.processRecurringTransactions()
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = isMainTab,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NavigationBar {
                        BottomNavItem.entries.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true

                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isMainTab && selectedAccountId != null) {
                    FloatingActionButton(
                        onClick = { showAddTransaction = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter une transaction")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            FinoriaNavHost(
                navController = navController,
                viewModel = viewModel,
                onShowAddTransaction = { showAddTransaction = true },
                onEditTransaction = { transactionToEdit = it },
                onShowAccountPicker = { showAccountPicker = true },
                modifier = Modifier.padding(padding)
            )
        }

        // ─── Add/Edit Transaction Sheet ──────────────────────────────
        if (showAddTransaction || transactionToEdit != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddTransaction = false
                    transactionToEdit = null
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    transactionToEdit = transactionToEdit,
                    onDismiss = {
                        showAddTransaction = false
                        transactionToEdit = null
                    }
                )
            }
        }

        // ─── Account Picker Sheet ────────────────────────────────────
        if (showAccountPicker) {
            val accounts by viewModel.accounts.collectAsStateWithLifecycle()
            AccountPickerSheet(
                viewModel = viewModel,
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onDismiss = { showAccountPicker = false },
                onAddAccount = {
                    showAccountPicker = false
                    accountToEdit = null
                    showAddAccount = true
                },
                onEditAccount = { account ->
                    showAccountPicker = false
                    accountToEdit = account
                    showAddAccount = true
                }
            )
        }

        // ─── Add/Edit Account Sheet ──────────────────────────────────
        if (showAddAccount || accountToEdit != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddAccount = false
                    accountToEdit = null
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AddAccountSheet(
                    viewModel = viewModel,
                    accountToEdit = accountToEdit,
                    onDismiss = {
                        showAddAccount = false
                        accountToEdit = null
                    }
                )
            }
        }
    }
}
