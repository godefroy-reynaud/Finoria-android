package com.finoria.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.finoria.app.data.local.entity.WidgetShortcutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetShortcutDao {

    @Query("SELECT * FROM widget_shortcuts")
    fun observeAll(): Flow<List<WidgetShortcutEntity>>

    @Query("SELECT * FROM widget_shortcuts")
    suspend fun getAll(): List<WidgetShortcutEntity>

    @Upsert
    suspend fun upsert(shortcut: WidgetShortcutEntity)

    @Query("DELETE FROM widget_shortcuts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM widget_shortcuts WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
