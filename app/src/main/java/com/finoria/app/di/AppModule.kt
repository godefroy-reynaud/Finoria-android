package com.finoria.app.di

import android.content.Context
import com.finoria.app.data.local.StorageService
import com.finoria.app.data.repository.AccountsRepository
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
    fun provideStorageService(
        @ApplicationContext context: Context
    ): StorageService = StorageService(context)

    @Provides
    @Singleton
    fun provideAccountsRepository(
        storageService: StorageService
    ): AccountsRepository = AccountsRepository(storageService)
}
