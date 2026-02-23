package com.finoria.ui.screens.recurring

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.finoria.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    viewModel: AppViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions récurrentes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_recurring") }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { innerPadding ->
        RecurringTransactionsGridView(
            viewModel = viewModel,
            onEditClick = { recurring ->
                navController.navigate("edit_recurring/${recurring.id}")
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
