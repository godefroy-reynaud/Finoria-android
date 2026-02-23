package com.finoria.app.ui.recurring

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.RecurringTransaction
import com.finoria.app.ui.components.StyleIconView
import com.finoria.app.util.formattedCurrency

/**
 * Grille 2 colonnes de transactions récurrentes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecurringGrid(
    recurrings: List<RecurringTransaction>,
    onEdit: (RecurringTransaction) -> Unit,
    onDelete: (RecurringTransaction) -> Unit,
    onTogglePause: (RecurringTransaction) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text("Récurrences", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(
                ((recurrings.size + 2) / 2 * 90).coerceAtLeast(90).dp
            )
        ) {
            items(recurrings, key = { it.id }) { recurring ->
                var showMenu by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onEdit(recurring) },
                            onLongClick = { showMenu = true }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (recurring.isPaused)
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StyleIconView(style = recurring.category, size = 28.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recurring.comment,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = recurring.amount.formattedCurrency(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recurring.frequency.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (recurring.isPaused) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "En pause",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.height(14.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            onClick = { showMenu = false; onEdit(recurring) }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (recurring.isPaused) "Reprendre" else "Mettre en pause")
                            },
                            leadingIcon = {
                                Icon(
                                    if (recurring.isPaused) Icons.Default.PlayArrow
                                    else Icons.Default.Pause,
                                    contentDescription = null
                                )
                            },
                            onClick = { showMenu = false; onTogglePause(recurring) }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete(recurring) }
                        )
                    }
                }
            }

            item {
                OutlinedCard(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
