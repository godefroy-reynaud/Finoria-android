package com.finoria.app.ui.shortcut

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.finoria.app.ui.components.CategorySelectionSection
import com.finoria.app.ui.components.CommentTextField
import com.finoria.app.ui.components.CurrencyTextField
import com.finoria.app.ui.components.FormDeleteButton
import com.finoria.app.ui.components.TransactionTypeSelector
import com.finoria.app.util.toAmountInput
import com.finoria.app.viewmodel.MainViewModel
import java.util.UUID

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
        mutableStateOf(shortcutToEdit?.amount?.toAmountInput() ?: "")
    }
    var comment by remember { mutableStateOf(shortcutToEdit?.comment ?: "") }
    var category by remember {
        mutableStateOf(shortcutToEdit?.category ?: TransactionCategory.OTHER)
    }
    var customCategoryId by remember { mutableStateOf(shortcutToEdit?.customCategoryId) }

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
                                id = shortcutToEdit?.id ?: UUID.randomUUID(),
                                amount = amount,
                                comment = comment,
                                type = type,
                                category = if (customCategoryId != null) {
                                    TransactionCategory.OTHER
                                } else category,
                                customCategoryId = customCategoryId
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
            TransactionTypeSelector(type = type, onTypeChange = { type = it })

            Spacer(Modifier.height(16.dp))

            CurrencyTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            CommentTextField(value = comment, onValueChange = { comment = it })

            Spacer(Modifier.height(16.dp))

            CategorySelectionSection(
                viewModel = viewModel,
                selectedCategory = category,
                selectedCustomCategoryId = customCategoryId,
                onSelectionChange = { newCategory, newCustomId ->
                    category = newCategory
                    customCategoryId = newCustomId
                }
            )

            if (isEdit) {
                Spacer(Modifier.height(24.dp))
                FormDeleteButton(
                    onClick = {
                        viewModel.removeShortcut(shortcutToEdit!!)
                        onDismiss()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
