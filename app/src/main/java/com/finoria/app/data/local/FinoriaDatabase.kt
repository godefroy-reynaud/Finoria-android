package com.finoria.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finoria.app.data.local.dao.AccountDao
import com.finoria.app.data.local.dao.CustomCategoryDao
import com.finoria.app.data.local.dao.RecurringTransactionDao
import com.finoria.app.data.local.dao.TransactionDao
import com.finoria.app.data.local.dao.WidgetShortcutDao
import com.finoria.app.data.local.entity.AccountEntity
import com.finoria.app.data.local.entity.CustomCategoryEntity
import com.finoria.app.data.local.entity.RecurringTransactionEntity
import com.finoria.app.data.local.entity.TransactionEntity
import com.finoria.app.data.local.entity.WidgetShortcutEntity

/**
 * Base Room locale de Finoria.
 *
 * Règles de suppression (au niveau base, reproduisant SwiftData) :
 * - Account → enfants : **CASCADE**
 * - RecurringTransaction → Transaction : **SET_NULL** (l'historique généré est conservé)
 * - CustomCategory → Transaction/Shortcut/Recurring : **SET_NULL** (nullify : ce qui
 *   l'utilisait retombe sur la catégorie par défaut `Autre`)
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
        CustomCategoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FinoriaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun widgetShortcutDao(): WidgetShortcutDao
    abstract fun customCategoryDao(): CustomCategoryDao

    companion object {
        const val NAME = "finoria.db"

        /**
         * v1 → v2 : catégories personnalisées.
         *
         * Crée `custom_categories`, puis ajoute `customCategoryId` (FK SET_NULL) aux
         * trois tables qui la référencent (+ `importedCategoryName` sur
         * `transactions`). SQLite ne permet pas d'ajouter une FK via
         * `ALTER TABLE ADD COLUMN` → recréation des tables (create → copy → drop →
         * rename), le pattern des migrations générées par Room. Les FK sont
         * inactives pendant `onUpgrade` (Room ne les active qu'à `onOpen`), l'ordre
         * de recréation est donc sans risque de violation.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Nouvelle table des catégories personnalisées.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_categories` (" +
                        "`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, `symbol` TEXT NOT NULL, " +
                        "`colorHex` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_custom_categories_accountId` " +
                        "ON `custom_categories` (`accountId`)"
                )

                // 2. recurring_transactions (recréée avant transactions qui la référence).
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring_transactions_new` (" +
                        "`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, `comment` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                        "`frequency` TEXT NOT NULL, `startDate` INTEGER NOT NULL, " +
                        "`lastGeneratedDate` INTEGER, `isPaused` INTEGER NOT NULL, " +
                        "`customCategoryId` TEXT, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`customCategoryId`) REFERENCES `custom_categories`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL(
                    "INSERT INTO `recurring_transactions_new` " +
                        "(`id`,`accountId`,`amount`,`comment`,`type`,`category`," +
                        "`frequency`,`startDate`,`lastGeneratedDate`,`isPaused`) " +
                        "SELECT `id`,`accountId`,`amount`,`comment`,`type`,`category`," +
                        "`frequency`,`startDate`,`lastGeneratedDate`,`isPaused` " +
                        "FROM `recurring_transactions`"
                )
                db.execSQL("DROP TABLE `recurring_transactions`")
                db.execSQL("ALTER TABLE `recurring_transactions_new` RENAME TO `recurring_transactions`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_transactions_accountId` " +
                        "ON `recurring_transactions` (`accountId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_transactions_customCategoryId` " +
                        "ON `recurring_transactions` (`customCategoryId`)"
                )

                // 3. widget_shortcuts.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `widget_shortcuts_new` (" +
                        "`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, `comment` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                        "`customCategoryId` TEXT, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`customCategoryId`) REFERENCES `custom_categories`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL(
                    "INSERT INTO `widget_shortcuts_new` " +
                        "(`id`,`accountId`,`amount`,`comment`,`type`,`category`) " +
                        "SELECT `id`,`accountId`,`amount`,`comment`,`type`,`category` " +
                        "FROM `widget_shortcuts`"
                )
                db.execSQL("DROP TABLE `widget_shortcuts`")
                db.execSQL("ALTER TABLE `widget_shortcuts_new` RENAME TO `widget_shortcuts`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_widget_shortcuts_accountId` " +
                        "ON `widget_shortcuts` (`accountId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_widget_shortcuts_customCategoryId` " +
                        "ON `widget_shortcuts` (`customCategoryId`)"
                )

                // 4. transactions.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions_new` (" +
                        "`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, `comment` TEXT NOT NULL, " +
                        "`potentiel` INTEGER NOT NULL, `date` INTEGER, " +
                        "`category` TEXT NOT NULL, `sourceRecurringId` TEXT, " +
                        "`customCategoryId` TEXT, `importedCategoryName` TEXT, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`sourceRecurringId`) REFERENCES `recurring_transactions`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE SET NULL , " +
                        "FOREIGN KEY(`customCategoryId`) REFERENCES `custom_categories`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL(
                    "INSERT INTO `transactions_new` " +
                        "(`id`,`accountId`,`amount`,`comment`,`potentiel`,`date`," +
                        "`category`,`sourceRecurringId`) " +
                        "SELECT `id`,`accountId`,`amount`,`comment`,`potentiel`,`date`," +
                        "`category`,`sourceRecurringId` FROM `transactions`"
                )
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_accountId` " +
                        "ON `transactions` (`accountId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_sourceRecurringId` " +
                        "ON `transactions` (`sourceRecurringId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_customCategoryId` " +
                        "ON `transactions` (`customCategoryId`)"
                )
            }
        }
    }
}
