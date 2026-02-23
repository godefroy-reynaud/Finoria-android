package com.finoria.ui.screens.analyses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finoria.ui.components.StyleIconView
import com.finoria.ui.utils.formattedCurrency

@Composable
fun CategoryBreakdownRow(
    categoryData: CategoryData,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyleIconView(style = categoryData.category, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(categoryData.category.label, fontWeight = FontWeight.SemiBold)
                Text(
                    text = String.format("%.1f%%", categoryData.percentage * 100),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { categoryData.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color = categoryData.color
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = categoryData.amount.formattedCurrency(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}