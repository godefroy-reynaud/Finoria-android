package com.finoria.app.ui.recurring

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.RecurrenceFrequency
import com.finoria.app.data.model.RecurringTransaction
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
 * Formulaire de création/édition d'une transaction récurrente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringScreen(
    viewModel: MainViewModel,
    recurringToEdit: RecurringTransaction? = null,
    onDismiss: () -> Unit
) {
    val isEdit = recurringToEdit != null

    var type by remember { mutableStateOf(recurringToEdit?.type ?: TransactionType.EXPENSE) }
    var amountText by remember {
        mutableStateOf(recurringToEdit?.amount?.toAmountInput() ?: "")
    }
    var comment by remember { mutableStateOf(recurringToEdit?.comment ?: "") }
    var category by remember {
        mutableStateOf(recurringToEdit?.category ?: TransactionCategory.OTHER)
    }
    var customCategoryId by remember { mutableStateOf(recurringToEdit?.customCategoryId) }
    var frequency by remember {
        mutableStateOf(recurringToEdit?.frequency ?: RecurrenceFrequency.MONTHLY)
    }
    var frequencyExpanded by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = recurringToEdit?.startDate?.toEpochMillis()
            ?: System.currentTimeMillis()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Modifier la récurrence" else "Nouvelle récurrence") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: return@TextButton
                            val startDate = datePickerState.selectedDateMillis?.toLocalDate()
                                ?: LocalDate.now()

                            val recurring = RecurringTransaction(
                                id = recurringToEdit?.id ?: UUID.randomUUID(),
                                amount = amount,
                                comment = comment,
                                type = type,
                                category = if (customCategoryId != null) {
                                    TransactionCategory.OTHER
                                } else category,
                                frequency = frequency,
                                startDate = startDate,
                                lastGeneratedDate = recurringToEdit?.lastGeneratedDate,
                                isPaused = recurringToEdit?.isPaused ?: false,
                                customCategoryId = customCategoryId
                            )
                            if (isEdit) viewModel.updateRecurring(recurring)
                            else viewModel.addRecurring(recurring)
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

            // Frequency picker
            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = it }
            ) {
                OutlinedTextField(
                    value = frequency.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fréquence") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    RecurrenceFrequency.entries.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq.label) },
                            onClick = {
                                frequency = freq
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Date de début", style = MaterialTheme.typography.titleSmall)
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth()
            )

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
                        viewModel.removeRecurring(recurringToEdit!!)
                        onDismiss()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
