package com.finoria.domain

import com.finoria.model.Transaction
import com.finoria.model.TransactionType
import com.finoria.ui.screens.analyses.CategoryData
import java.time.LocalDate
import kotlin.math.abs

object CalculationService {
    fun totalNonPotential(transactions: List<Transaction>): Double =
        transactions.filter { !it.isPotential }.sumOf { it.amount }

    fun totalPotential(transactions: List<Transaction>): Double =
        transactions.filter { it.isPotential }.sumOf { it.amount }

    fun totalForMonth(month: Int, year: Int, transactions: List<Transaction>): Double =
        transactions.filter { 
            val date = it.date ?: return@filter false
            date.monthValue == month && date.year == year && !it.isPotential 
        }.sumOf { it.amount }

    fun availableYears(transactions: List<Transaction>): List<Int> =
        transactions.mapNotNull { it.date?.year }.distinct().sortedDescending()

    fun monthlyChangePercentage(currentMonth: Double, previousMonth: Double): Double {
        if (previousMonth == 0.0) return 0.0
        return ((currentMonth - previousMonth) / abs(previousMonth)) * 100.0
    }

    fun validatedTransactions(year: Int, month: Int, transactions: List<Transaction>): List<Transaction> =
        transactions.filter {
            val date = it.date ?: return@filter false
            date.year == year && date.monthValue == month && !it.isPotential
        }.sortedByDescending { it.date }

    fun getCategoryBreakdown(
        transactions: List<Transaction>,
        type: TransactionType
    ): List<CategoryData> {
        val filteredTransactions = transactions.filter { !it.isPotential &&
            if (type == TransactionType.INCOME) it.amount > 0 else it.amount < 0
        }

        val totalAmount = filteredTransactions.sumOf { abs(it.amount) }
        if (totalAmount == 0.0) return emptyList()

        return filteredTransactions
            .groupBy { it.category }
            .map { (category, transactionsInCategory) ->
                val categoryTotal = transactionsInCategory.sumOf { abs(it.amount) }
                CategoryData(
                    category = category,
                    amount = categoryTotal,
                    percentage = (categoryTotal / totalAmount).toFloat(),
                    color = category.color
                )
            }
            .sortedByDescending { it.amount }
    }
}
