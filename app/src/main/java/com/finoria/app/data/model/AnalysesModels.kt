package com.finoria.app.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Type d'analyse (dépenses ou revenus).
 */
@Serializable
enum class AnalysisType(val label: String) {
    EXPENSES("Dépenses"),
    INCOME("Revenus")
}

/**
 * Données d'une catégorie pour l'affichage dans le pie chart et la liste.
 */
data class CategoryData(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

/**
 * Route de détail pour une catégorie sélectionnée.
 */
data class CategoryDetailRoute(
    val category: TransactionCategory,
    val month: Int,
    val year: Int
)
