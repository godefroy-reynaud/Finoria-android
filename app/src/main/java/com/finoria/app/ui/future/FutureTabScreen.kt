package com.finoria.app.ui.future

import androidx.compose.runtime.Composable
import com.finoria.app.data.model.Transaction
import com.finoria.app.viewmodel.MainViewModel

/**
 * Wrapper de l'onglet Futur.
 */
@Composable
fun FutureTabScreen(
    viewModel: MainViewModel,
    onEditTransaction: (Transaction) -> Unit = {}
) {
    PotentialTransactionsScreen(
        viewModel = viewModel,
        onEditTransaction = onEditTransaction,
        embedded = true
    )
}
