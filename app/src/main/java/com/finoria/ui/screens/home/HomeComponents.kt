package com.finoria.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun BalanceHeader(
    totalBalance: Double,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .fillMaxWidth(),
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
fun QuickCard(
    title: String,
    amount: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Text(
                text = amount.formattedCurrency(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (amount >= 0) Color(0xFF388E3C) else Color.Red
            )
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutsGrid(
    shortcuts: List<WidgetShortcut>,
    onShortcutClick: (WidgetShortcut) -> Unit,
    onShortcutLongClick: ((WidgetShortcut) -> Unit)? = null
) {
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
                    .then(
                        if (onShortcutLongClick != null) {
                            Modifier.combinedClickable(
                                onClick = { onShortcutClick(shortcut) },
                                onLongClick = { onShortcutLongClick(shortcut) }
                            )
                        } else Modifier.clickable { onShortcutClick(shortcut) }
                    )
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
