package com.finoria.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.finoria.app.data.local.entity.CustomCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {

    @Query("SELECT * FROM custom_categories")
    fun observeAll(): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_categories")
    suspend fun getAll(): List<CustomCategoryEntity>

    @Query("SELECT * FROM custom_categories WHERE accountId = :accountId")
    suspend fun getForAccount(accountId: String): List<CustomCategoryEntity>

    @Upsert
    suspend fun upsert(category: CustomCategoryEntity)

    /** Les références (transactions/raccourcis/récurrences) passent à null via FK SET_NULL. */
    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteById(id: String)
}
