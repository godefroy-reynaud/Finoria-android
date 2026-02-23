package com.finoria.ui.screens.analyses

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.finoria.model.TransactionCategory
import com.finoria.ui.components.TransactionRow
import com.finoria.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    viewModel: AppViewModel,
    categoryName: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions = uiState.transactionsByAccount[uiState.selectedAccountId] ?: emptyList()

    val category = remember { TransactionCategory.valueOf(categoryName) }

    val filteredTransactions = remember(transactions, category) {
        transactions.filter { !it.isPotential && it.category == category }
            .sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.label, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(filteredTransactions) { transaction ->
                TransactionRow(transaction)
            }
        }
    }
}