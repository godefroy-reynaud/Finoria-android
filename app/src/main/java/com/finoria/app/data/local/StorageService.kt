package com.finoria.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finoria.app.data.model.Account
import com.finoria.app.data.model.TransactionManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "finoria_prefs")

/**
 * Petites préférences (DataStore) : compte sélectionné, flag de migration.
 * Sert aussi à **lire** l'ancienne persistance JSON (pré-Room) pour la
 * migration one-shot vers Room — voir AccountsRepository.migrateLegacyJsonIfNeeded.
 */
@Singleton
class StorageService @Inject constructor(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val ACCOUNTS_KEY = stringPreferencesKey("accounts_data_v2")
        private val SELECTED_ACCOUNT_KEY = stringPreferencesKey("lastSelectedAccountId")
        private val MIGRATED_TO_ROOM_KEY = booleanPreferencesKey("migrated_to_room_v1")
    }

    /** Migration JSON → Room déjà effectuée ? (one-shot, évite de ré-importer après un reset). */
    suspend fun hasMigratedToRoom(): Boolean =
        context.dataStore.data.first()[MIGRATED_TO_ROOM_KEY] ?: false

    suspend fun setMigratedToRoom() {
        context.dataStore.edit { prefs -> prefs[MIGRATED_TO_ROOM_KEY] = true }
    }

    @Serializable
    data class AccountData(
        val account: Account,
        val manager: TransactionManager
    )

    /**
     * Charge les comptes et leurs données depuis l'ancienne persistance JSON
     * (lecture seule — n'existe que pour la migration one-shot vers Room).
     */
    suspend fun load(): Pair<List<Account>, Map<UUID, TransactionManager>> {
        val prefs = context.dataStore.data.first()
        val jsonString = prefs[ACCOUNTS_KEY] ?: return Pair(emptyList(), emptyMap())
        return try {
            val dataList = json.decodeFromString<List<AccountData>>(jsonString)
            val accounts = dataList.map { it.account }
            val managers = dataList.associate { it.account.id to it.manager }
            Pair(accounts, managers)
        } catch (e: Exception) {
            Pair(emptyList(), emptyMap())
        }
    }

    /**
     * Sauvegarde l'ID du compte sélectionné.
     */
    suspend fun saveSelectedAccountId(id: UUID?) {
        context.dataStore.edit { prefs ->
            if (id != null) {
                prefs[SELECTED_ACCOUNT_KEY] = id.toString()
            } else {
                prefs.remove(SELECTED_ACCOUNT_KEY)
            }
        }
    }

    /**
     * Charge l'ID du dernier compte sélectionné.
     */
    suspend fun loadSelectedAccountId(): UUID? {
        val prefs = context.dataStore.data.first()
        val idString = prefs[SELECTED_ACCOUNT_KEY] ?: return null
        return try {
            UUID.fromString(idString)
        } catch (e: Exception) {
            null
        }
    }
}
