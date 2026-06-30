package com.finoria.app.di

import android.content.Context
import androidx.room.Room
import com.finoria.app.data.local.FinoriaDatabase
import com.finoria.app.data.local.StorageService
import com.finoria.app.data.local.dao.AccountDao
import com.finoria.app.data.local.dao.RecurringTransactionDao
import com.finoria.app.data.local.dao.TransactionDao
import com.finoria.app.data.local.dao.WidgetShortcutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinoriaDatabase =
        Room.databaseBuilder(context, FinoriaDatabase::class.java, FinoriaDatabase.NAME)
            // Room active `PRAGMA foreign_keys=ON` : CASCADE / SET_NULL appliqués.
            // Pas de fallbackToDestructiveMigration : zéro perte de données.
            .build()

    @Provides
    fun provideAccountDao(db: FinoriaDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: FinoriaDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideRecurringTransactionDao(db: FinoriaDatabase): RecurringTransactionDao =
        db.recurringTransactionDao()

    @Provides
    fun provideWidgetShortcutDao(db: FinoriaDatabase): WidgetShortcutDao = db.widgetShortcutDao()

    @Provides
    @Singleton
    fun provideStorageService(@ApplicationContext context: Context): StorageService =
        StorageService(context)
}
