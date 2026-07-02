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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.CustomCategoryIcons
import com.finoria.app.data.model.TransactionCategory
import java.util.UUID

/** Palette proposée (couleurs système iOS du portage, sans opacité). */
private val COLOR_PALETTE = listOf(
    "#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#00C7BE", "#30B0C7", "#32ADE6",
    "#007AFF", "#5856D6", "#AF52DE", "#FF2D55", "#A2845E", "#8E8E93",
)

private const val SYMBOL_COLUMNS = 6

/**
 * Sheet de création/édition d'une catégorie personnalisée.
 *
 * Formulaire du portage iOS : nom (max 15 caractères + compteur, capitalisation
 * par mot), couleur (palette, convertie en `#RRGGBB`), symbole (grille de
 * 6 colonnes, ~72 icônes) avec aperçu. Validation au « Valider » : nom obligatoire
 * et unique (comparaison **normalisée** : casse/accents) vis-à-vis des catégories
 * par défaut et des autres personnalisées du compte — sinon « Nom déjà utilisé. »
 * et la sheet reste ouverte.
 *
 * @param categoryToEdit null = mode création ; sinon la sheet est pré-remplie.
 * @param existingCategories catégories personnalisées du compte (contrôle doublons).
 * @param onSave appelé avec la catégorie validée (id conservé en édition).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCategorySheet(
    categoryToEdit: CustomCategory?,
    existingCategories: List<CustomCategory>,
    onSave: (CustomCategory) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = categoryToEdit != null

    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var colorHex by remember {
        mutableStateOf(categoryToEdit?.colorHex ?: CustomCategory.DEFAULT_COLOR_HEX)
    }
    var symbol by remember {
        mutableStateOf(categoryToEdit?.symbol ?: CustomCategory.DEFAULT_SYMBOL)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedColor = CustomCategory.parseHexColor(colorHex)

    fun validateAndSave() {
        val trimmed = name.trim()
        val key = CustomCategory.normalizeName(trimmed)
        errorMessage = when {
            trimmed.isEmpty() -> "Le nom est obligatoire."
            TransactionCategory.entries.any {
                CustomCategory.normalizeName(it.labelText) == key
            } -> "Nom déjà utilisé."
            existingCategories.any {
                it.id != categoryToEdit?.id && CustomCategory.normalizeName(it.name) == key
            } -> "Nom déjà utilisé."
            else -> null
        }
        if (errorMessage != null) return

        onSave(
            CustomCategory(
                id = categoryToEdit?.id ?: UUID.randomUUID(),
                name = trimmed,
                symbol = symbol,
                colorHex = colorHex,
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ─── Barre : Annuler / titre / Valider ───────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) { Text("Annuler") }
                Text(
                    text = if (isEdit) "Modifier la catégorie" else "Nouvelle catégorie",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                TextButton(onClick = ::validateAndSave) { Text("Valider") }
            }

            Spacer(Modifier.height(8.dp))

            // ─── Nom ─────────────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(CustomCategory.MAX_NAME_LENGTH)
                    errorMessage = null
                },
                label = { Text("Nom") },
                isError = errorMessage != null,
                supportingText = {
                    Text(errorMessage ?: "${name.length}/${CustomCategory.MAX_NAME_LENGTH}")
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ─── Couleur ─────────────────────────────────────────────
            Text("Couleur", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            COLOR_PALETTE.chunked(7).forEach { rowColors ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    rowColors.forEach { hex ->
                        val color = CustomCategory.parseHexColor(hex)
                        val isSelected = hex.equals(colorHex, ignoreCase = true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    } else Modifier
                                )
                                .clickable { colorHex = hex }
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── Symbole ─────────────────────────────────────────────
            Text("Symbole", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            // Aperçu : cercle de la couleur choisie + icône + nom du symbole.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(selectedColor.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = CustomCategoryIcons.iconFor(symbol),
                        contentDescription = null,
                        tint = selectedColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = name.ifBlank { "Aperçu" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = selectedColor
                )
            }

            Spacer(Modifier.height(12.dp))

            // Grille de 6 colonnes des ~72 symboles proposés.
            val symbolKeys = remember { CustomCategoryIcons.all.keys.toList() }
            symbolKeys.chunked(SYMBOL_COLUMNS).forEach { rowKeys ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isSelected = key == symbol
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) selectedColor.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, selectedColor, CircleShape)
                                    } else Modifier
                                )
                                .clickable { symbol = key }
                        ) {
                            Icon(
                                imageVector = CustomCategoryIcons.all.getValue(key),
                                contentDescription = key,
                                tint = if (isSelected) selectedColor
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    // Complète la dernière ligne pour garder 6 colonnes alignées.
                    repeat(SYMBOL_COLUMNS - rowKeys.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
