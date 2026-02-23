package com.finoria.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.finoria.app.data.model.RecurringTransaction
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.WidgetShortcut
import com.finoria.app.navigation.Screen
import com.finoria.app.ui.recurring.RecurringGrid
import com.finoria.app.ui.shortcut.ShortcutsGrid
import com.finoria.app.viewmodel.MainViewModel
import java.time.LocalDate

/**
 * Écran d'accueil : LazyColumn avec BalanceHeader + QuickCards + ShortcutsGrid + RecurringGrid.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onEditTransaction: (Transaction) -> Unit,
    onEditShortcut: (WidgetShortcut) -> Unit,
    onAddShortcut: () -> Unit,
    onEditRecurring: (RecurringTransaction) -> Unit,
    onAddRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()
    val shortcuts by viewModel.currentShortcuts.collectAsStateWithLifecycle()
    val recurrings by viewModel.currentRecurring.collectAsStateWithLifecycle()

    val totalCurrent = viewModel.totalNonPotential(transactions)
    val totalPotential = viewModel.totalPotential(transactions)
    val percentageChange = viewModel.monthlyChangePercentage(transactions)

    val now = LocalDate.now()
    val currentMonthTotal = viewModel.totalForMonth(now.monthValue, now.year, transactions)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance header
        item {
            BalanceHeader(
                accountName = account?.name,
                totalCurrent = totalCurrent,
                percentageChange = percentageChange,
                onClick = { navController.navigate(Screen.AllTransactions.route) }
            )
        }

        // Quick cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickCard(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Solde du mois",
                    value = currentMonthTotal,
                    onClick = {
                        navController.navigate(
                            Screen.transactionsListRoute(now.monthValue, now.year)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickCard(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "À venir",
                    value = totalPotential,
                    onClick = {
                        navController.navigate(Screen.PotentialTransactions.route)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Shortcuts grid
        item {
            ShortcutsGrid(
                shortcuts = shortcuts,
                onTap = { shortcut -> viewModel.executeShortcut(shortcut) },
                onEdit = onEditShortcut,
                onDelete = { shortcut -> viewModel.removeShortcut(shortcut) },
                onAdd = onAddShortcut
            )
        }

        // Recurring grid
        item {
            RecurringGrid(
                recurrings = recurrings,
                onEdit = onEditRecurring,
                onDelete = { recurring -> viewModel.removeRecurring(recurring) },
                onTogglePause = { recurring -> viewModel.togglePauseRecurring(recurring) },
                onAdd = onAddRecurring
            )
        }

        // Bottom spacer for FAB clearance
        item { Spacer(Modifier.height(80.dp)) }
    }
}
