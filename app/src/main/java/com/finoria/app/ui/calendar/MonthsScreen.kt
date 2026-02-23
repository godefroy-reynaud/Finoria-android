package com.finoria.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.finoria.app.data.model.Transaction
import com.finoria.app.navigation.Screen
import com.finoria.app.ui.theme.IncomeGreen
import com.finoria.app.util.formattedCurrency
import com.finoria.app.util.monthName
import com.finoria.app.viewmodel.MainViewModel

/**
 * Écran affichant les mois d'une année donnée sous forme de grille.
 * Navigation depuis le vue Calendrier (mode Année -> clic sur année).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthsScreen(
    viewModel: MainViewModel,
    year: Int,
    navController: NavController
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$year") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            MonthsGridForYear(
                viewModel = viewModel,
                year = year,
                transactions = transactions,
                onMonthClick = { month ->
                    navController.navigate(Screen.transactionsListRoute(month, year))
                }
            )
        }
    }
}

/**
 * Grille de 12 mois pour une année.
 * Réutilisé dans MonthsScreen et MonthsOverviewScreen.
 */
@Composable
fun MonthsGridForYear(
    viewModel: MainViewModel,
    year: Int,
    transactions: List<Transaction>,
    onMonthClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((1..12).toList()) { month ->
            val total = viewModel.totalForMonth(month, year, transactions)
            val hasData = viewModel.validatedTransactions(transactions, year, month).isNotEmpty()

            Card(
                onClick = { onMonthClick(month) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasData)
                        MaterialTheme.colorScheme.surfaceContainerLow
                    else
                        MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = monthName(month).take(4),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    if (hasData) {
                        Text(
                            text = total.formattedCurrency(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (total >= 0) IncomeGreen
                            else MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
