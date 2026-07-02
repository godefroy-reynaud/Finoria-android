package com.finoria.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.TransactionCategory
import java.util.UUID
import kotlinx.coroutines.launch

private const val COLUMNS = 5
private const val ROWS = 2
private const val ITEMS_PER_PAGE = COLUMNS * ROWS

/** Une tuile du sélecteur : catégorie par défaut, personnalisée, ou « + Ajouter ». */
private sealed interface PickerItem {
    data class Default(val category: TransactionCategory) : PickerItem
    data class Custom(val category: CustomCategory) : PickerItem
    data object Add : PickerItem
}

/**
 * Sélecteur de catégorie de transaction paginé.
 *
 * Pages de **5 colonnes × 2 lignes** (10 tuiles) parcourues par swipe horizontal
 * ([HorizontalPager]) avec indicateur de pages cliquable. Contenu, dans l'ordre :
 * les 32 catégories par défaut, puis les **catégories personnalisées** du compte
 * (tri alphabétique insensible à la casse/accents), puis la tuile **« + Ajouter »**.
 *
 * **Double sélection** (portage iOS) : une catégorie personnalisée est sélectionnée
 * quand [selectedCustomCategoryId] != null (et [selectedCategory] est alors `Autre`
 * par convention) ; sinon c'est la catégorie par défaut qui l'est.
 *
 * **Appui long** : sur une personnalisée → menu Modifier/Supprimer (avec alerte de
 * confirmation avant suppression) ; sur une par défaut → « Non modifiable » ; sur
 * « + Ajouter » → rien. Retour haptique au déclenchement.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionCategoryPicker(
    selectedCategory: TransactionCategory,
    selectedCustomCategoryId: UUID?,
    customCategories: List<CustomCategory>,
    onSelectDefault: (TransactionCategory) -> Unit,
    onSelectCustom: (CustomCategory) -> Unit,
    onAddCustom: () -> Unit,
    onEditCustom: (CustomCategory) -> Unit,
    onDeleteCustom: (CustomCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val items: List<PickerItem> = remember(customCategories) {
        buildList {
            TransactionCategory.entries.forEach { add(PickerItem.Default(it)) }
            customCategories
                .sortedBy { CustomCategory.normalizeName(it.name) }
                .forEach { add(PickerItem.Custom(it)) }
            add(PickerItem.Add)
        }
    }

    val pageCount = (items.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
    val selectedIndex = items.indexOfFirst { item ->
        when (item) {
            is PickerItem.Default ->
                selectedCustomCategoryId == null && item.category == selectedCategory
            is PickerItem.Custom -> item.category.id == selectedCustomCategoryId
            PickerItem.Add -> false
        }
    }
    val selectedPage = (selectedIndex.coerceAtLeast(0) / ITEMS_PER_PAGE)
        .coerceIn(0, pageCount - 1)

    val pagerState = rememberPagerState(initialPage = selectedPage) { pageCount }
    val scope = rememberCoroutineScope()

    // État des interactions long-press : menu contextuel ouvert + confirmation.
    var menuTarget by remember { mutableStateOf<PickerItem?>(null) }
    var customToDelete by remember { mutableStateOf<CustomCategory?>(null) }

    // Navigation auto : quand la sélection passe sur une autre page (ex. catégorie
    // devinée depuis le commentaire ou fraîchement créée), on s'y positionne.
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
            val pageItems = items.subList(start, minOf(start + ITEMS_PER_PAGE, items.size))
            PickerPage(
                items = pageItems,
                isSelected = { index -> start + index == selectedIndex },
                menuTarget = menuTarget,
                onTap = { item ->
                    when (item) {
                        is PickerItem.Default -> onSelectDefault(item.category)
                        is PickerItem.Custom -> onSelectCustom(item.category)
                        PickerItem.Add -> onAddCustom()
                    }
                },
                onLongPress = { item -> if (item != PickerItem.Add) menuTarget = item },
                onDismissMenu = { menuTarget = null },
                onEdit = { category ->
                    menuTarget = null
                    onEditCustom(category)
                },
                onDeleteRequest = { category ->
                    menuTarget = null
                    customToDelete = category
                }
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

    // Alerte de confirmation avant suppression (suppression = nullify : les
    // transactions qui l'utilisaient retombent sur « Autre »).
    customToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { customToDelete = null },
            title = { Text("Supprimer la catégorie ?") },
            text = {
                Text(
                    "« ${category.name} » sera définitivement supprimée. " +
                        "Les transactions qui l'utilisent retomberont sur « Autre »."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustom(category)
                        customToDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { customToDelete = null }) { Text("Annuler") }
            }
        )
    }
}

/**
 * Une page du sélecteur : grille fixe de [ROWS] × [COLUMNS]. Les cellules
 * manquantes (dernière page) sont laissées vides pour garder une hauteur
 * constante d'une page à l'autre (pas de « saut »).
 */
@Composable
private fun PickerPage(
    items: List<PickerItem>,
    isSelected: (Int) -> Boolean,
    menuTarget: PickerItem?,
    onTap: (PickerItem) -> Unit,
    onLongPress: (PickerItem) -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: (CustomCategory) -> Unit,
    onDeleteRequest: (CustomCategory) -> Unit
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
                        val item = items[index]
                        PickerTile(
                            item = item,
                            isSelected = isSelected(index),
                            menuOpen = menuTarget == item,
                            onTap = { onTap(item) },
                            onLongPress = { onLongPress(item) },
                            onDismissMenu = onDismissMenu,
                            onEdit = onEdit,
                            onDeleteRequest = onDeleteRequest,
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

/**
 * Tuile : cercle coloré + icône + libellé, anneau si sélectionnée. Ancre aussi le
 * menu contextuel d'appui long (Modifier/Supprimer ou « Non modifiable »).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickerTile(
    item: PickerItem,
    isSelected: Boolean,
    menuOpen: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: (CustomCategory) -> Unit,
    onDeleteRequest: (CustomCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    val icon = when (item) {
        is PickerItem.Default -> item.category.icon
        is PickerItem.Custom -> item.category.icon
        PickerItem.Add -> Icons.Default.Add
    }
    val color = when (item) {
        is PickerItem.Default -> item.category.color
        is PickerItem.Custom -> item.category.color
        PickerItem.Add -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when (item) {
        is PickerItem.Default -> item.category.label
        is PickerItem.Custom -> item.category.label
        PickerItem.Add -> "Ajouter"
    }

    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
                .padding(vertical = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (isSelected) 0.3f else 0.1f))
                    .then(
                        if (isSelected) Modifier.border(2.dp, color, CircleShape)
                        else Modifier
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) color
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
            when (item) {
                is PickerItem.Custom -> {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { onEdit(item.category) }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = { onDeleteRequest(item.category) }
                    )
                }
                is PickerItem.Default -> {
                    DropdownMenuItem(
                        text = { Text("Non modifiable") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        enabled = false,
                        onClick = {}
                    )
                }
                PickerItem.Add -> Unit
            }
        }
    }
}
