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
import kotlin.text.get

private val Context.dataStore by preferencesDataStore(name = "finoria_settings")

class AppDataStore(private val context: Context) {
    private val appStateKey = stringPreferencesKey("app_state_json")
    private val json = Json { ignoreUnknownKeys = true }

    val appStateFlow: Flow<AppState> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[appStateKey]
        if (jsonString != null) {
            json.decodeFromString<AppState>(jsonString)
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