package com.finoria.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.TransactionCategory

/**
 * Sélecteur de catégorie de transaction, construit avec des composants Material 3
 * natifs : des [FilterChip] disposés dans un [FlowRow] qui reviennent à la ligne.
 *
 * C'est l'équivalent Android idiomatique du sélecteur paginé iOS : plutôt qu'une
 * grille paginée par swipe avec des points, on présente les 32 catégories par
 * défaut sous forme de puces filtrables. Chaque puce porte l'icône teintée de la
 * couleur de la catégorie ; la puce sélectionnée reprend cette couleur.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionCategoryPicker(
    selected: TransactionCategory,
    onSelect: (TransactionCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category.label) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.color,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.color.copy(alpha = 0.18f),
                    selectedLabelColor = category.color
                )
            )
        }
    }
}
