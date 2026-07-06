package com.finoria.app.ui.analyses

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.finoria.app.data.model.AnalysisType
import com.finoria.app.navigation.Screen
import com.finoria.app.ui.LocalCustomCategories
import com.finoria.app.util.formattedCurrency
import com.finoria.app.util.monthName
import com.finoria.app.viewmodel.MainViewModel
import java.time.LocalDate

/**
 * Écran d'analyses : SegmentedButtons + MonthNavigator + PieChart + CategoryList.
 */
@Composable
fun AnalysesScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val transactions by viewModel.currentTransactions.collectAsStateWithLifecycle()

    // rememberSaveable : restaure le filtre (type/mois/année/catégorie) au retour
    // depuis un sous-écran, au lieu de repartir du mois courant en mode dépenses.
    var analysisType by rememberSaveable { mutableStateOf(AnalysisType.EXPENSES) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedYear by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }
    // Clé de part sélectionnée : nom d'enum (défaut) ou UUID (catégorie perso).
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val categoryData = viewModel.getCategoryBreakdown(
        transactions, analysisType, selectedMonth, selectedYear,
        LocalCustomCategories.current
    )
    val totalAmount = categoryData.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Segmented buttons Dépenses / Revenus
        item {
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AnalysisType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = analysisType == type,
                        onClick = {
                            analysisType = type
                            selectedKey = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index, AnalysisType.entries.size
                        )
                    ) { Text(type.label) }
                }
            }
        }

        // Month navigator: < Mois Année >
        item {
            MonthNavigator(
                month = selectedMonth,
                year = selectedYear,
                onPrevious = {
                    if (selectedMonth == 1) {
                        selectedMonth = 12
                        selectedYear--
                    } else {
                        selectedMonth--
                    }
                    selectedKey = null
                },
                onNext = {
                    if (selectedMonth == 12) {
                        selectedMonth = 1
                        selectedYear++
                    } else {
                        selectedMonth++
                    }
                    selectedKey = null
                }
            )
        }

        // Pie chart
        item {
            if (categoryData.isNotEmpty()) {
                AnalysesPieChart(
                    data = categoryData,
                    total = totalAmount,
                    analysisType = analysisType,
                    selectedKey = selectedKey,
                    onCategorySelected = { item ->
                        selectedKey = if (selectedKey == item.selectionKey) null
                        else item.selectionKey
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Aucune donnée pour cette période",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Total
        if (categoryData.isNotEmpty()) {
            item {
                Text(
                    text = "Total : ${totalAmount.formattedCurrency()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Category breakdown rows
        items(categoryData) { item ->
            CategoryBreakdownRow(
                data = item,
                totalAmount = totalAmount,
                isSelected = selectedKey == item.selectionKey,
                onClick = {
                    navController.navigate(
                        Screen.categoryTransactionsRoute(
                            // Nom d'enum, ou UUID pour une catégorie perso —
                            // le NavHost distingue les deux au parsing.
                            item.selectionKey,
                            selectedMonth,
                            selectedYear
                        )
                    )
                }
            )
        }

        // Bottom spacer
        item { Spacer(Modifier.height(80.dp)) }
    }
}

/**
 * Navigateur de mois : boutons < et >, mois + année au centre.
 */
@Composable
fun MonthNavigator(
    month: Int,
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent")
        }
        Text(
            text = "${monthName(month)} $year",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant")
        }
    }
}
