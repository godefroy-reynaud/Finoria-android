package com.finoria.app.domain

import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.domain.service.CsvService
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Export CSV × catégories personnalisées + normalisation des noms.
 *
 * (L'import complet passe par Uri/Context Android → couvert par la logique du
 * repository et le picker ; ici on fige le contrat du fichier écrit.)
 */
class CsvCustomCategoryTest {

    private val customId = UUID.randomUUID()
    private val custom = CustomCategory(id = customId, name = "Sport Club")
    private val customs = mapOf(customId to custom)

    private fun tx(
        category: TransactionCategory = TransactionCategory.OTHER,
        customCategoryId: UUID? = null,
        importedCategoryName: String? = null,
    ) = Transaction(
        amount = -42.0,
        comment = "test",
        potentiel = false,
        date = LocalDate.of(2026, 3, 15),
        category = category,
        customCategoryId = customCategoryId,
        importedCategoryName = importedCategoryName,
    )

    @Test
    fun `export ecrit le nom de la categorie personnalisee`() {
        val csv = CsvService.buildCsvContent(listOf(tx(customCategoryId = customId)), customs)!!
        val line = csv.lines()[1]
        assertTrue("attendu 'Sport Club' dans : $line", line.endsWith("Sport Club"))
    }

    @Test
    fun `export retombe sur le libelle par defaut sans categorie personnalisee`() {
        val csv = CsvService.buildCsvContent(listOf(tx(category = TransactionCategory.GROCERY)))!!
        assertTrue(csv.lines()[1].endsWith("Courses"))
    }

    @Test
    fun `export conserve un libelle importe non resolu`() {
        val csv = CsvService.buildCsvContent(listOf(tx(importedCategoryName = "Abo Gym")))!!
        assertTrue(csv.lines()[1].endsWith("Abo Gym"))
    }

    @Test
    fun `un nom avec virgule est echappe RFC 4180`() {
        val id = UUID.randomUUID()
        val withComma = mapOf(id to CustomCategory(id = id, name = "Sport, loisirs"))
        val csv = CsvService.buildCsvContent(listOf(tx(customCategoryId = id)), withComma)!!
        assertTrue(csv.lines()[1].endsWith("\"Sport, loisirs\""))
    }

    @Test
    fun `une categorie personnalisee inconnue de la map retombe sur le libelle par defaut`() {
        // Ex. catégorie supprimée entre-temps : pas de crash, colonne = « Autre ».
        val csv = CsvService.buildCsvContent(listOf(tx(customCategoryId = UUID.randomUUID())))!!
        assertTrue(csv.lines()[1].endsWith("Autre"))
    }

    @Test
    fun `normalisation insensible casse accents et espaces`() {
        val reference = CustomCategory.normalizeName("Épargne")
        assertEquals(reference, CustomCategory.normalizeName(" epargne "))
        assertEquals(reference, CustomCategory.normalizeName("ÉPARGNE"))
        assertNotEquals(reference, CustomCategory.normalizeName("epargnes"))
    }
}
