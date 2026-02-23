package com.finoria.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finoria.model.AppState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "finoria_settings")

class AppDataStore(private val context: Context) {
    private val appStateKey = stringPreferencesKey("app_state_json")
    private val json = Json { ignoreUnknownKeys = true }

    val appStateFlow: Flow<AppState> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[appStateKey]
        if (jsonString != null) {
            try {
                json.decodeFromString<AppState>(jsonString)
            } catch (e: Exception) {
                AppState() // Fallback si données corrompues
            }
        } else {
            AppState()
        }
    }

    suspend fun saveAppState(state: AppState) {
        context.dataStore.edit { preferences ->
            preferences[appStateKey] = json.encodeToString(state)
        }
    }
}