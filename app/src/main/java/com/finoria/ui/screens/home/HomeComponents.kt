package com.finoria.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finoria.model.Account
import com.finoria.model.WidgetShortcut
import com.finoria.ui.components.StyleIconView
import com.finoria.ui.utils.formattedCurrency

@Composable
fun BalanceHeader(totalBalance: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Solde Total",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Text(
            text = totalBalance.formattedCurrency(),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            ),
            color = if (totalBalance >= 0) Color(0xFF388E3C) else Color.Red
        )
    }
}

@Composable
fun AccountCard(account: Account, balance: Double, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(account.style.color)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(account.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(account.detail, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
        Text(
            text = balance.formattedCurrency(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun ShortcutsGrid(shortcuts: List<WidgetShortcut>, onShortcutClick: (WidgetShortcut) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.heightIn(max = 200.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(shortcuts) { shortcut ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onShortcutClick(shortcut) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyleIconView(style = shortcut.style, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(shortcut.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    Text(shortcut.amount.formattedCurrency(), fontSize = 12.sp)
                }
            }
        }
    }
}
