package com.finoria.ui.screens.analyses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.domain.CalculationService
import com.finoria.model.TransactionType
import com.finoria.ui.components.AnalysesPieChart
import com.finoria.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysesScreen(viewModel: AppViewModel, onCategoryClick: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions = uiState.transactionsByAccount[uiState.selectedAccountId] ?: emptyList()
    var analysisType by remember { mutableStateOf(AnalysisType.EXPENSE) }

    val categoryData = remember(transactions, analysisType) {
        CalculationService.getCategoryBreakdown(
            transactions,
            if (analysisType == AnalysisType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Analyses", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            SegmentedControl(
                selectedType = analysisType,
                onTypeSelected = { analysisType = it }
            )
            Spacer(Modifier.height(16.dp))

            if (categoryData.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune donnée pour cette période", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    item {
                        AnalysesPieChart(
                            data = categoryData,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    item {
                        Text(
                            "Répartition par catégorie",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(categoryData) { data ->
                        CategoryBreakdownRow(categoryData = data) {
                            onCategoryClick(data.category.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    selectedType: AnalysisType,
    onTypeSelected: (AnalysisType) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedType == AnalysisType.EXPENSE,
            onClick = { onTypeSelected(AnalysisType.EXPENSE) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text("Dépenses")
        }
        SegmentedButton(
            selected = selectedType == AnalysisType.INCOME,
            onClick = { onTypeSelected(AnalysisType.INCOME) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text("Revenus")
        }
    }
}
