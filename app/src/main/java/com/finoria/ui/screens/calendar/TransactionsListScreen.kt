package com.finoria.ui.screens.calendar

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.finoria.viewmodel.AppViewModel
import com.finoria.domain.CalculationService
import java.time.LocalDate
import com.finoria.ui.utils.monthName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    viewModel: AppViewModel,
    year: Int,
    month: Int,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions = uiState.transactionsByAccount[uiState.selectedAccountId] ?: emptyList()

    val filteredTransactions = remember(transactions, year, month) {
        CalculationService.validatedTransactions(year, month, transactions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${LocalDate.of(year, month, 1).monthName()} $year",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        AllTransactionsView(filteredTransactions)
    }
}