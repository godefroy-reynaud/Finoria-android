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
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.ui.LocalCustomCategories
import com.finoria.app.ui.components.SwipeableTransactionRow
import com.finoria.app.util.dayHeaderFormatted
import com.finoria.app.util.monthName
import com.finoria.app.viewmodel.MainViewModel
import java.util.UUID

/**
 * Transactions d'une catégorie donnée pour un mois donné, groupées par jour.
 *
 * Soit [category] (catégorie par défaut), soit [customCategoryId] (catégorie
 * personnalisée) est fourni — cohérent avec les parts du camembert : la vue
 * d'une catégorie par défaut **exclut** les transactions rattachées à une
 * catégorie perso (elles ont leur propre part/écran).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    viewModel: MainViewModel,
    category: TransactionCategory?,
    customCategoryId: UUID? = null,
    month: Int,
    year: Int,
    navController: NavController,
    onEditTransaction: (Transaction) -> Unit = {}
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()
    val customCategories = LocalCustomCategories.current

    val filtered = viewModel.validatedTransactions(transactions, year, month)
        .filter { tx ->
            if (customCategoryId != null) {
                tx.customCategoryId == customCategoryId
            } else {
                tx.category == category &&
                    tx.customCategoryId?.let { it in customCategories } != true
            }
        }
        .sortedByDescending { it.date }

    val grouped = filtered.groupBy { it.date }

    val title = customCategoryId?.let { customCategories[it]?.name }
        ?: category?.label
        ?: "Catégorie"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title — ${monthName(month)} $year") },
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
                    SwipeableTransactionRow(
                        transaction = tx,
                        onEdit = onEditTransaction,
                        onDelete = { viewModel.removeTransaction(it) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
