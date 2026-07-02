package com.finoria.app.data.repository

import com.finoria.app.data.local.StorageService
import com.finoria.app.data.local.dao.AccountDao
import com.finoria.app.data.local.dao.CustomCategoryDao
import com.finoria.app.data.local.dao.RecurringTransactionDao
import com.finoria.app.data.local.dao.TransactionDao
import com.finoria.app.data.local.dao.WidgetShortcutDao
import com.finoria.app.data.local.entity.toDomain
import com.finoria.app.data.local.entity.toEntity
import com.finoria.app.data.model.Account
import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.RecurringTransaction
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.data.model.TransactionManager
import com.finoria.app.data.model.WidgetShortcut
import com.finoria.app.domain.service.RecurrenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository central — remplace AccountsManager iOS. **Seul chemin d'écriture.**
 *
 * Persistance : **Room** (source de vérité). La réactivité vient des `Flow` Room,
 * ré-émis automatiquement après chaque écriture — l'UI se rafraîchit toute seule
 * (plus de hack `dataVersion`).
 *
 * L'API publique (StateFlow + suspend fun) est volontairement **identique** à la
 * version JSON précédente : ViewModel et UI n'ont pas à changer. Les modèles de
 * domaine restent les types exposés ; les entités Room vivent derrière les mappers.
 */
@Singleton
class AccountsRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val recurringDao: RecurringTransactionDao,
    private val shortcutDao: WidgetShortcutDao,
    private val customCategoryDao: CustomCategoryDao,
    private val storage: StorageService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ─── Observable State (assemblé depuis les Flow Room) ────────────

    private val accountsFlow = accountDao.observeAll()

    val accounts: StateFlow<List<Account>> = accountsFlow
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Vue par compte, reconstruite à chaque émission Room. Conserve le type
     * `Map<UUID, TransactionManager>` attendu par le ViewModel.
     */
    val transactionManagers: StateFlow<Map<UUID, TransactionManager>> = combine(
        accountsFlow,
        transactionDao.observeAll(),
        shortcutDao.observeAll(),
        recurringDao.observeAll(),
        customCategoryDao.observeAll(),
    ) { accountEntities, txEntities, shortcutEntities, recurringEntities, customCategoryEntities ->
        accountEntities.associate { account ->
            UUID.fromString(account.id) to TransactionManager(
                accountName = account.name,
                transactions = txEntities.asSequence()
                    .filter { it.accountId == account.id }
                    .map { it.toDomain() }
                    .toMutableList(),
                widgetShortcuts = shortcutEntities.asSequence()
                    .filter { it.accountId == account.id }
                    .map { it.toDomain() }
                    .toMutableList(),
                recurringTransactions = recurringEntities.asSequence()
                    .filter { it.accountId == account.id }
                    .map { it.toDomain() }
                    .toMutableList(),
                customCategories = customCategoryEntities.asSequence()
                    .filter { it.accountId == account.id }
                    .map { it.toDomain() }
                    .toMutableList(),
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    private val _selectedAccountId = MutableStateFlow<UUID?>(null)
    val selectedAccountId: StateFlow<UUID?> = _selectedAccountId.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // ─── Initialization ──────────────────────────────────────────────

    suspend fun init() {
        migrateLegacyJsonIfNeeded()

        val all = accountDao.getAll()
        val stored = storage.loadSelectedAccountId()
        _selectedAccountId.value =
            stored?.takeIf { id -> all.any { it.id == id.toString() } }
                ?: all.firstOrNull()?.let { UUID.fromString(it.id) }

        processRecurrences()
        _isInitialized.value = true
    }

    /**
     * Import unique des données de l'ancienne persistance JSON (DataStore) vers Room.
     * Ne s'exécute qu'une seule fois (flag persistant), pour ne pas ressusciter des
     * comptes supprimés après un reset.
     */
    private suspend fun migrateLegacyJsonIfNeeded() {
        if (storage.hasMigratedToRoom()) return

        val (legacyAccounts, legacyManagers) = storage.load()
        for (account in legacyAccounts) {
            accountDao.upsert(account.toEntity())
            val manager = legacyManagers[account.id] ?: continue

            // Récurrences d'abord : elles sont la cible FK de transactions.sourceRecurringId.
            val recurringIds = manager.recurringTransactions.mapTo(HashSet()) { it.id }
            manager.recurringTransactions.forEach { recurringDao.upsert(it.toEntity(account.id)) }
            manager.widgetShortcuts.forEach { shortcutDao.upsert(it.toEntity(account.id)) }
            manager.transactions.forEach { tx ->
                // Neutralise une référence pendante éventuelle pour satisfaire la FK.
                val safe = if (tx.recurringTransactionId != null &&
                    tx.recurringTransactionId !in recurringIds
                ) tx.copy(recurringTransactionId = null) else tx
                transactionDao.upsert(safe.toEntity(account.id))
            }
        }
        storage.setMigratedToRoom()
    }

    // ─── Account CRUD ────────────────────────────────────────────────

    suspend fun addAccount(account: Account) {
        accountDao.upsert(account.toEntity())
        // Sélectionne toujours le compte fraîchement créé.
        selectAccount(account.id)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.upsert(account.toEntity())
    }

    suspend fun deleteAccount(account: Account) {
        accountDao.deleteById(account.id.toString()) // CASCADE supprime le contenu
        if (_selectedAccountId.value == account.id) {
            val next = accountDao.getAll().firstOrNull()?.let { UUID.fromString(it.id) }
            selectAccount(next)
        }
    }

    /**
     * Réinitialise un compte : supprime **uniquement les transactions** et met
     * les récurrences **en pause**. Les raccourcis (widgets) et les récurrences
     * elles-mêmes sont conservés.
     */
    suspend fun resetAccount(account: Account) {
        val id = account.id.toString()
        transactionDao.deleteForAccount(id)
        recurringDao.pauseForAccount(id)
    }

    suspend fun selectAccount(id: UUID?) {
        _selectedAccountId.value = id
        storage.saveSelectedAccountId(id)
    }

    // ─── Transaction CRUD ────────────────────────────────────────────

    suspend fun addTransaction(accountId: UUID, transaction: Transaction) {
        transactionDao.upsert(transaction.toEntity(accountId))
    }

    suspend fun updateTransaction(accountId: UUID, transaction: Transaction) {
        transactionDao.upsert(transaction.toEntity(accountId))
    }

    suspend fun removeTransaction(accountId: UUID, transaction: Transaction) {
        transactionDao.deleteById(transaction.id.toString())
    }

    suspend fun validateTransaction(accountId: UUID, transaction: Transaction) {
        transactionDao.upsert(transaction.validated().toEntity(accountId))
    }

    // ─── Shortcut CRUD ───────────────────────────────────────────────

    suspend fun addShortcut(accountId: UUID, shortcut: WidgetShortcut) {
        shortcutDao.upsert(shortcut.toEntity(accountId))
    }

    suspend fun updateShortcut(accountId: UUID, shortcut: WidgetShortcut) {
        shortcutDao.upsert(shortcut.toEntity(accountId))
    }

    suspend fun removeShortcut(accountId: UUID, shortcut: WidgetShortcut) {
        shortcutDao.deleteById(shortcut.id.toString())
    }

    // ─── Recurring CRUD ──────────────────────────────────────────────

    suspend fun addRecurring(accountId: UUID, recurring: RecurringTransaction) {
        recurringDao.upsert(recurring.toEntity(accountId))
        processRecurrences()
    }

    suspend fun updateRecurring(accountId: UUID, recurring: RecurringTransaction) {
        transactionDao.deletePotentialForRecurring(recurring.id.toString())
        recurringDao.upsert(recurring.toEntity(accountId))
        processRecurrences()
    }

    suspend fun removeRecurring(accountId: UUID, recurring: RecurringTransaction) {
        transactionDao.deletePotentialForRecurring(recurring.id.toString())
        // SET_NULL : l'historique déjà validé est conservé (sourceRecurringId → null).
        recurringDao.deleteById(recurring.id.toString())
    }

    suspend fun togglePauseRecurring(accountId: UUID, recurring: RecurringTransaction) {
        val updated = recurring.copy(isPaused = !recurring.isPaused)
        if (updated.isPaused) {
            transactionDao.deletePotentialForRecurring(recurring.id.toString())
            recurringDao.upsert(updated.toEntity(accountId))
        } else {
            recurringDao.upsert(updated.toEntity(accountId))
            processRecurrences()
        }
    }

    // ─── Custom category CRUD ────────────────────────────────────────

    /**
     * Crée une catégorie personnalisée puis **rattache automatiquement** les
     * transactions du compte importées avec ce libellé (celles dont
     * `importedCategoryName` correspond au nom normalisé) — « rattachement différé ».
     */
    suspend fun addCustomCategory(accountId: UUID, category: CustomCategory) {
        customCategoryDao.upsert(category.toEntity(accountId))
        relinkImportedTransactions(accountId, category)
    }

    /** Mise à jour nom/symbole/couleur + rattachement différé (comme la création). */
    suspend fun updateCustomCategory(accountId: UUID, category: CustomCategory) {
        customCategoryDao.upsert(category.toEntity(accountId))
        relinkImportedTransactions(accountId, category)
    }

    /**
     * Suppression = **nullify** : les transactions/raccourcis/récurrences qui la
     * référençaient voient leur `customCategoryId` remis à null par la FK SET_NULL
     * (elles retombent sur la catégorie par défaut `Autre`).
     */
    suspend fun removeCustomCategory(category: CustomCategory) {
        customCategoryDao.deleteById(category.id.toString())
    }

    /**
     * Rattache à [category] toutes les transactions du compte dont le
     * `importedCategoryName` (encore présent) a le même nom normalisé.
     */
    private suspend fun relinkImportedTransactions(accountId: UUID, category: CustomCategory) {
        val key = CustomCategory.normalizeName(category.name)
        transactionDao.getForAccount(accountId.toString())
            .filter { entity ->
                entity.importedCategoryName
                    ?.let { CustomCategory.normalizeName(it) == key } == true
            }
            .forEach { entity ->
                transactionDao.upsert(
                    entity.copy(
                        customCategoryId = category.id.toString(),
                        category = TransactionCategory.OTHER,
                        importedCategoryName = null,
                    )
                )
            }
    }

    // ─── Bulk operations ─────────────────────────────────────────────

    /**
     * Commit de l'import CSV. Les transactions portant un `importedCategoryName`
     * (libellé inconnu des catégories par défaut) sont rattachées à une catégorie
     * personnalisée **résolue ou créée automatiquement** (symbole/couleur par
     * défaut), dé-dupliquée par nom normalisé — y compris entre lignes du même
     * import. Le champ temporaire est effacé une fois le rattachement fait.
     */
    suspend fun importTransactions(accountId: UUID, transactions: List<Transaction>) {
        val existing = customCategoryDao.getForAccount(accountId.toString())
        val cache = existing
            .associateBy { CustomCategory.normalizeName(it.name) }
            .toMutableMap()

        val resolved = transactions.map { tx ->
            val label = tx.importedCategoryName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@map tx.copy(importedCategoryName = null)

            val key = CustomCategory.normalizeName(label)
            val categoryEntity = cache.getOrPut(key) {
                val created = CustomCategory(name = label).toEntity(accountId)
                customCategoryDao.upsert(created)
                created
            }
            tx.copy(
                customCategoryId = UUID.fromString(categoryEntity.id),
                category = TransactionCategory.OTHER,
                importedCategoryName = null,
            )
        }

        transactionDao.insertAll(resolved.map { it.toEntity(accountId) })
    }

    // ─── Recurring processing ────────────────────────────────────────

    suspend fun processRecurrences() {
        val accountEntities = accountDao.getAll()
        if (accountEntities.isEmpty()) return

        val txEntities = transactionDao.getAll()
        val recEntities = recurringDao.getAll()

        val existingTxIds = txEntities.mapTo(HashSet()) { it.id }
        val recurringLastGeneratedBefore = recEntities.associate { it.id to it.lastGeneratedDate }

        val accountsDomain = accountEntities.map { it.toDomain() }
        val managers = accountEntities.associate { account ->
            UUID.fromString(account.id) to TransactionManager(
                accountName = account.name,
                transactions = txEntities.asSequence()
                    .filter { it.accountId == account.id }
                    .map { it.toDomain() }
                    .toMutableList(),
                recurringTransactions = recEntities.asSequence()
                    .filter { it.accountId == account.id }
                    .map { it.toDomain() }
                    .toMutableList(),
            )
        }

        val modified = RecurrenceEngine.processAll(accountsDomain, managers)
        if (!modified) return

        for ((accountId, manager) in managers) {
            manager.transactions.forEach { tx ->
                if (tx.id.toString() !in existingTxIds) {
                    transactionDao.upsert(tx.toEntity(accountId))
                }
            }
            manager.recurringTransactions.forEach { recurring ->
                val before = recurringLastGeneratedBefore[recurring.id.toString()]
                if (before != recurring.lastGeneratedDate) {
                    recurringDao.upsert(recurring.toEntity(accountId))
                }
            }
        }
    }
}
