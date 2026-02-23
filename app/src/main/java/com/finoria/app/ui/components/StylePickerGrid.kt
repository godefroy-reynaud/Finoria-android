package com.finoria.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Grille de sélection générique pour les enums StylableEnum.
 * Utilisée pour AccountStyle et TransactionCategory.
 */
@Composable
fun <T> StylePickerGrid(
    selected: T,
    onSelect: (T) -> Unit,
    values: Array<T>,
    columns: Int = 4,
    modifier: Modifier = Modifier
) where T : Enum<T>, T : StylableEnum {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(values) { style ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(style) }
                    .padding(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            style.color.copy(
                                alpha = if (selected == style) 0.3f else 0.1f
                            )
                        )
                        .then(
                            if (selected == style)
                                Modifier.border(2.dp, style.color, CircleShape)
                            else Modifier
                        )
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = style.label,
                        tint = style.color
                    )
                }
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected == style) style.color
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
