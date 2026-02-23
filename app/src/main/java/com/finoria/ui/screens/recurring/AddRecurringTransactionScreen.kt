package com.finoria.ui.screens.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.model.*
import com.finoria.ui.components.CurrencyTextField
import com.finoria.ui.components.StylePickerGrid
import com.finoria.viewmodel.AppViewModel
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringTransactionScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf(0.0) }
    var comment by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TransactionCategory.OTHER) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var frequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle récurrence", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Dépense") }
                SegmentedButton(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Revenu") }
            }

            Spacer(Modifier.height(16.dp))

            CurrencyTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Montant mensuel",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Nom de l'abonnement / Note") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Fréquence", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(freq.name.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }) }
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("Catégorie", style = MaterialTheme.typography.labelLarge)
            StylePickerGrid(
                items = TransactionCategory.entries,
                selectedItem = category,
                onItemSelected = { category = it }
            )
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (amount != 0.0) {
                        viewModel.addRecurringTransaction(
                            RecurringTransaction(
                                amount = amount,
                                comment = comment,
                                type = type,
                                category = category,
                                frequency = frequency,
                                startDate = startDate
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount != 0.0
            ) {
                Text("Enregistrer la récurrence")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}