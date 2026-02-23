package com.finoria.ui.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.ui.utils.monthName
import java.time.LocalDate

@Composable
fun MonthsView(year: Int, onMonthClick: (Int) -> Unit) {
    val months = (1..12).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(months) { month ->
            val date = LocalDate.of(year, month, 1)
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onMonthClick(month) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = date.monthName(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}