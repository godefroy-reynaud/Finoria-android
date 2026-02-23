package com.finoria.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finoria.model.Transaction
import com.finoria.ui.utils.formattedCurrency

@Composable
fun TransactionRow(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyleIconView(style = transaction.category)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.comment, fontWeight = FontWeight.SemiBold)
            Text(transaction.category.label, fontSize = 12.sp, color = Color.Gray)
        }
        Text(
            text = transaction.amount.formattedCurrency(),
            color = if (transaction.amount < 0) Color.Red else Color(0xFF388E3C),
            fontWeight = FontWeight.Bold
        )
    }
}