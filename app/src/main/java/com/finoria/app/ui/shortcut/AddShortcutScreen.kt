package com.finoria.app.ui.shortcut

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.data.model.TransactionType
import com.finoria.app.data.model.WidgetShortcut
import com.finoria.app.ui.components.CurrencyTextField
import com.finoria.app.ui.components.StylePickerGrid
import com.finoria.app.viewmodel.MainViewModel
import kotlin.math.abs

/**
 * Formulaire de création/édition d'un raccourci.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShortcutScreen(
    viewModel: MainViewModel,
    shortcutToEdit: WidgetShortcut? = null,
    onDismiss: () -> Unit
) {
    val isEdit = shortcutToEdit != null

    var type by remember {
        mutableStateOf(shortcutToEdit?.type ?: TransactionType.EXPENSE)
    }
    var amountText by remember {
        mutableStateOf(
            shortcutToEdit?.let { String.format("%.2f", abs(it.amount)) } ?: ""
        )
    }
    var comment by remember { mutableStateOf(shortcutToEdit?.comment ?: "") }
    var category by remember {
        mutableStateOf(shortcutToEdit?.category ?: TransactionCategory.OTHER)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Modifier le raccourci" else "Nouveau raccourci") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: return@TextButton
                            val shortcut = WidgetShortcut(
                                id = shortcutToEdit?.id ?: java.util.UUID.randomUUID(),
                                amount = amount,
                                comment = comment,
                                type = type,
                                category = category
                            )
                            if (isEdit) viewModel.updateShortcut(shortcut)
                            else viewModel.addShortcut(shortcut)
                            onDismiss()
                        },
                        enabled = amountText.toDoubleOrNull() != null && comment.isNotBlank()
                    ) {
                        Text(if (isEdit) "OK" else "Créer")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, txType ->
                    SegmentedButton(
                        selected = type == txType,
                        onClick = { type = txType },
                        shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size)
                    ) { Text(txType.label) }
                }
            }

            Spacer(Modifier.height(16.dp))

            CurrencyTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 30) comment = it },
                label = { Text("Commentaire") },
                supportingText = { Text("${comment.length}/30") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text("Catégorie", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            StylePickerGrid(
                selected = category,
                onSelect = { category = it },
                values = TransactionCategory.entries.toTypedArray(),
                columns = 5
            )

            if (isEdit) {
                Spacer(Modifier.height(24.dp))
                TextButton(
                    onClick = {
                        viewModel.removeShortcut(shortcutToEdit!!)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Supprimer")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
