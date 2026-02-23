package com.finoria.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finoria.model.Transaction
import com.finoria.ui.components.TransactionRow
import com.finoria.ui.utils.dayHeaderFormatted
import java.time.LocalDate

@Composable
fun AllTransactionsView(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val groupedTransactions = remember(transactions) {
        transactions.filter { !it.isPotential && it.date != null }
            .groupBy { it.date!! }
            .toSortedMap(compareByDescending { it })
    }

    if (groupedTransactions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Aucune transaction", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            groupedTransactions.forEach { (date, dailyTransactions) ->
                item {
                    Text(
                        text = date.dayHeaderFormatted().uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(dailyTransactions) { transaction ->
                    TransactionRow(transaction)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
