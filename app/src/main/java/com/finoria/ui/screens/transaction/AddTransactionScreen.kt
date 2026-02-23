package com.finoria.ui.screens.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.model.Transaction
import com.finoria.model.TransactionCategory
import com.finoria.ui.components.CurrencyTextField
import com.finoria.ui.components.StylePickerGrid
import com.finoria.viewmodel.AppViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf(0.0) }
    var comment by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TransactionCategory.OTHER) }
    var date by remember { mutableStateOf(LocalDate.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle transaction", fontWeight = FontWeight.Bold) },
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
            CurrencyTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Montant",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Note / Commentaire") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(24.dp))
            Text("Catégorie", style = MaterialTheme.typography.labelLarge)
            StylePickerGrid(
                items = TransactionCategory.entries,
                selectedItem = category,
                onItemSelected = { category = it }
            )
            
            Spacer(Modifier.weight(1f))
            
            Button(
                onClick = {
                    if (amount != 0.0) {
                        viewModel.addTransaction(
                            Transaction(
                                amount = amount,
                                comment = comment,
                                category = category,
                                date = date
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount != 0.0
            ) {
                Text("Ajouter")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
