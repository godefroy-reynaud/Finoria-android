package com.finoria.app.domain.service

import com.finoria.app.data.model.AnalysisType
import com.finoria.app.data.model.CategoryData
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionType
import java.time.LocalDate
import kotlin.math.abs

/**
 * Service de calcul pur, sans dépendance d'état.
 * Toutes les fonctions sont stateless et déterministes.
 */
object CalculationService {

    fun totalNonPotential(transactions: List<Transaction>): Double =
        transactions.filter { !it.potentiel }.sumOf { it.amount }

    fun totalPotential(transactions: List<Transaction>): Double =
        transactions.filter { it.potentiel }.sumOf { it.amount }

    fun availableYears(transactions: List<Transaction>): List<Int> =
        transactions.filter { !it.potentiel }
            .mapNotNull { it.date?.year }
            .distinct()
            .sorted()

    fun totalForYear(year: Int, transactions: List<Transaction>): Double =
        transactions.filter { !it.potentiel && it.date?.year == year }
            .sumOf { it.amount }

    fun totalForMonth(month: Int, year: Int, transactions: List<Transaction>): Double =
        transactions.filter {
            !it.potentiel && it.date?.year == year && it.date?.monthValue == month
        }.sumOf { it.amount }

    fun monthlyChangePercentage(transactions: List<Transaction>): Double? {
        val now = LocalDate.now()
        val currentTotal = totalForMonth(now.monthValue, now.year, transactions)
        val prev = now.minusMonths(1)
        val previousTotal = totalForMonth(prev.monthValue, prev.year, transactions)
        if (previousTotal == 0.0) return null
        return ((currentTotal - previousTotal) / abs(previousTotal)) * 100
    }

    fun potentialTransactions(from: List<Transaction>): List<Transaction> =
        from.filter { it.potentiel }

    fun validatedTransactions(
        from: List<Transaction>,
        year: Int? = null,
        month: Int? = null
    ): List<Transaction> {
        var result = from.filter { !it.potentiel }
        year?.let { y -> result = result.filter { it.date?.year == y } }
        month?.let { m -> result = result.filter { it.date?.monthValue == m } }
        return result.sortedByDescending { it.date }
    }

    fun getCategoryBreakdown(
        transactions: List<Transaction>,
        type: AnalysisType,
        month: Int,
        year: Int
    ): List<CategoryData> {
        val filtered = transactions.filter { tx ->
            !tx.potentiel &&
                    tx.date?.year == year &&
                    tx.date?.monthValue == month &&
                    when (type) {
                        AnalysisType.EXPENSES -> tx.amount < 0
                        AnalysisType.INCOME -> tx.amount > 0
                    }
        }
        val totalAmount = filtered.sumOf { abs(it.amount) }
        if (totalAmount == 0.0) return emptyList()

        return filtered
            .groupBy { it.category }
            .map { (category, txns) ->
                val categoryTotal = txns.sumOf { abs(it.amount) }
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
