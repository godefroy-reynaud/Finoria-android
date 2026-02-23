package com.finoria.app.ui.future

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finoria.app.ui.components.TransactionRow
import com.finoria.app.ui.theme.IncomeGreen
import com.finoria.app.viewmodel.MainViewModel

/**
 * Écran des transactions potentielles (futures).
 * 2 sections : récurrentes et normales, avec SwipeToDismiss pour valider/supprimer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotentialTransactionsScreen(
    viewModel: MainViewModel
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()
    val potential = viewModel.potentialTransactions(transactions)

    val recurringPotential = potential.filter { it.recurringTransactionId != null }
    val normalPotential = potential.filter { it.recurringTransactionId == null }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Section: Recurring potential
        if (recurringPotential.isNotEmpty()) {
            item {
                SectionHeader("Transactions récurrentes")
            }
            items(recurringPotential, key = { it.id }) { tx ->
                val dismissState = rememberSwipeToDismissBoxState()

                LaunchedEffect(dismissState.currentValue) {
                    when (dismissState.currentValue) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            // Swipe right → validate
                            viewModel.validateTransaction(tx)
                        }
                        SwipeToDismissBoxValue.EndToStart -> {
                            // Swipe left → delete
                            viewModel.removeTransaction(tx)
                        }
                        SwipeToDismissBoxValue.Settled -> { /* no-op */ }
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        SwipeDismissBackground(dismissState.dismissDirection)
                    }
                ) {
                    TransactionRow(transaction = tx)
                }
            }
        }

        // Section: Normal potential
        if (normalPotential.isNotEmpty()) {
            item {
                SectionHeader("Futures")
            }
            items(normalPotential, key = { it.id }) { tx ->
                val dismissState = rememberSwipeToDismissBoxState()

                LaunchedEffect(dismissState.currentValue) {
                    when (dismissState.currentValue) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            viewModel.validateTransaction(tx)
                        }
                        SwipeToDismissBoxValue.EndToStart -> {
                            viewModel.removeTransaction(tx)
                        }
                        SwipeToDismissBoxValue.Settled -> { /* no-op */ }
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        SwipeDismissBackground(dismissState.dismissDirection)
                    }
                ) {
                    TransactionRow(transaction = tx)
                }
            }
        }

        // Empty state
        if (potential.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Aucune transaction future",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDismissBackground(direction: SwipeToDismissBoxValue) {
    val color by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> IncomeGreen
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
        },
        label = "swipeColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            else -> Alignment.CenterEnd
        }
    ) {
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Valider",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            SwipeToDismissBoxValue.EndToStart -> {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
            else -> {}
        }
    }
}
