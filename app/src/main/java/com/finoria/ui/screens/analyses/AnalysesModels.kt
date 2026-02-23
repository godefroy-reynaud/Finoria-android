package com.finoria.ui.screens.analyses

import androidx.compose.ui.graphics.Color
import com.finoria.model.TransactionCategory

enum class AnalysisType {
    EXPENSE, INCOME
}

data class CategoryData(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Float,
    val color: Color
)