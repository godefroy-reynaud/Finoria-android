package com.finoria.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finoria.app.data.local.entity.AccountEntity
import com.finoria.app.data.local.entity.RecurringTransactionEntity
import com.finoria.app.data.local.entity.TransactionEntity
import com.finoria.app.data.local.entity.WidgetShortcutEntity
import com.finoria.app.data.model.AccountStyle
import com.finoria.app.data.model.RecurrenceFrequency
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.data.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

/**
 * Vérifie les règles de suppression au niveau base (équivalent SwiftData) :
 * - Account → enfants : **CASCADE**
 * - RecurringTransaction → Transaction : **SET_NULL** (l'historique généré est conservé)
 *
 * Test instrumenté (Room in-memory) : nécessite un device/émulateur.
 */
@RunWith(AndroidJUnit4::class)
class FinoriaDatabaseTest {

    private lateinit var db: FinoriaDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinoriaDatabase::class.java).build()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun account(id: String) =
        AccountEntity(id = id, name = "Compte", detail = "", style = AccountStyle.BANK)

    @Test
    fun deletingAccount_cascadesToAllChildren() = runBlocking {
        val accId = UUID.randomUUID().toString()
        val recId = UUID.randomUUID().toString()
        db.accountDao().upsert(account(accId))
        db.recurringTransactionDao().upsert(
            RecurringTransactionEntity(
                id = recId, accountId = accId, amount = 800.0, comment = "Loyer",
                type = TransactionType.EXPENSE, category = TransactionCategory.RENT,
                frequency = RecurrenceFrequency.MONTHLY, startDate = LocalDate.now(),
                lastGeneratedDate = null, isPaused = false,
            ),
        )
        db.transactionDao().upsert(
            TransactionEntity(
                id = UUID.randomUUID().toString(), accountId = accId, amount = -45.0,
                comment = "Courses", potentiel = false, date = LocalDate.now(),
                category = TransactionCategory.SHOPPING, sourceRecurringId = null,
            ),
        )
        db.widgetShortcutDao().upsert(
            WidgetShortcutEntity(
                id = UUID.randomUUID().toString(), accountId = accId, amount = 10.0,
                comment = "Café", type = TransactionType.EXPENSE,
                category = TransactionCategory.OTHER,
            ),
        )

        db.accountDao().deleteById(accId)

        assertTrue(db.transactionDao().getAll().isEmpty())
        assertTrue(db.recurringTransactionDao().getAll().isEmpty())
        assertTrue(db.widgetShortcutDao().getAll().isEmpty())
    }

    @Test
    fun deletingRecurring_nullifiesGeneratedTransactions_butKeepsThem() = runBlocking {
        val accId = UUID.randomUUID().toString()
        val recId = UUID.randomUUID().toString()
        val txId = UUID.randomUUID().toString()
        db.accountDao().upsert(account(accId))
        db.recurringTransactionDao().upsert(
            RecurringTransactionEntity(
                id = recId, accountId = accId, amount = 800.0, comment = "Loyer",
                type = TransactionType.EXPENSE, category = TransactionCategory.RENT,
                frequency = RecurrenceFrequency.MONTHLY, startDate = LocalDate.now(),
                lastGeneratedDate = null, isPaused = false,
            ),
        )
        db.transactionDao().upsert(
            TransactionEntity(
                id = txId, accountId = accId, amount = -800.0, comment = "Loyer",
                potentiel = false, date = LocalDate.now(),
                category = TransactionCategory.RENT, sourceRecurringId = recId,
            ),
        )

        db.recurringTransactionDao().deleteById(recId)

        val remaining = db.transactionDao().getAll()
        assertEquals(1, remaining.size) // l'historique est conservé
        assertEquals(txId, remaining.first().id)
        assertNull(remaining.first().sourceRecurringId) // lien remis à null
    }
}
