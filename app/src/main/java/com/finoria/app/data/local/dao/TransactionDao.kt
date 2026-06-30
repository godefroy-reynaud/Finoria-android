package com.finoria.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.finoria.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)

    /** Supprime uniquement les occurrences **potentielles** générées par une récurrence. */
    @Query("DELETE FROM transactions WHERE sourceRecurringId = :recurringId AND potentiel = 1")
    suspend fun deletePotentialForRecurring(recurringId: String)
}
