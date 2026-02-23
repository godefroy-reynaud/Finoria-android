package com.finoria.app.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.finoria.app.data.model.Account
import com.finoria.app.data.model.AccountStyle
import com.finoria.app.ui.components.StylePickerGrid
import com.finoria.app.viewmodel.MainViewModel

/**
 * Formulaire de création/édition d'un compte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    viewModel: MainViewModel,
    accountToEdit: Account? = null,
    onDismiss: () -> Unit
) {
    val isEdit = accountToEdit != null
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var detail by remember { mutableStateOf(accountToEdit?.detail ?: "") }
    var style by remember { mutableStateOf(accountToEdit?.style ?: AccountStyle.BANK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Modifier le compte" else "Nouveau compte") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                if (isEdit) {
                                    viewModel.updateAccount(
                                        accountToEdit!!.copy(
                                            name = name,
                                            detail = detail,
                                            style = style
                                        )
                                    )
                                } else {
                                    viewModel.addAccount(
                                        Account(
                                            name = name,
                                            detail = detail,
                                            style = style
                                        )
                                    )
                                }
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank()
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
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 15) name = it },
                label = { Text("Nom du compte") },
                supportingText = { Text("${name.length}/15") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = detail,
                onValueChange = { if (it.length <= 20) detail = it },
                label = { Text("Détail (optionnel)") },
                supportingText = { Text("${detail.length}/20") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text("Icône", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            StylePickerGrid(
                selected = style,
                onSelect = { style = it },
                values = AccountStyle.entries.toTypedArray()
            )

            Spacer(Modifier.height(16.dp))

            Text("Aperçu", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            AccountCard(
                account = Account(
                    name = name.ifEmpty { "Nouveau compte" },
                    detail = detail,
                    style = style
                ),
                solde = 0.0,
                futur = 0.0
            )

            if (isEdit) {
                Spacer(Modifier.height(24.dp))
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(accountToEdit!!)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("Supprimer le compte")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
