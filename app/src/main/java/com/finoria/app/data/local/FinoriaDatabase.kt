package com.finoria.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.finoria.app.data.local.dao.AccountDao
import com.finoria.app.data.local.dao.RecurringTransactionDao
import com.finoria.app.data.local.dao.TransactionDao
import com.finoria.app.data.local.dao.WidgetShortcutDao
import com.finoria.app.data.local.entity.AccountEntity
import com.finoria.app.data.local.entity.RecurringTransactionEntity
import com.finoria.app.data.local.entity.TransactionEntity
import com.finoria.app.data.local.entity.WidgetShortcutEntity

/**
 * Base Room locale de Finoria.
 *
 * Règles de suppression (au niveau base, reproduisant SwiftData) :
 * - Account → enfants : **CASCADE**
 * - RecurringTransaction → Transaction : **SET_NULL** (l'historique généré est conservé)
 *
 * ⚠️ Toute évolution du schéma doit **bumper `version`** + fournir une `Migration`
 * (jamais `fallbackToDestructiveMigration` en prod : perte de données).
 */
@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        RecurringTransactionEntity::class,
        WidgetShortcutEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FinoriaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun widgetShortcutDao(): WidgetShortcutDao

    companion object {
        const val NAME = "finoria.db"
    }
}
