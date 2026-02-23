package com.finoria.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finoria.ui.utils.StylableEnum

@Composable
fun <T : StylableEnum> StylePickerGrid(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 50.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 300.dp)
    ) {
        items(items) { item ->
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (item == selectedItem) 3.dp else 0.dp,
                        color = if (item == selectedItem) Color.Black else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onItemSelected(item) }
            ) {
                StyleIconView(style = item, modifier = Modifier.fillMaxSize())
            }
        }
    }
}