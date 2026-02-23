package com.finoria.app.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.finoria.app.data.model.RecurringTransaction
import com.finoria.app.data.model.WidgetShortcut
import com.finoria.app.domain.service.CsvService
import com.finoria.app.ui.LocalSnackbarHostState
import com.finoria.app.ui.components.NoAccountView
import com.finoria.app.ui.recurring.AddRecurringScreen
import com.finoria.app.ui.shortcut.AddShortcutScreen
import com.finoria.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Wrapper de l'onglet Home : TopAppBar avec CSV export/import + account picker.
 * Affiche NoAccountView si aucun compte n'est sélectionné.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onShowAccountPicker: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current

    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()
    val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()

    // Sheet states for shortcuts and recurring editing
    var shortcutToEdit by remember { mutableStateOf<WidgetShortcut?>(null) }
    var showAddShortcut by remember { mutableStateOf(false) }
    var recurringToEdit by remember { mutableStateOf<RecurringTransaction?>(null) }
    var showAddRecurring by remember { mutableStateOf(false) }

    // CSV file picker
    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val imported = CsvService.importCsv(it, context)
            if (imported.isNotEmpty()) {
                viewModel.importTransactions(imported)
                scope.launch {
                    snackbarHostState.showSnackbar("${imported.size} transactions importées")
                }
            }
        }
    }

    if (selectedAccountId != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        // CSV actions
                        IconButton(onClick = {
                            val uri = CsvService.generateCsv(
                                transactions,
                                selectedAccount?.name ?: "export",
                                context
                            )
                            if (uri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Exporter CSV")
                                )
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Aucune transaction à exporter")
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Exporter CSV")
                        }
                        IconButton(onClick = {
                            csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Importer CSV")
                        }
                    },
                    actions = {
                        IconButton(onClick = onShowAccountPicker) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Changer de compte")
                        }
                    }
                )
            }
        ) { padding ->
            HomeScreen(
                viewModel = viewModel,
                navController = navController,
                onEditTransaction = { /* handled by MainScreen modal */ },
                onEditShortcut = { shortcutToEdit = it },
                onAddShortcut = { showAddShortcut = true },
                onEditRecurring = { recurringToEdit = it },
                onAddRecurring = { showAddRecurring = true },
                modifier = Modifier.padding(padding)
            )
        }
    } else {
        NoAccountView(onAddAccount = onShowAccountPicker)
    }

    // ─── Shortcut Sheet ──────────────────────────────────────────────
    if (showAddShortcut || shortcutToEdit != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddShortcut = false
                shortcutToEdit = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddShortcutScreen(
                viewModel = viewModel,
                shortcutToEdit = shortcutToEdit,
                onDismiss = {
                    showAddShortcut = false
                    shortcutToEdit = null
                }
            )
        }
    }

    // ─── Recurring Sheet ─────────────────────────────────────────────
    if (showAddRecurring || recurringToEdit != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddRecurring = false
                recurringToEdit = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddRecurringScreen(
                viewModel = viewModel,
                recurringToEdit = recurringToEdit,
                onDismiss = {
                    showAddRecurring = false
                    recurringToEdit = null
                }
            )
        }
    }
}
