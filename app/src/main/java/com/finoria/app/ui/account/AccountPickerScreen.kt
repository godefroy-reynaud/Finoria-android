package com.finoria.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.Account
import com.finoria.app.viewmodel.MainViewModel

/**
 * Bottom sheet de sélection de compte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    viewModel: MainViewModel,
    accounts: List<Account>,
    selectedAccountId: java.util.UUID?,
    onDismiss: () -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (Account) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var contextMenuAccount by remember { mutableStateOf<Account?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = "Mes comptes",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    solde = viewModel.totalNonPotentialForAccount(account.id),
                    futur = viewModel.totalWithPotentialForAccount(account.id),
                    isSelected = account.id == selectedAccountId,
                    onClick = {
                        viewModel.selectAccount(account.id)
                        onDismiss()
                    },
                    onLongClick = { contextMenuAccount = account }
                )

                // Context menu for long press
                DropdownMenu(
                    expanded = contextMenuAccount == account,
                    onDismissRequest = { contextMenuAccount = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        onClick = {
                            contextMenuAccount = null
                            onEditAccount(account)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Réinitialiser") },
                        onClick = {
                            contextMenuAccount = null
                            viewModel.resetAccount(account)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            contextMenuAccount = null
                            viewModel.deleteAccount(account)
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAddAccount,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter un compte")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
