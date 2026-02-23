package com.finoria.app.ui.analyses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.ui.components.TransactionRow
import com.finoria.app.util.dayHeaderFormatted
import com.finoria.app.util.monthName
import com.finoria.app.viewmodel.MainViewModel

/**
 * Transactions d'une catégorie donnée pour un mois donné,
 * groupées par jour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    viewModel: MainViewModel,
    category: TransactionCategory,
    month: Int,
    year: Int,
    navController: NavController
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()

    val filtered = viewModel.validatedTransactions(transactions, year, month)
        .filter { it.category == category }
        .sortedByDescending { it.date }

    val grouped = filtered.groupBy { it.date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${category.label} — ${monthName(month)} $year") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.forEach { (date, txns) ->
                item {
                    Text(
                        text = date?.dayHeaderFormatted() ?: "Sans date",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(txns, key = { it.id }) { tx ->
                    TransactionRow(transaction = tx)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
