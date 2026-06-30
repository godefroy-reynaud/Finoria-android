package com.finoria.app.domain

import com.finoria.app.data.model.RecurrenceFrequency
import com.finoria.app.data.model.RecurringTransaction
import com.finoria.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Vérifie que les occurrences récurrentes sont **ancrées sur startDate** et
 * clampées correctement pour les mois courts (critère du plan de portage).
 *
 * Le bug à éviter : chaîner `plusMonths(1)` depuis l'occurrence précédente fait
 * dériver un loyer du 31 vers le 28 de façon permanente après février.
 */
class RecurrenceAnchoringTest {

    private fun monthly(start: LocalDate) = RecurringTransaction(
        amount = 800.0,
        comment = "Loyer",
        type = TransactionType.EXPENSE,
        frequency = RecurrenceFrequency.MONTHLY,
        startDate = start,
    )

    @Test
    fun `monthly on the 31st clamps to month length without drifting`() {
        val rec = monthly(LocalDate.of(2026, 1, 31))

        val dates = rec.occurrences(
            from = LocalDate.of(2026, 1, 1),
            to = LocalDate.of(2026, 5, 31),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28), // clampé
                LocalDate.of(2026, 3, 31), // revient au 31 (pas de dérive !)
                LocalDate.of(2026, 4, 30), // clampé
                LocalDate.of(2026, 5, 31),
            ),
            dates,
        )
    }

    @Test
    fun `occurrenceDate is anchored on startDate, not chained`() {
        val rec = monthly(LocalDate.of(2026, 1, 31))
        // index 2 = mars : doit être le 31, pas le 28 (preuve d'absence de dérive)
        assertEquals(LocalDate.of(2026, 3, 31), rec.occurrenceDate(2))
    }

    @Test
    fun `yearly on Feb 29 clamps on non-leap years`() {
        val rec = RecurringTransaction(
            amount = 100.0,
            comment = "Assurance",
            type = TransactionType.EXPENSE,
            frequency = RecurrenceFrequency.YEARLY,
            startDate = LocalDate.of(2024, 2, 29),
        )
        assertEquals(LocalDate.of(2025, 2, 28), rec.occurrenceDate(1)) // 2025 non bissextile
        assertEquals(LocalDate.of(2028, 2, 29), rec.occurrenceDate(4)) // 2028 bissextile : retour au 29
    }
}
