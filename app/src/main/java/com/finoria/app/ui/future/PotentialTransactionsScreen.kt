package com.finoria.app.ui.future

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.finoria.app.data.model.Transaction
import com.finoria.app.ui.components.SwipeableTransactionRow
import com.finoria.app.viewmodel.MainViewModel

/**
 * Écran des transactions potentielles (futures).
 * 2 sections : récurrentes et normales, avec SwipeToDismiss pour valider/supprimer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotentialTransactionsScreen(
    viewModel: MainViewModel,
    navController: NavController? = null,
    onEditTransaction: (Transaction) -> Unit = {},
    embedded: Boolean = navController == null
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()
    val potential = viewModel.potentialTransactions(transactions)

    val recurringPotential = potential.filter { it.recurringTransactionId != null }
    val normalPotential = potential.filter { it.recurringTransactionId == null }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        LazyColumn(
            modifier = modifier
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
                    SwipeableTransactionRow(
                        transaction = tx,
                        onEdit = onEditTransaction,
                        onDelete = { viewModel.removeTransaction(it) },
                        onValidate = { viewModel.validateTransaction(it) }
                    )
                }
            }

            // Section: Normal potential
            if (normalPotential.isNotEmpty()) {
                item {
                    SectionHeader("Futures")
                }
                items(normalPotential, key = { it.id }) { tx ->
                    SwipeableTransactionRow(
                        transaction = tx,
                        onEdit = onEditTransaction,
                        onDelete = { viewModel.removeTransaction(it) },
                        onValidate = { viewModel.validateTransaction(it) }
                    )
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

    if (!embedded && navController != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Transactions futures") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            content(Modifier.padding(padding))
        }
    } else {
        content(Modifier)
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
