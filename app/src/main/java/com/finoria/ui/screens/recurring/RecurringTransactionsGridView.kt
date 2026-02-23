package com.finoria.ui.screens.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.model.RecurringTransaction
import com.finoria.ui.components.StyleIconView
import com.finoria.ui.utils.formattedCurrency
import com.finoria.viewmodel.AppViewModel
import java.util.Locale

@Composable
fun RecurringTransactionsGridView(
    viewModel: AppViewModel,
    onEditClick: (RecurringTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val recurringList = uiState.recurringTransactions

    if (recurringList.isEmpty()) {
        EmptyRecurringState(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recurringList) { recurring ->
                RecurringTransactionCard(
                    recurring = recurring,
                    onEdit = { onEditClick(recurring) },
                    onPauseResume = {
                        if (recurring.isPaused) {
                            viewModel.resumeRecurringTransaction(recurring.id)
                            viewModel.showToast("Récurrence reprise")
                        } else {
                            viewModel.pauseRecurringTransaction(recurring.id)
                            viewModel.showToast("Récurrence mise en pause")
                        }
                    },
                    onDelete = {
                        viewModel.deleteRecurringTransaction(recurring.id)
                        viewModel.showToast("Récurrence supprimée")
                    }
                )
            }
        }
    }
}

@Composable
private fun RecurringTransactionCard(
    recurring: RecurringTransaction,
    onEdit: () -> Unit,
    onPauseResume: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (recurring.isPaused) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyleIconView(style = recurring.category, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recurring.comment,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${recurring.amount.formattedCurrency()} / ${recurring.frequency.name.lowercase(Locale.getDefault())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (recurring.isPaused) {
                    Text(
                        "En pause",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier")
                }
                IconButton(onClick = onPauseResume) {
                    Icon(
                        if (recurring.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (recurring.isPaused) "Reprendre" else "Pause"
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la récurrence ?") },
            text = { Text("Les transactions futures générées par cette récurrence seront également supprimées.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun EmptyRecurringState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Aucune transaction récurrente",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Ajoutez des abonnements, loyers ou salaires récurrents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
