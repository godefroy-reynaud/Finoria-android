package com.finoria.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.finoria.data.CsvService
import com.finoria.domain.CalculationService
import com.finoria.model.Transaction
import com.finoria.ui.components.StyleIconView
import com.finoria.ui.components.TransactionRow
import com.finoria.ui.screens.account.AddAccountSheet
import com.finoria.viewmodel.AppViewModel
import com.finoria.ui.navigation.Screen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AppViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedAccount = uiState.accounts.find { it.id.toString() == uiState.selectedAccountId }
    val transactions = uiState.transactionsByAccount[uiState.selectedAccountId] ?: emptyList()

    val totalBalance = CalculationService.totalNonPotential(transactions)
    val monthBalance = CalculationService.totalForMonth(
        LocalDate.now().monthValue,
        LocalDate.now().year,
        transactions
    )
    val potentialBalance = CalculationService.totalPotential(transactions)
    val context = LocalContext.current

    var showAddAccountSheet by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importTransactionsFromCsv(context, uri)
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finoria", fontWeight = FontWeight.Bold) },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.AccountBalance, contentDescription = "Comptes")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        uiState.accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    viewModel.selectAccount(account.id.toString())
                                    showMenu = false
                                },
                                leadingIcon = { 
                                    StyleIconView(style = account.style, modifier = Modifier.size(24.dp)) 
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ajouter un compte") },
                            onClick = { 
                                showAddAccountSheet = true
                                showMenu = false 
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Importer (CSV)") },
                            onClick = { 
                                importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv"))
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Exporter (CSV)") },
                            onClick = {
                                val csvContent = CsvService.generateCsv(transactions, selectedAccount?.name ?: "Export")
                                val file = CsvService.saveCsvToFile(context, csvContent)
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    exportLauncher.launch(android.content.Intent.createChooser(intent, "Exporter les transactions"))
                                }
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_transaction") }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                BalanceHeader(
                    totalBalance = totalBalance,
                    onClick = { navController.navigate("all_transactions") }
                )
            }

            if (selectedAccount != null) {
                item {
                    Text("Compte sélectionné", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    AccountCard(
                        account = selectedAccount,
                        balance = totalBalance,
                        onClick = { navController.navigate("all_transactions") }
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickCard(
                            title = "Solde du mois",
                            amount = monthBalance,
                            onClick = {
                                navController.navigate("calendar_month/${LocalDate.now().year}/${LocalDate.now().monthValue}")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        QuickCard(
                            title = "À venir",
                            amount = potentialBalance,
                            onClick = { navController.navigate(Screen.Future.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    TextButton(
                        onClick = { navController.navigate("recurring_list") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Récurrences${if (uiState.recurringTransactions.isNotEmpty()) " (${uiState.recurringTransactions.size})" else ""}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.weight(1f))
                        Text("Gérer →")
                    }
                }
            } else if (!uiState.isLoading) {
                item {
                    com.finoria.ui.components.EmptyStateView(
                        title = "Aucun compte",
                        subtitle = "Créez votre premier compte pour commencer à gérer vos finances"
                    )
                    Button(
                        onClick = { showAddAccountSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Créer mon premier compte")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Raccourcis rapides", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { navController.navigate("add_shortcut") }) {
                        Text("+ Ajouter")
                    }
                }
            }
            if (uiState.shortcuts.isNotEmpty()) {
                item {
                    ShortcutsGrid(
                        shortcuts = uiState.shortcuts,
                        onShortcutClick = { shortcut ->
                            viewModel.addTransaction(
                                Transaction(
                                    amount = shortcut.amount,
                                    comment = shortcut.name,
                                    category = shortcut.category,
                                    date = LocalDate.now()
                                )
                            )
                            viewModel.showToast("Transaction rapide ajoutée !")
                        },
                        onShortcutLongClick = { shortcut ->
                            navController.navigate("edit_shortcut/${shortcut.id}")
                        }
                    )
                }
            }

            item {
                Text("Dernières transactions", style = MaterialTheme.typography.titleMedium)
            }

            items(transactions.sortedByDescending { it.date }.take(10)) { transaction ->
                TransactionRow(transaction = transaction)
            }
            
            item { Spacer(Modifier.height(16.dp)) }
        }

        if (showAddAccountSheet) {
            AddAccountSheet(
                onDismiss = { showAddAccountSheet = false },
                onSave = { account ->
                    viewModel.addAccount(account)
                    showAddAccountSheet = false
                }
            )
        }
    }
}
