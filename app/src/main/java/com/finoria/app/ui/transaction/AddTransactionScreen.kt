package com.finoria.app.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.finoria.app.ui.components.CurrencyTextField
import com.finoria.app.ui.components.StylePickerGrid
import com.finoria.app.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

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
        mutableStateOf(
            transactionToEdit?.let { String.format("%.2f", abs(it.amount)) } ?: ""
        )
    }
    var comment by remember { mutableStateOf(transactionToEdit?.comment ?: "") }
    var category by remember {
        mutableStateOf(transactionToEdit?.category ?: TransactionCategory.OTHER)
    }
    var isPotentiel by remember { mutableStateOf(transactionToEdit?.potentiel ?: true) }
    var manualCategory by remember { mutableStateOf(transactionToEdit != null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = transactionToEdit?.date?.let {
            it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } ?: System.currentTimeMillis()
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
                            val signedAmount = if (type == TransactionType.EXPENSE) -abs(amount) else abs(amount)
                            val selectedDate = if (!isPotentiel) {
                                datePickerState.selectedDateMillis?.let {
                                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                } ?: LocalDate.now()
                            } else null

                            val finalCategory = if (!manualCategory) {
                                TransactionCategory.guessFrom(comment, type)
                            } else category

                            val transaction = Transaction(
                                id = transactionToEdit?.id ?: java.util.UUID.randomUUID(),
                                amount = signedAmount,
                                comment = comment,
                                potentiel = isPotentiel,
                                date = selectedDate,
                                category = finalCategory,
                                recurringTransactionId = transactionToEdit?.recurringTransactionId
                            )

                            if (isEdit) {
                                viewModel.updateTransaction(transaction)
                            } else {
                                viewModel.addTransaction(transaction)
                            }
                            onDismiss()
                        },
                        enabled = amountText.toDoubleOrNull() != null && amountText.toDoubleOrNull()!! > 0
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
            // Type segmented buttons
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, txType ->
                    SegmentedButton(
                        selected = type == txType,
                        onClick = {
                            type = txType
                            if (!manualCategory) {
                                category = TransactionCategory.guessFrom(comment, txType)
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransactionType.entries.size
                        )
                    ) {
                        Text(txType.label)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Amount
            CurrencyTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Comment
            OutlinedTextField(
                value = comment,
                onValueChange = {
                    if (it.length <= 30) {
                        comment = it
                        if (!manualCategory) {
                            category = TransactionCategory.guessFrom(it, type)
                        }
                    }
                },
                label = { Text("Commentaire") },
                supportingText = { Text("${comment.length}/30") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Category
            Text("Catégorie", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            StylePickerGrid(
                selected = category,
                onSelect = {
                    category = it
                    manualCategory = true
                },
                values = TransactionCategory.entries.toTypedArray(),
                columns = 5
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
                TextButton(
                    onClick = {
                        viewModel.removeTransaction(transactionToEdit!!)
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
