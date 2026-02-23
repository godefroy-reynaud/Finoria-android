package com.finoria.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finoria.model.Account
import com.finoria.model.AccountStyle
import com.finoria.ui.components.StylePickerGrid
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(AccountStyle.BANK) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Nouveau compte", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du compte") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it },
                label = { Text("Détail (ex: IBAN, Banque)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
            Text("Style", style = MaterialTheme.typography.labelLarge)
            StylePickerGrid(
                items = AccountStyle.entries,
                selectedItem = selectedStyle,
                onItemSelected = { selectedStyle = it }
            )
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(Account(name = name, detail = detail, style = selectedStyle))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Enregistrer")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
