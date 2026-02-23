package com.finoria.app.ui.calendar

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
import com.finoria.app.data.model.Transaction
import com.finoria.app.ui.components.TransactionRow
import com.finoria.app.util.dayHeaderFormatted
import com.finoria.app.viewmodel.MainViewModel

/**
 * Toutes les transactions validées, groupées par jour, triées décroissant.
 * Peut être affiché en mode embedded (dans CalendarContentScreen) ou standalone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    viewModel: MainViewModel,
    navController: NavController,
    embedded: Boolean = false
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()

    val validated = viewModel.validatedTransactions(transactions)
        .sortedByDescending { it.date }

    val grouped = validated.groupBy { it.date }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (validated.isEmpty()) {
                item {
                    Text(
                        text = "Aucune transaction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
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

    if (embedded) {
        content(Modifier)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Toutes les transactions") },
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
    }
}
