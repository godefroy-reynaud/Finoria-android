package com.finoria.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.finoria.app.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAll(): List<RecurringTransactionEntity>

    @Upsert
    suspend fun upsert(recurring: RecurringTransactionEntity)

    /**
     * Supprime la récurrence. Grâce à la ForeignKey `SET_NULL`, les transactions
     * déjà générées sont **conservées** avec `sourceRecurringId = null`.
     */
    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurring_transactions WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
