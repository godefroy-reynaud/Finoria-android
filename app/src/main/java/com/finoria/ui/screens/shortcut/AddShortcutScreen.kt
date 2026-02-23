package com.finoria.ui.screens.shortcut

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.model.ShortcutStyle
import com.finoria.model.TransactionCategory
import com.finoria.model.WidgetShortcut
import com.finoria.ui.components.CurrencyTextField
import com.finoria.ui.components.StylePickerGrid
import com.finoria.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShortcutScreen(
    viewModel: AppViewModel,
    existingShortcut: WidgetShortcut? = null,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(existingShortcut?.name ?: "") }
    var amount by remember { mutableStateOf(existingShortcut?.amount ?: 0.0) }
    var category by remember { mutableStateOf(existingShortcut?.category ?: TransactionCategory.OTHER) }
    var selectedStyle by remember { mutableStateOf(existingShortcut?.style ?: ShortcutStyle.STANDARD) }

    LaunchedEffect(existingShortcut) {
        existingShortcut?.let {
            name = it.name
            amount = it.amount
            category = it.category
            selectedStyle = it.style
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingShortcut != null) "Modifier le raccourci" else "Nouveau raccourci",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du raccourci") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            CurrencyTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Montant",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Text("Catégorie", style = MaterialTheme.typography.labelLarge)
            StylePickerGrid(
                items = TransactionCategory.entries,
                selectedItem = category,
                onItemSelected = { category = it }
            )

            Spacer(Modifier.height(24.dp))
            Text("Style", style = MaterialTheme.typography.labelLarge)
            StylePickerGrid(
                items = ShortcutStyle.entries,
                selectedItem = selectedStyle,
                onItemSelected = { selectedStyle = it }
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isNotBlank() && amount != 0.0) {
                        if (existingShortcut != null) {
                            viewModel.updateShortcut(
                                existingShortcut.copy(
                                    name = name,
                                    amount = amount,
                                    category = category,
                                    style = selectedStyle
                                )
                            )
                        } else {
                            viewModel.addShortcut(
                                WidgetShortcut(
                                    name = name,
                                    amount = amount,
                                    category = category,
                                    style = selectedStyle
                                )
                            )
                        }
                        viewModel.showToast(
                            if (existingShortcut != null) "Raccourci modifié" else "Raccourci ajouté"
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && amount != 0.0
            ) {
                Text(if (existingShortcut != null) "Enregistrer" else "Ajouter")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
