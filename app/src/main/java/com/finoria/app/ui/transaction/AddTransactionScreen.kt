package com.finoria.app.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.data.model.TransactionType
import com.finoria.app.ui.components.CategorySelectionSection
import com.finoria.app.ui.components.CommentTextField
import com.finoria.app.ui.components.CurrencyTextField
import com.finoria.app.ui.components.FormDeleteButton
import com.finoria.app.ui.components.TransactionTypeSelector
import com.finoria.app.util.toAmountInput
import com.finoria.app.util.toEpochMillis
import com.finoria.app.util.toLocalDate
import com.finoria.app.viewmodel.MainViewModel
import java.time.LocalDate
import java.util.UUID

/**
 * Formulaire de création/édition d'une transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    transactionToEdit: Transaction? = null,
    onDismiss: () -> Unit
) {
    val isEdit = transactionToEdit != null

    var type by remember {
        mutableStateOf(
            if (transactionToEdit != null && transactionToEdit.amount >= 0)
                TransactionType.INCOME else TransactionType.EXPENSE
        )
    }
    var amountText by remember {
        mutableStateOf(transactionToEdit?.amount?.toAmountInput() ?: "")
    }
    var comment by remember { mutableStateOf(transactionToEdit?.comment ?: "") }
    var category by remember {
        mutableStateOf(transactionToEdit?.category ?: TransactionCategory.OTHER)
    }
    var customCategoryId by remember { mutableStateOf(transactionToEdit?.customCategoryId) }
    var isPotentiel by remember { mutableStateOf(transactionToEdit?.potentiel ?: false) }
    var manualCategory by remember { mutableStateOf(transactionToEdit != null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = transactionToEdit?.date?.toEpochMillis()
            ?: System.currentTimeMillis()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEdit) "Modifier la transaction" else "Nouvelle transaction")
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: return@TextButton
                            val selectedDate = if (!isPotentiel) {
                                datePickerState.selectedDateMillis?.toLocalDate()
                                    ?: LocalDate.now()
                            } else null

                            // Convention iOS : catégorie perso sélectionnée → la
                            // catégorie par défaut est forcée sur « Autre ».
                            val finalCategory = when {
                                customCategoryId != null -> TransactionCategory.OTHER
                                !manualCategory -> TransactionCategory.guessFrom(comment, type)
                                else -> category
                            }

                            val transaction = Transaction(
                                id = transactionToEdit?.id ?: UUID.randomUUID(),
                                amount = type.signed(amount),
                                comment = comment,
                                potentiel = isPotentiel,
                                date = selectedDate,
                                category = finalCategory,
                                recurringTransactionId = transactionToEdit?.recurringTransactionId,
                                customCategoryId = customCategoryId,
                                // Rattaché à une vraie catégorie → le libellé
                                // importé en attente n'a plus lieu d'être.
                                importedCategoryName = if (customCategoryId != null) null
                                else transactionToEdit?.importedCategoryName
                            )

                            if (isEdit) {
                                viewModel.updateTransaction(transaction)
                            } else {
                                viewModel.addTransaction(transaction)
                            }
                            onDismiss()
                        },
                        enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
                    ) {
                        Text(if (isEdit) "OK" else "Ajouter")
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
            TransactionTypeSelector(
                type = type,
                onTypeChange = { txType ->
                    type = txType
                    if (!manualCategory && customCategoryId == null) {
                        category = TransactionCategory.guessFrom(comment, txType)
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            CurrencyTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            CommentTextField(
                value = comment,
                onValueChange = {
                    comment = it
                    if (!manualCategory && customCategoryId == null) {
                        category = TransactionCategory.guessFrom(it, type)
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            CategorySelectionSection(
                viewModel = viewModel,
                selectedCategory = category,
                selectedCustomCategoryId = customCategoryId,
                onSelectionChange = { newCategory, newCustomId ->
                    category = newCategory
                    customCategoryId = newCustomId
                    manualCategory = true
                }
            )

            Spacer(Modifier.height(16.dp))

            // Potential toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaction potentielle")
                Switch(
                    checked = isPotentiel,
                    onCheckedChange = { isPotentiel = it }
                )
            }

            // Date picker (only if not potential)
            if (!isPotentiel) {
                Spacer(Modifier.height(8.dp))
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Delete button in edit mode
            if (isEdit) {
                Spacer(Modifier.height(16.dp))
                FormDeleteButton(
                    onClick = {
                        viewModel.removeTransaction(transactionToEdit!!)
                        onDismiss()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
