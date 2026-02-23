package com.finoria.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finoria.app.data.model.Account
import com.finoria.app.data.model.TransactionManager
import com.finoria.app.data.model.serializers.UUIDSerializer
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "finoria_prefs")

/**
 * Service de persistance utilisant DataStore + Kotlinx Serialization (JSON).
 * Approche fidèle au StorageService iOS (UserDefaults + JSON).
 */
@Singleton
class StorageService @Inject constructor(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    companion object {
        private val ACCOUNTS_KEY = stringPreferencesKey("accounts_data_v2")
        private val SELECTED_ACCOUNT_KEY = stringPreferencesKey("lastSelectedAccountId")
    }

    @Serializable
    data class AccountData(
        val account: Account,
        val manager: TransactionManager
    )

    /**
     * Sauvegarde l'intégralité des comptes et leurs données.
     */
    suspend fun save(
        accounts: List<Account>,
        managers: Map<@Serializable(with = UUIDSerializer::class) UUID, TransactionManager>
    ) {
        val dataList = accounts.map { account ->
            AccountData(
                account = account,
                manager = managers[account.id] ?: TransactionManager(accountName = account.name)
            )
        }
        context.dataStore.edit { prefs ->
            prefs[ACCOUNTS_KEY] = json.encodeToString(dataList)
        }
    }

    /**
     * Charge les comptes et leurs données depuis le DataStore.
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
