package com.finoria.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.data.model.TransactionType
import com.finoria.app.viewmodel.MainViewModel
import java.util.UUID

/**
 * Briques partagées par les trois formulaires (transaction, raccourci,
 * récurrence) : sélecteur de type, champ commentaire, section catégorie
 * (picker + sheet de catégorie personnalisée) et bouton de suppression.
 */

private const val MAX_COMMENT_LENGTH = 30

/** Boutons segmentés Dépense / Revenu. */
@Composable
fun TransactionTypeSelector(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        TransactionType.entries.forEachIndexed { index, txType ->
            SegmentedButton(
                selected = type == txType,
                onClick = { onTypeChange(txType) },
                shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size)
            ) {
                Text(txType.label)
            }
        }
    }
}

/** Champ commentaire limité à [MAX_COMMENT_LENGTH] caractères, avec compteur. */
@Composable
fun CommentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= MAX_COMMENT_LENGTH) onValueChange(it) },
        label = { Text("Commentaire") },
        supportingText = { Text("${value.length}/$MAX_COMMENT_LENGTH") },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Section « Catégorie » complète : titre + [TransactionCategoryPicker] +
 * [CustomCategorySheet] (création/édition), avec le CRUD des catégories
 * personnalisées branché sur [viewModel].
 *
 * Toute la double sélection transite par [onSelectionChange] :
 * - catégorie par défaut → `(category, null)` ;
 * - catégorie personnalisée → `(OTHER, id)` (convention du portage iOS) ;
 * - suppression de la catégorie sélectionnée → retombe sur `(OTHER, null)`.
 */
@Composable
fun CategorySelectionSection(
    viewModel: MainViewModel,
    selectedCategory: TransactionCategory,
    selectedCustomCategoryId: UUID?,
    onSelectionChange: (TransactionCategory, UUID?) -> Unit
) {
    val customCategories by viewModel.currentCustomCategories.collectAsStateWithLifecycle()
    var showCategorySheet by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CustomCategory?>(null) }

    Text("Catégorie", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))

    TransactionCategoryPicker(
        selectedCategory = selectedCategory,
        selectedCustomCategoryId = selectedCustomCategoryId,
        customCategories = customCategories,
        onSelectDefault = { onSelectionChange(it, null) },
        onSelectCustom = { onSelectionChange(TransactionCategory.OTHER, it.id) },
        onAddCustom = {
            categoryToEdit = null
            showCategorySheet = true
        },
        onEditCustom = {
            categoryToEdit = it
            showCategorySheet = true
        },
        onDeleteCustom = { deleted ->
            viewModel.removeCustomCategory(deleted)
            // La sélection courante retombe sur « Autre ».
            if (selectedCustomCategoryId == deleted.id) {
                onSelectionChange(TransactionCategory.OTHER, null)
            }
        }
    )

    if (showCategorySheet) {
        CustomCategorySheet(
            categoryToEdit = categoryToEdit,
            existingCategories = customCategories,
            onSave = { saved ->
                if (categoryToEdit == null) viewModel.addCustomCategory(saved)
                else viewModel.updateCustomCategory(saved)
                // La catégorie créée/éditée devient la sélection courante.
                onSelectionChange(TransactionCategory.OTHER, saved.id)
                showCategorySheet = false
                categoryToEdit = null
            },
            onDismiss = {
                showCategorySheet = false
                categoryToEdit = null
            }
        )
    }
}

/** Bouton « Supprimer » rouge des formulaires en mode édition. */
@Composable
fun FormDeleteButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(Icons.Default.Delete, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text("Supprimer")
    }
}
