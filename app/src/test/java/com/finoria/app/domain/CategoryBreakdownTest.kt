package com.finoria.app.domain

import com.finoria.app.data.model.AnalysisType
import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.domain.service.CalculationService
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Ventilation des analyses : chaque catégorie personnalisée a sa **propre part**,
 * distincte du bucket « Autre ».
 */
class CategoryBreakdownTest {

    private val sportId = UUID.randomUUID()
    private val sport = CustomCategory(id = sportId, name = "Sport Club", colorHex = "#FF3B30")
    private val customs = mapOf(sportId to sport)

    private fun tx(
        amount: Double,
        category: TransactionCategory = TransactionCategory.OTHER,
        customCategoryId: UUID? = null,
    ) = Transaction(
        amount = amount,
        potentiel = false,
        date = LocalDate.of(2026, 3, 10),
        category = category,
        customCategoryId = customCategoryId,
    )

    @Test
    fun `une categorie perso a sa propre part, separee de Autre`() {
        val data = CalculationService.getCategoryBreakdown(
            listOf(
                tx(-30.0, customCategoryId = sportId),
                tx(-10.0, customCategoryId = sportId),
                tx(-60.0), // « Autre » pur
            ),
            AnalysisType.EXPENSES, month = 3, year = 2026, customCategories = customs,
        )

        assertEquals(2, data.size)
        val sportSlice = data.first { it.customCategory != null }
        val otherSlice = data.first { it.customCategory == null }

        assertEquals("Sport Club", sportSlice.label)
        assertEquals(40.0, sportSlice.amount, 0.001)
        assertEquals(0.4f, sportSlice.percentage, 0.001f)
        assertEquals(sport.color, sportSlice.color)
        assertEquals(sportId.toString(), sportSlice.selectionKey)

        assertEquals("Autre", otherSlice.label)
        assertEquals(60.0, otherSlice.amount, 0.001)
        assertEquals(TransactionCategory.OTHER.name, otherSlice.selectionKey)
    }

    @Test
    fun `une reference perso non resoluble retombe dans la categorie par defaut`() {
        val data = CalculationService.getCategoryBreakdown(
            listOf(tx(-50.0, customCategoryId = UUID.randomUUID())),
            AnalysisType.EXPENSES, month = 3, year = 2026, customCategories = customs,
        )

        assertEquals(1, data.size)
        assertNull(data[0].customCategory)
        assertEquals("Autre", data[0].label)
    }

    @Test
    fun `sans map fournie, comportement historique conserve`() {
        val data = CalculationService.getCategoryBreakdown(
            listOf(
                tx(-30.0, customCategoryId = sportId),
                tx(-70.0, category = TransactionCategory.GROCERY),
            ),
            AnalysisType.EXPENSES, month = 3, year = 2026,
        )

        assertEquals(2, data.size)
        assertEquals(listOf("Courses", "Autre"), data.map { it.label })
    }
}
