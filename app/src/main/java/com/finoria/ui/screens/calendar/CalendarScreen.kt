package com.finoria.ui.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finoria.viewmodel.AppViewModel
import java.time.LocalDate

enum class CalendarMode { DAY, MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: AppViewModel, onMonthSelected: (Int, Int) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions = uiState.transactionsByAccount[uiState.selectedAccountId] ?: emptyList()
    
    var mode by remember { mutableStateOf(CalendarMode.DAY) }
    var selectedYear by remember { mutableStateOf(LocalDate.now().year) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calendrier", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                CalendarMode.entries.forEachIndexed { index, calendarMode ->
                    SegmentedButton(
                        selected = mode == calendarMode,
                        onClick = { mode = calendarMode },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = CalendarMode.entries.size)
                    ) {
                        Text(when(calendarMode) {
                            CalendarMode.DAY -> "Jour"
                            CalendarMode.MONTH -> "Mois"
                            CalendarMode.YEAR -> "Année"
                        })
                    }
                }
            }

            when (mode) {
                CalendarMode.DAY -> AllTransactionsView(transactions)
                CalendarMode.MONTH -> MonthsView(selectedYear) { month -> 
                    onMonthSelected(selectedYear, month) 
                }
                CalendarMode.YEAR -> {
                    val years = (selectedYear - 5..selectedYear + 1).toList().reversed()
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(years) { year ->
                            ListItem(
                                headlineContent = { Text(year.toString(), fontWeight = FontWeight.Bold) },
                                modifier = Modifier.clickable { 
                                    selectedYear = year
                                    mode = CalendarMode.MONTH
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
