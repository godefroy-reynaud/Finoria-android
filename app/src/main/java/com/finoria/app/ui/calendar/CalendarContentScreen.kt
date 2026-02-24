package com.finoria.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.finoria.app.viewmodel.MainViewModel

/**
 * Mode d'affichage du calendrier.
 */
enum class CalendarViewMode(val label: String) {
    DAY("Jour"),
    MONTH("Mois"),
    YEAR("Année")
}

/**
 * Écran principal du calendrier : Segmented buttons Jour/Mois/Année + contenu conditionnel.
 */
@Composable
fun CalendarContentScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onEditTransaction: (Transaction) -> Unit = {}
) {
    var mode by remember { mutableStateOf(CalendarViewMode.DAY) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            CalendarViewMode.entries.forEachIndexed { index, m ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = { mode = m },
                    shape = SegmentedButtonDefaults.itemShape(
                        index, CalendarViewMode.entries.size
                    )
                ) { Text(m.label) }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (mode) {
            CalendarViewMode.DAY -> AllTransactionsScreen(
                viewModel = viewModel,
                navController = navController,
                embedded = true,
                onEditTransaction = onEditTransaction
            )
            CalendarViewMode.MONTH -> MonthsOverviewScreen(
                viewModel = viewModel,
                navController = navController
            )
            CalendarViewMode.YEAR -> YearsOverviewScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
    }
}

/**
 * Vue des mois pour l'année courante, cliquable pour naviguer vers les détails.
 */
@Composable
fun MonthsOverviewScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()
    val years = viewModel.availableYears(transactions).ifEmpty { listOf(java.time.LocalDate.now().year) }

    // Display as a list of year sections with months
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        years.sortedDescending().forEach { year ->
            item {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
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
        item { Spacer(Modifier.height(80.dp)) }
    }
}

/**
 * Vue des années avec total annuel, cliquable pour voir les mois.
 */
@Composable
fun YearsOverviewScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()
    val years = viewModel.availableYears(transactions)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        items(years.sortedDescending()) { year ->
            val total = viewModel.totalForYear(year, transactions)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = {
                    navController.navigate(Screen.monthsListRoute(year))
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = total.formattedCurrency(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (total >= 0) IncomeGreen
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
