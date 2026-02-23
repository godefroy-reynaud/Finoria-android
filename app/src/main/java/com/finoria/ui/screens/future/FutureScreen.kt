package com.finoria.ui.screens.future

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.ui.components.TransactionRow
import com.finoria.viewmodel.AppViewModel
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions = uiState.transactionsByAccount[uiState.selectedAccountId] ?: emptyList()

    // On filtre uniquement les transactions potentielles
    val potentialTransactions = transactions.filter { it.isPotential }
        .sortedBy { it.date }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("À venir", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (potentialTransactions.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune transaction prévue", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                items(potentialTransactions) { transaction ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TransactionRow(transaction = transaction)
                            }
                            IconButton(onClick = { viewModel.validateTransaction(transaction) }) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Valider",
                                    tint = Color(0xFF388E3C)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTransaction(transaction) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}