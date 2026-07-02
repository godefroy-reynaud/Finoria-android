package com.finoria.app.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 *
 * Une part peut représenter une **catégorie personnalisée** ([customCategory]
 * non nul, [category] vaut alors `Autre` par convention) : le nom/icône/couleur
 * affichés sont ceux de la catégorie personnalisée.
 */
data class CategoryData(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val customCategory: CustomCategory? = null
) {
    /** Clé stable identifiant la part (sélection, navigation). */
    val selectionKey: String
        get() = customCategory?.id?.toString() ?: category.name

    val label: String
        get() = customCategory?.name ?: category.label

    val icon: ImageVector
        get() = customCategory?.icon ?: category.icon
}
