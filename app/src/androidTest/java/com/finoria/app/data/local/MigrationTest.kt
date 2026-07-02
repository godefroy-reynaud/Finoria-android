package com.finoria.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rejoue chaque migration sur une vraie base créée depuis le **schéma exporté**
 * (`app/schemas/…/<version>.json`) puis valide que le résultat correspond
 * exactement au schéma de la version cible — c'est le garde-fou officiel Room
 * contre la perte de données à la mise à jour.
 *
 * ⚠️ À chaque bump de version de [FinoriaDatabase] : ajouter ici un test
 * `migrateNtoN+1` qui insère des données représentatives en vN et vérifie
 * qu'elles survivent à la migration.
 *
 * Test instrumenté (nécessite un device/émulateur).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FinoriaDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesAllData() {
        val dbName = "migration-test"

        // Base en version 1, peuplée comme le ferait l'app publiée en v1.
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO accounts (id, name, detail, style) " +
                    "VALUES ('acc-1', 'Compte courant', 'Perso', 'BANK')"
            )
            execSQL(
                "INSERT INTO recurring_transactions " +
                    "(id, accountId, amount, comment, type, category, frequency, " +
                    "startDate, lastGeneratedDate, isPaused) " +
                    "VALUES ('rec-1', 'acc-1', 800.0, 'Loyer', 'EXPENSE', 'RENT', " +
                    "'MONTHLY', 20000, NULL, 0)"
            )
            execSQL(
                "INSERT INTO widget_shortcuts (id, accountId, amount, comment, type, category) " +
                    "VALUES ('sc-1', 'acc-1', 4.5, 'Café', 'EXPENSE', 'COFFEE')"
            )
            execSQL(
                "INSERT INTO transactions " +
                    "(id, accountId, amount, comment, potentiel, date, category, sourceRecurringId) " +
                    "VALUES ('tx-1', 'acc-1', -800.0, 'Loyer', 0, 20001, 'RENT', 'rec-1')"
            )
            close()
        }

        // Migration + validation stricte contre le schéma exporté 2.json.
        val db = helper.runMigrationsAndValidate(dbName, 2, true, FinoriaDatabase.MIGRATION_1_2)

        db.query("SELECT name, style FROM accounts WHERE id = 'acc-1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Compte courant", c.getString(0))
            assertEquals("BANK", c.getString(1))
        }
        db.query(
            "SELECT amount, category, customCategoryId FROM recurring_transactions WHERE id = 'rec-1'"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(800.0, c.getDouble(0), 0.0)
            assertEquals("RENT", c.getString(1))
            assertNull(c.getString(2)) // nouvelle colonne : null par défaut
        }
        db.query("SELECT comment, customCategoryId FROM widget_shortcuts WHERE id = 'sc-1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Café", c.getString(0))
            assertNull(c.getString(1))
        }
        db.query(
            "SELECT amount, sourceRecurringId, customCategoryId, importedCategoryName " +
                "FROM transactions WHERE id = 'tx-1'"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(-800.0, c.getDouble(0), 0.0)
            assertEquals("rec-1", c.getString(1)) // le lien vers la récurrence survit
            assertNull(c.getString(2))
            assertNull(c.getString(3))
        }
        db.close()
    }
}
