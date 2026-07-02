package com.finoria.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.TransactionCategory
import kotlinx.coroutines.launch

private const val COLUMNS = 5
private const val ROWS = 2
private const val ITEMS_PER_PAGE = COLUMNS * ROWS

/**
 * Sélecteur de catégorie de transaction paginé.
 *
 * Équivalent Android du `TabView` iOS en mode page : les 32 catégories par défaut
 * sont réparties en pages de **5 colonnes × 2 lignes** (10 tuiles) parcourues par
 * **swipe horizontal** via le [HorizontalPager] natif de Compose. Un indicateur de
 * pages (points) sous la grille signale qu'il reste des catégories et permet de
 * sauter directement à une page.
 */
@Composable
fun TransactionCategoryPicker(
    selected: TransactionCategory,
    onSelect: (TransactionCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = TransactionCategory.entries
    val pageCount = (categories.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
    val selectedPage = (categories.indexOf(selected) / ITEMS_PER_PAGE).coerceIn(0, pageCount - 1)

    val pagerState = rememberPagerState(initialPage = selectedPage) { pageCount }
    val scope = rememberCoroutineScope()

    // Navigation auto : quand la sélection passe sur une autre page (ex. catégorie
    // devinée depuis le commentaire), on s'y positionne avec animation.
    LaunchedEffect(selectedPage) {
        if (selectedPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val start = page * ITEMS_PER_PAGE
            val pageItems = categories.subList(start, minOf(start + ITEMS_PER_PAGE, categories.size))
            CategoryPage(
                items = pageItems,
                selected = selected,
                onSelect = onSelect
            )
        }

        if (pageCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                repeat(pageCount) { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                    )
                }
            }
        }
    }
}

/**
 * Une page du sélecteur : une grille fixe de [ROWS] lignes × [COLUMNS] colonnes.
 * Les cellules manquantes (dernière page) sont laissées vides pour garder une
 * hauteur constante d'une page à l'autre (pas de « saut »).
 */
@Composable
private fun CategoryPage(
    items: List<TransactionCategory>,
    selected: TransactionCategory,
    onSelect: (TransactionCategory) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(ROWS) { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(COLUMNS) { col ->
                    val index = row * COLUMNS + col
                    if (index < items.size) {
                        val category = items[index]
                        CategoryTile(
                            category = category,
                            isSelected = category == selected,
                            onClick = { onSelect(category) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Tuile d'une catégorie : cercle coloré + icône + libellé, anneau si sélectionnée. */
@Composable
private fun CategoryTile(
    category: TransactionCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(category.color.copy(alpha = if (isSelected) 0.3f else 0.1f))
                .then(
                    if (isSelected) Modifier.border(2.dp, category.color, CircleShape)
                    else Modifier
                )
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.label,
                tint = category.color
            )
        }
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) category.color
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
