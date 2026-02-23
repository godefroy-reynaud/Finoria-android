# 🏗️ ANDROID MIGRATION GUIDE — Finoria iOS → Android

> **Document technique de migration** — Ce fichier sert de référence exhaustive pour recréer l'application Finoria sur Android en Kotlin / Jetpack Compose, en respectant Material Design 3.
>
> **Source analysée** : Projet iOS complet (Swift/SwiftUI) — 33 fichiers, ~4 500 lignes de code.
>
> **Cible** : Android natif — Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture.

---

## Table des matières

1. [Architecture cible](#1-architecture-cible)
2. [Mapping des dossiers](#2-mapping-des-dossiers)
3. [Analyse de la logique métier](#3-analyse-de-la-logique-métier)
4. [UI/UX — SwiftUI vers Jetpack Compose](#4-uiux--swiftui-vers-jetpack-compose)
5. [Gestion d'état & Navigation](#5-gestion-détat--navigation)
6. [Dépendances](#6-dépendances)
7. [Checklist d'implémentation](#7-checklist-dimplémentation)

---

## 1. Architecture cible

### 1.1 Pattern recommandé : MVVM + Clean Architecture (simplifiée)

L'app iOS utilise un **orchestrateur central** (`AccountsManager` : `ObservableObject`) qui fait office de ViewModel + Repository. Sur Android, on découpe proprement en couches :

```
┌─────────────────────────────────┐
│          UI Layer               │
│  Compose Screens + ViewModels   │
├─────────────────────────────────┤
│         Domain Layer            │
│  Use Cases (optionnel ici)      │
│  Models (data class)            │
├─────────────────────────────────┤
│          Data Layer             │
│  Repository + DataStore/Room    │
│  Services (CSV, Recurrence)     │
└─────────────────────────────────┘
```

### 1.2 Principes structurants

| Principe iOS actuel | Équivalent Android |
|---|---|
| `AccountsManager` (ObservableObject, single source of truth) | `AccountsRepository` + `MainViewModel` exposant des `StateFlow` |
| `@Published var` → SwiftUI observe | `MutableStateFlow` / `mutableStateOf` → Compose recompose |
| `StorageService` (UserDefaults + JSON) | `DataStore<Preferences>` ou **Room** (recommandé pour les requêtes complexes) |
| Services purs (`CalculationService`, `CSVService`, `RecurrenceEngine`) | Objets Kotlin (`object` ou classes injectées via Hilt) |
| `@StateObject` dans ContentView | `hiltViewModel()` dans le Composable racine |
| `@ObservedObject` dans les enfants | Paramètre `viewModel` passé ou `hiltViewModel()` scopé |

### 1.3 Injection de dépendances

Utiliser **Hilt** (standard Android) :

```kotlin
// Module Hilt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAccountsRepository(
        storageService: StorageService
    ): AccountsRepository = AccountsRepository(storageService)

    @Provides
    @Singleton
    fun provideStorageService(
        @ApplicationContext context: Context
    ): StorageService = StorageService(context)
}
```

---

## 2. Mapping des dossiers

### 2.1 Structure iOS actuelle → Structure Android cible

Le package racine Android sera `com.finoria.app`.

| Dossier iOS | Fichier(s) iOS | Package Android cible | Fichier(s) Kotlin |
|---|---|---|---|
| `/` | `FinoriaApp.swift` | `com.finoria.app` | `FinoriaApp.kt` (Application class) + `MainActivity.kt` |
| `/` | `Notifications.swift` | `com.finoria.app.notifications` | `NotificationManager.kt`, `NotificationWorker.kt` |
| `Models/` | `Account.swift` | `com.finoria.app.data.model` | `Account.kt` |
| `Models/` | `Transaction.swift` | `com.finoria.app.data.model` | `Transaction.kt` |
| `Models/` | `TransactionCategory.swift` | `com.finoria.app.data.model` | `TransactionCategory.kt` |
| `Models/` | `RecurringTransaction.swift` | `com.finoria.app.data.model` | `RecurringTransaction.kt` |
| `Models/` | `WidgetShortcut.swift` | `com.finoria.app.data.model` | `WidgetShortcut.kt` |
| `Models/` | `TransactionManager.swift` | `com.finoria.app.data.model` | `TransactionManager.kt` |
| `Models/` | `AccountsManager.swift` | `com.finoria.app.data.repository` | `AccountsRepository.kt` |
| `Services/` | `StorageService.swift` | `com.finoria.app.data.local` | `StorageService.kt` |
| `Services/` | `CalculationService.swift` | `com.finoria.app.domain.service` | `CalculationService.kt` |
| `Services/` | `CSVService.swift` | `com.finoria.app.domain.service` | `CsvService.kt` |
| `Services/` | `RecurrenceEngine.swift` | `com.finoria.app.domain.service` | `RecurrenceEngine.kt` |
| `Extensions/` | `DateFormatting.swift` | `com.finoria.app.util` | `DateFormatting.kt` |
| `Extensions/` | `StylableEnum.swift` | `com.finoria.app.ui.components` | `StylableEnum.kt`, `StylePickerGrid.kt`, `StyleIconView.kt` |
| `Extensions/` | `ViewModifiers.swift` | `com.finoria.app.ui.theme` | `Modifiers.kt`, `FormatUtils.kt` |
| `Views/` | `ContentView.swift` | `com.finoria.app.ui` | `MainScreen.kt`, `FinoriaNavHost.kt` |
| `Views/` | `NoAccountView.swift` | `com.finoria.app.ui.components` | `NoAccountView.kt` |
| `Views/` | `DocumentPicker.swift` | `com.finoria.app.util` | *(remplacé par Intent ACTION_OPEN_DOCUMENT)* |
| `Views/Account/` | `AccountCardView.swift` | `com.finoria.app.ui.account` | `AccountCard.kt` |
| `Views/Account/` | `AccountPickerView.swift` | `com.finoria.app.ui.account` | `AccountPickerScreen.kt` |
| `Views/Account/` | `AddAccountSheet.swift` | `com.finoria.app.ui.account` | `AddAccountSheet.kt` |
| `Views/Components/` | `CurrencyTextField.swift` | `com.finoria.app.ui.components` | `CurrencyTextField.kt` |
| `Views/Transactions/` | `AddTransactionView.swift` | `com.finoria.app.ui.transaction` | `AddTransactionScreen.kt` |
| `Views/Transactions/` | `TransactionRow.swift` | `com.finoria.app.ui.transaction` | `TransactionRow.kt` |
| `Views/Widget/` | `AddWidgetShortcutView.swift` | `com.finoria.app.ui.shortcut` | `AddShortcutScreen.kt` |
| `Views/Widget/Toast/` | `ToastCard.swift`, `ToastData.swift`, `ToastView.swift` | `com.finoria.app.ui.components` | `ToastHost.kt` *(remplacé par `Snackbar` Material 3)* |
| `Views/Recurring/` | `AddRecurringTransactionView.swift` | `com.finoria.app.ui.recurring` | `AddRecurringScreen.kt` |
| `Views/Recurring/` | `RecurringTransactionsGridView.swift` | `com.finoria.app.ui.recurring` | `RecurringGrid.kt` |
| `Views/TabView/` | `HomeTabView.swift` | `com.finoria.app.ui.home` | `HomeTabScreen.kt` |
| `Views/TabView/` | `HomeView.swift` | `com.finoria.app.ui.home` | `HomeScreen.kt` |
| `Views/TabView/Home/` | `HomeComponents.swift` | `com.finoria.app.ui.home` | `BalanceHeader.kt`, `QuickCard.kt` |
| `Views/TabView/Home/` | `ShortcutsGridView.swift` | `com.finoria.app.ui.shortcut` | `ShortcutsGrid.kt` |
| `Views/TabView/` | `FutureTabView.swift` | `com.finoria.app.ui.future` | `FutureTabScreen.kt` |
| `Views/TabView/` | `PotentialTransactionsView.swift` | `com.finoria.app.ui.future` | `PotentialTransactionsScreen.kt` |
| `Views/TabView/Analyses/` | `AnalysesModels.swift` | `com.finoria.app.data.model` | `AnalysesModels.kt` |
| `Views/TabView/Analyses/` | `AnalysesPieChart.swift` | `com.finoria.app.ui.analyses` | `AnalysesPieChart.kt` |
| `Views/TabView/Analyses/` | `AnalysesTabView.swift` | `com.finoria.app.ui.analyses` | `AnalysesTabScreen.kt` |
| `Views/TabView/Analyses/` | `AnalysesView.swift` | `com.finoria.app.ui.analyses` | `AnalysesScreen.kt` |
| `Views/TabView/Analyses/` | `CategoryBreakdownRow.swift` | `com.finoria.app.ui.analyses` | `CategoryBreakdownRow.kt` |
| `Views/TabView/Analyses/` | `CategoryTransactionsView.swift` | `com.finoria.app.ui.analyses` | `CategoryTransactionsScreen.kt` |
| `Views/TabView/Calendrier/` | `CalendrierMainView.swift` | `com.finoria.app.ui.calendar` | `CalendarTabScreen.kt` |
| `Views/TabView/Calendrier/` | `CalendrierTabView.swift` | `com.finoria.app.ui.calendar` | `CalendarContentScreen.kt` |
| `Views/TabView/Calendrier/` | `CalendrierRoute.swift` | `com.finoria.app.navigation` | `CalendarRoute.kt` |
| `Views/TabView/Calendrier/` | `AllTransactionsView.swift` | `com.finoria.app.ui.calendar` | `AllTransactionsScreen.kt` |
| `Views/TabView/Calendrier/` | `MonthsView.swift` | `com.finoria.app.ui.calendar` | `MonthsScreen.kt` |
| `Views/TabView/Calendrier/` | `TransactionsListView.swift` | `com.finoria.app.ui.calendar` | `TransactionsListScreen.kt` |

### 2.2 Structure de packages finale

```
com.finoria.app/
├── FinoriaApp.kt                    // @HiltAndroidApp Application
├── MainActivity.kt                  // setContent { FinoriaTheme { MainScreen() } }
├── data/
│   ├── local/
│   │   └── StorageService.kt        // DataStore / Room DAO
│   ├── model/
│   │   ├── Account.kt
│   │   ├── AccountStyle.kt
│   │   ├── Transaction.kt
│   │   ├── TransactionType.kt
│   │   ├── TransactionCategory.kt
│   │   ├── RecurringTransaction.kt
│   │   ├── RecurrenceFrequency.kt
│   │   ├── WidgetShortcut.kt
│   │   ├── TransactionManager.kt
│   │   └── AnalysesModels.kt
│   └── repository/
│       └── AccountsRepository.kt
├── domain/
│   └── service/
│       ├── CalculationService.kt
│       ├── CsvService.kt
│       └── RecurrenceEngine.kt
├── di/
│   └── AppModule.kt                 // Hilt DI module
├── navigation/
│   ├── FinoriaNavHost.kt
│   ├── Screen.kt                    // sealed class de routes
│   └── CalendarRoute.kt
├── notifications/
│   ├── NotificationHelper.kt
│   └── WeeklyReminderWorker.kt      // WorkManager
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── components/
│   │   ├── CurrencyTextField.kt
│   │   ├── StylePickerGrid.kt
│   │   ├── StyleIconView.kt
│   │   ├── NoAccountView.kt
│   │   └── SnackbarHost.kt
│   ├── account/
│   │   ├── AccountCard.kt
│   │   ├── AccountPickerScreen.kt
│   │   └── AddAccountSheet.kt
│   ├── home/
│   │   ├── HomeTabScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── BalanceHeader.kt
│   │   └── QuickCard.kt
│   ├── transaction/
│   │   ├── AddTransactionScreen.kt
│   │   └── TransactionRow.kt
│   ├── shortcut/
│   │   ├── ShortcutsGrid.kt
│   │   └── AddShortcutScreen.kt
│   ├── recurring/
│   │   ├── RecurringGrid.kt
│   │   └── AddRecurringScreen.kt
│   ├── future/
│   │   ├── FutureTabScreen.kt
│   │   └── PotentialTransactionsScreen.kt
│   ├── analyses/
│   │   ├── AnalysesTabScreen.kt
│   │   ├── AnalysesScreen.kt
│   │   ├── AnalysesPieChart.kt
│   │   ├── CategoryBreakdownRow.kt
│   │   └── CategoryTransactionsScreen.kt
│   └── calendar/
│       ├── CalendarTabScreen.kt
│       ├── CalendarContentScreen.kt
│       ├── AllTransactionsScreen.kt
│       ├── MonthsScreen.kt
│       └── TransactionsListScreen.kt
├── util/
│   ├── DateFormatting.kt
│   └── FormatUtils.kt
└── viewmodel/
    └── MainViewModel.kt
```

---

## 3. Analyse de la logique métier

### 3.1 Modèles de données — Conversion Swift → Kotlin

#### 3.1.1 `Account` (struct → data class)

**iOS** : `struct Account: Identifiable, Codable, Equatable` avec un enum `AccountStyle`.

```kotlin
// Account.kt
data class Account(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val detail: String = "",
    val style: AccountStyle = AccountStyle.guessFrom(name)
)
```

- [ ] Créer `Account.kt` — data class avec `UUID`
- [ ] Créer `AccountStyle.kt` — enum class avec `icon`, `color`, `label`, `guessFrom()`

```kotlin
// AccountStyle.kt
enum class AccountStyle(
    val icon: ImageVector,
    val color: Color,
    val label: String
) : StylableEnum {
    BANK(Icons.Outlined.AccountBalance, Color(0xFF2196F3), "Compte courant"),
    SAVINGS(Icons.Outlined.Savings, Color(0xFFFF9800), "Épargne"),
    INVESTMENT(Icons.Outlined.ShowChart, Color(0xFF9C27B0), "Investissements"),
    CARD(Icons.Outlined.CreditCard, Color(0xFF4CAF50), "Carte"),
    CASH(Icons.Outlined.Payments, Color(0xFF00BCD4), "Espèces"),
    PIGGY(Icons.Outlined.CardGiftcard, Color(0xFFE91E63), "Tirelire"),
    WALLET(Icons.Outlined.AccountBalanceWallet, Color(0xFF795548), "Portefeuille"),
    BUSINESS(Icons.Outlined.BusinessCenter, Color(0xFF3F51B5), "Professionnel");

    companion object {
        fun guessFrom(name: String): AccountStyle {
            val text = name.lowercase()
            return when {
                text.containsAny("courant", "principal", "bnp", "société générale", "crédit") -> BANK
                text.containsAny("livret", "épargne", "ldd", "pel") -> SAVINGS
                text.containsAny("invest", "pea", "crypto", "bourse", "action") -> INVESTMENT
                text.containsAny("carte", "revolut", "n26", "lydia") -> CARD
                text.containsAny("espèce", "cash", "liquide") -> CASH
                text.containsAny("tirelire", "économie") -> PIGGY
                text.containsAny("portefeuille", "wallet") -> WALLET
                text.containsAny("pro", "entreprise", "business") -> BUSINESS
                else -> BANK
            }
        }
    }
}

private fun String.containsAny(vararg terms: String): Boolean =
    terms.any { this.contains(it) }
```

#### 3.1.2 `Transaction` (struct immuable → data class)

**iOS** : `struct Transaction` avec méthodes `validated(at:)` et `modified(...)` qui retournent de nouvelles instances.

```kotlin
// Transaction.kt
data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String,
    val potentiel: Boolean = true,
    val date: LocalDate? = null,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val recurringTransactionId: UUID? = null
) {
    /** Retourne une copie validée (non potentielle avec date) */
    fun validated(at: LocalDate = LocalDate.now()): Transaction =
        copy(potentiel = false, date = at)

    /** Retourne une copie modifiée */
    fun modified(
        amount: Double? = null,
        comment: String? = null,
        potentiel: Boolean? = null,
        date: LocalDate? = this.date,
        category: TransactionCategory? = null
    ): Transaction = copy(
        amount = amount ?: this.amount,
        comment = comment ?: this.comment,
        potentiel = potentiel ?: this.potentiel,
        date = date,
        category = category ?: this.category
    )
}
```

> **Note** : utiliser `java.time.LocalDate` au lieu de `java.util.Date`. Minimum API 26 ou `desugaring` pour compat.

- [ ] Créer `Transaction.kt` — data class avec `copy()` natif Kotlin
- [ ] Créer `TransactionType.kt` — enum avec `label`

```kotlin
// TransactionType.kt
enum class TransactionType(val symbol: String, val label: String) {
    INCOME("+", "Revenu"),
    EXPENSE("-", "Dépense")
}
```

#### 3.1.3 `TransactionCategory` (enum avec 20 cases)

- [ ] Créer `TransactionCategory.kt` — enum class implémentant `StylableEnum`

```kotlin
// TransactionCategory.kt
enum class TransactionCategory(
    override val icon: ImageVector,
    override val color: Color,
    override val label: String
) : StylableEnum {
    SALARY(Icons.Outlined.BusinessCenter, Color(0xFF4CAF50), "Salaire"),
    INCOME(Icons.Outlined.ArrowCircleDown, Color(0xFF4CAF50), "Revenu"),
    RENT(Icons.Outlined.Home, Color(0xFFFF9800), "Loyer"),
    UTILITIES(Icons.Outlined.Bolt, Color(0xFFFFEB3B), "Charges"),
    SUBSCRIPTION(Icons.Outlined.PlayArrow, Color(0xFF9C27B0), "Abonnement"),
    PHONE(Icons.Outlined.PhoneAndroid, Color(0xFF3F51B5), "Téléphone"),
    INSURANCE(Icons.Outlined.Shield, Color(0xFF2196F3), "Assurance"),
    FOOD(Icons.Outlined.Restaurant, Color(0xFFFFEB3B), "Restaurant"),
    SHOPPING(Icons.Outlined.ShoppingCart, Color(0xFF2196F3), "Courses"),
    FUEL(Icons.Outlined.LocalGasStation, Color(0xFFFF9800), "Carburant"),
    TRANSPORT(Icons.Outlined.DirectionsCar, Color(0xFF00BCD4), "Transport"),
    LOAN(Icons.Outlined.Percent, Color(0xFFF44336), "Crédit"),
    SAVINGS(Icons.Outlined.Savings, Color(0xFF26A69A), "Épargne"),
    FAMILY(Icons.Outlined.Person, Color(0xFF9C27B0), "Famille"),
    HEALTH(Icons.Outlined.LocalHospital, Color(0xFF26A69A), "Santé"),
    GIFT(Icons.Outlined.CardGiftcard, Color(0xFF3F51B5), "Cadeau"),
    PARTY(Icons.Outlined.Favorite, Color(0xFFE91E63), "Soirée"),
    EXPENSE(Icons.Outlined.ArrowCircleUp, Color(0xFFF44336), "Dépense"),
    OTHER(Icons.Outlined.MoreHoriz, Color(0xFF9E9E9E), "Autre");

    companion object {
        /** Auto-détection identique à l'iOS */
        fun guessFrom(comment: String, type: TransactionType): TransactionCategory {
            val text = comment.lowercase()
            return when {
                text.containsAny("loyer", "appartement", "maison") -> RENT
                text.containsAny("salaire", "paie", "travail") -> SALARY
                text.containsAny("netflix", "spotify", "abonnement", "abo") -> SUBSCRIPTION
                text.containsAny("assurance", "mutuelle") -> INSURANCE
                text.containsAny("crédit", "prêt", "emprunt") -> LOAN
                text.containsAny("edf", "eau", "gaz", "électricité", "charge") -> UTILITIES
                text.containsAny("épargne", "livret", "économie") -> SAVINGS
                text.containsAny("téléphone", "internet", "mobile", "forfait") -> PHONE
                text.containsAny("carburant", "essence", "gasoil") -> FUEL
                text.containsAny("course", "supermarché", "magasin") -> SHOPPING
                text.containsAny("maman", "papa", "famille") -> FAMILY
                text.containsAny("soirée", "bar", "fête") -> PARTY
                text.containsAny("resto", "restaurant", "repas") -> FOOD
                text.containsAny("voiture", "transport", "train", "taxi", "uber", "bus") -> TRANSPORT
                text.containsAny("médecin", "pharmacie", "santé") -> HEALTH
                text.containsAny("cadeau", "anniversaire") -> GIFT
                else -> if (type == TransactionType.INCOME) INCOME else EXPENSE
            }
        }
    }
}
```

#### 3.1.4 `RecurringTransaction`

- [ ] Créer `RecurringTransaction.kt` — data class
- [ ] Créer `RecurrenceFrequency.kt` — enum class

```kotlin
// RecurrenceFrequency.kt
enum class RecurrenceFrequency(val label: String, val shortLabel: String) {
    DAILY("Tous les jours", "Quotidien"),
    WEEKLY("Toutes les semaines", "Hebdo"),
    MONTHLY("Tous les mois", "Mensuel"),
    YEARLY("Tous les ans", "Annuel")
}

// RecurringTransaction.kt
data class RecurringTransaction(
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String,
    val type: TransactionType,
    val category: TransactionCategory = TransactionCategory.guessFrom(comment, type),
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val startDate: LocalDate = LocalDate.now(),
    val lastGeneratedDate: LocalDate? = null,
    val isPaused: Boolean = false
) {
    fun occurrences(from: LocalDate, to: LocalDate): List<LocalDate> { /* même logique */ }
    fun pendingTransactions(): List<Pair<LocalDate, Transaction>> { /* même logique */ }
}
```

#### 3.1.5 `WidgetShortcut`

- [ ] Créer `WidgetShortcut.kt` — data class

```kotlin
data class WidgetShortcut(
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String,
    val type: TransactionType,
    val category: TransactionCategory = TransactionCategory.guessFrom(comment, type)
)
```

#### 3.1.6 `TransactionManager`

- [ ] Créer `TransactionManager.kt` — classe mutable contenant les listes pour un compte

```kotlin
class TransactionManager(val accountName: String) {
    val transactions = mutableListOf<Transaction>()
    val widgetShortcuts = mutableListOf<WidgetShortcut>()
    val recurringTransactions = mutableListOf<RecurringTransaction>()

    fun add(transaction: Transaction) { transactions.add(transaction) }
    fun remove(transaction: Transaction) { transactions.removeAll { it.id == transaction.id } }
    fun update(transaction: Transaction) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index >= 0) transactions[index] = transaction
    }
}
```

### 3.2 Services — Conversion

#### 3.2.1 `StorageService` (UserDefaults → DataStore / Room)

**iOS** : Sérialise un `[AccountData]` en JSON dans `UserDefaults`.

**Recommandation Android** : Utiliser **Room** pour le stockage structuré ou **DataStore + Kotlinx Serialization** pour rester le plus proche de l'architecture iOS.

- [ ] Créer `StorageService.kt`

**Option A — DataStore + JSON (plus proche de l'iOS)** :

```kotlin
class StorageService(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        private val ACCOUNTS_KEY = stringPreferencesKey("accounts_data_v2")
        private val SELECTED_ACCOUNT_KEY = stringPreferencesKey("lastSelectedAccountId")
        private val Context.dataStore by preferencesDataStore(name = "finoria_prefs")
    }

    @Serializable
    data class AccountData(
        val account: Account,
        val transactions: List<Transaction>,
        val widgetShortcuts: List<WidgetShortcut>,
        val recurringTransactions: List<RecurringTransaction>
    )

    suspend fun save(accounts: List<Account>, managers: Map<UUID, TransactionManager>) { ... }
    suspend fun load(): Pair<List<Account>, Map<UUID, TransactionManager>> { ... }
    suspend fun saveSelectedAccountId(id: UUID?) { ... }
    suspend fun loadSelectedAccountId(): UUID? { ... }
}
```

**Option B — Room (recommandé pour la scalabilité)** :

```kotlin
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val detail: String,
    val style: String
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amount: Double,
    val comment: String,
    val potentiel: Boolean,
    val date: Long?,
    val category: String,
    val recurringTransactionId: String?
)
// + entités pour WidgetShortcut, RecurringTransaction
```

> **Décision** : Si vous voulez un portage rapide et fidèle, Option A. Si vous visez la maintenabilité long terme, Option B.

#### 3.2.2 `CalculationService` — Pur, sans dépendance d'état

- [ ] Créer `CalculationService.kt` — `object` Kotlin avec fonctions pures

```kotlin
object CalculationService {
    fun totalNonPotential(transactions: List<Transaction>): Double =
        transactions.filter { !it.potentiel }.sumOf { it.amount }

    fun totalPotential(transactions: List<Transaction>): Double =
        transactions.filter { it.potentiel }.sumOf { it.amount }

    fun availableYears(transactions: List<Transaction>): List<Int> =
        transactions.filter { !it.potentiel }
            .mapNotNull { it.date?.year }
            .distinct()
            .sorted()

    fun totalForYear(year: Int, transactions: List<Transaction>): Double =
        transactions.filter { !it.potentiel && it.date?.year == year }
            .sumOf { it.amount }

    fun totalForMonth(month: Int, year: Int, transactions: List<Transaction>): Double =
        transactions.filter {
            !it.potentiel && it.date?.year == year && it.date?.monthValue == month
        }.sumOf { it.amount }

    fun monthlyChangePercentage(transactions: List<Transaction>): Double? {
        val now = LocalDate.now()
        val currentTotal = totalForMonth(now.monthValue, now.year, transactions)
        val prev = now.minusMonths(1)
        val previousTotal = totalForMonth(prev.monthValue, prev.year, transactions)
        if (previousTotal == 0.0) return null
        return ((currentTotal - previousTotal) / abs(previousTotal)) * 100
    }

    fun potentialTransactions(from: List<Transaction>): List<Transaction> =
        from.filter { it.potentiel }

    fun validatedTransactions(
        from: List<Transaction>,
        year: Int? = null,
        month: Int? = null
    ): List<Transaction> {
        var result = from.filter { !it.potentiel }
        year?.let { y -> result = result.filter { it.date?.year == y } }
        month?.let { m -> result = result.filter { it.date?.monthValue == m } }
        return result
    }
}
```

#### 3.2.3 `CSVService` — Export/Import fichier

- [ ] Créer `CsvService.kt` — export via `FileProvider` + `Intent.ACTION_SEND`, import via `ActivityResultContracts.OpenDocument`

```kotlin
object CsvService {
    fun generateCsv(transactions: List<Transaction>, accountName: String, context: Context): Uri? {
        val sorted = transactions.sortedByDescending { it.date }
        if (sorted.isEmpty()) return null

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE)
        val sb = StringBuilder("Date,Type,Montant,Commentaire,Statut,Catégorie\n")

        for (tx in sorted) {
            val dateStr = tx.date?.format(formatter) ?: "N/A"
            val type = if (tx.amount >= 0) "Revenu" else "Dépense"
            val amount = String.format("%.2f", abs(tx.amount))
            val comment = tx.comment.replace(",", ";")
            val status = if (tx.potentiel) "Potentielle" else "Validée"
            val category = tx.category.label
            sb.appendLine("$dateStr,$type,$amount,$comment,$status,$category")
        }

        // Écrire dans le cache et retourner Uri via FileProvider
        val file = File(context.cacheDir, "${accountName}_transactions_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString())
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun importCsv(uri: Uri, context: Context): List<Transaction> { /* parsing identique */ }
}
```

#### 3.2.4 `RecurrenceEngine` — Moteur de récurrences

- [ ] Créer `RecurrenceEngine.kt` — même logique que l'iOS

```kotlin
object RecurrenceEngine {
    fun processAll(
        accounts: List<Account>,
        managers: Map<UUID, TransactionManager>
    ): Boolean { /* copie logique iOS: générer pending, auto-valider passées */ }

    fun removePotentialTransactions(
        recurringId: UUID,
        transactions: MutableList<Transaction>
    ) {
        transactions.removeAll { it.recurringTransactionId == recurringId && it.potentiel }
    }
}
```

### 3.3 `AccountsRepository` — Remplacement d'`AccountsManager`

- [ ] Créer `AccountsRepository.kt` — classe singleton injectée par Hilt

L'`AccountsManager` iOS cumule Repository + ViewModel. Sur Android, on le sépare :

```kotlin
@Singleton
class AccountsRepository @Inject constructor(
    private val storage: StorageService
) {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _transactionManagers = MutableStateFlow<Map<UUID, TransactionManager>>(emptyMap())
    val transactionManagers: StateFlow<Map<UUID, TransactionManager>> = _transactionManagers.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<UUID?>(null)
    val selectedAccountId: StateFlow<UUID?> = _selectedAccountId.asStateFlow()

    // Méthodes identiques à AccountsManager: addAccount, deleteAccount, addTransaction, etc.
    // Chaque mutation appelle persist() à la fin
    suspend fun init() { /* charge depuis storage */ }
    private suspend fun persist() { /* sauvegarde dans storage */ }
}
```

### 3.4 `MainViewModel`

- [ ] Créer `MainViewModel.kt` — expose les données du Repository en UI State

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AccountsRepository
) : ViewModel() {

    val accounts = repository.accounts
    val selectedAccountId = repository.selectedAccountId

    // Computed state pour l'UI
    val selectedAccount: StateFlow<Account?> = combine(accounts, selectedAccountId) { accs, id ->
        accs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addAccount(account: Account) { viewModelScope.launch { repository.addAccount(account) } }
    fun deleteAccount(account: Account) { viewModelScope.launch { repository.deleteAccount(account) } }
    // ... toutes les autres méthodes proxy
}
```

---

## 4. UI/UX — SwiftUI vers Jetpack Compose

### 4.1 Adaptations Material Design 3

> **Règle fondamentale** : L'app Android ne doit PAS ressembler à une copie d'iOS. Utiliser les composants Material 3 natifs.

| Composant iOS (SwiftUI) | Composant Android (Material 3) | Notes |
|---|---|---|
| `TabView` (5 onglets dont "+" action) | `NavigationBar` (4 onglets) + `FloatingActionButton` | Le bouton "+" devient un FAB flottant, PAS un onglet |
| `.sheet(isPresented:)` | `ModalBottomSheet` ou `NavHost` destination | Préférer BottomSheet pour les formulaires courts |
| `NavigationStack` | `NavHost` + `NavController` | Navigation Compose standard |
| `.navigationTitle("...")` | `TopAppBar(title = { Text("...") })` | `CenterAlignedTopAppBar` ou `LargeTopAppBar` selon le contexte |
| `.toolbar { ToolbarItem }` | `TopAppBar(actions = { ... })` | Actions dans la TopAppBar |
| `List { Section { ... } }` | `LazyColumn` avec `items()` + `Card` ou dividers | Pas de "section header" natif, utiliser `Text` stylisé |
| `Form { Section { ... } }` | `Column` avec `OutlinedTextField`, `Card` groupés | Material 3 ne possède pas de "Form" → composer manuellement |
| `Picker(.segmented)` | `SingleChoiceSegmentedButtonRow` | Material 3 Segmented Buttons |
| `DatePicker(.graphical)` | `DatePicker` (Material 3) ou `DatePickerDialog` | |
| `Toggle` | `Switch` | |
| `Alert` / `.alert()` | `AlertDialog` | |
| `contextMenu { ... }` | Long press → `DropdownMenu` | |
| `.swipeActions()` | `SwipeToDismissBox` (M3) | |
| `LazyVGrid(columns: 2)` | `LazyVerticalGrid(columns = GridCells.Fixed(2))` | |
| `Charts.SectorMark` (Swift Charts) | **Vico** ou **Canvas** custom | Pas d'équivalent natif, voir §6 |
| Toast personnalisé | `Snackbar` via `SnackbarHost` | Material 3 natif |
| `UIActivityViewController` (Share) | `Intent.ACTION_SEND` | |
| `UIDocumentPickerViewController` | `ActivityResultContracts.OpenDocument()` | |
| `UIImpactFeedbackGenerator` (haptic) | `view.performHapticFeedback()` | |
| `UNUserNotificationCenter` | `NotificationManager` + `WorkManager` | |

### 4.2 Écran par écran — Mapping détaillé

#### Écran 1 : `ContentView.swift` → `MainScreen.kt`

**iOS** : `TabView` avec 5 onglets (Home, Analyses, Calendrier, Futur, + bouton d'ajout déguisé en onglet).

**Android** :
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                // 4 onglets : Home, Analyses, Calendrier, Futur
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                // ... (Analyses, Calendrier, Futur)
            }
        },
        floatingActionButton = {
            // Le "+" iOS devient un FAB Android natif
            FloatingActionButton(
                onClick = { showAddTransactionSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(navController, startDestination = "home", Modifier.padding(padding)) {
            composable("home") { HomeTabScreen(viewModel) }
            composable("analyses") { AnalysesTabScreen(viewModel) }
            composable("calendar") { CalendarTabScreen(viewModel) }
            composable("future") { FutureTabScreen(viewModel) }
        }
    }
}
```

- [ ] `MainScreen.kt` — Scaffold + NavigationBar (4 items) + FAB + NavHost
- [ ] Le bouton "+" iOS (Tab avec role `.search`) → **FloatingActionButton** Material 3
- [ ] Les toasts iOS personnalisés → **Snackbar** Material 3

#### Écran 2 : `HomeView.swift` → `HomeScreen.kt`

**iOS** : ScrollView vertical contenant BalanceHeader, 2 QuickCards (NavigationLink), ShortcutsGridView, RecurringTransactionsGridView.

**Android** :
```kotlin
@Composable
fun HomeScreen(viewModel: MainViewModel, navController: NavController) {
    val account by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // En-tête solde
        item {
            BalanceHeader(
                accountName = account?.name,
                totalCurrent = viewModel.totalNonPotential(),
                percentageChange = viewModel.monthlyChangePercentage(),
                onClick = { navController.navigate("allTransactions") }
            )
        }
        // Cartes rapides
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickCard(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Solde du mois",
                    value = currentMonthSolde,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("transactions/$month/$year") }
                )
                QuickCard(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "À venir",
                    value = totalPotential,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("potential") }
                )
            }
        }
        // Grille raccourcis
        item { ShortcutsGrid(shortcuts, onTap, onEdit, onDelete, onAdd) }
        // Grille récurrences
        item { RecurringGrid(recurrings, onEdit, onDelete, onPause, onResume, onAdd) }
    }
}
```

- [ ] `HomeScreen.kt` — LazyColumn + BalanceHeader + QuickCards + ShortcutsGrid + RecurringGrid
- [ ] `BalanceHeader.kt` — titre, solde formaté, indicateur %
- [ ] `QuickCard.kt` — Card Material 3 cliquable

#### Écran 3 : `HomeTabView.swift` → `HomeTabScreen.kt`

**iOS** : NavigationStack wrapping HomeView + toolbar (CSV export/import) + AccountPicker.

**Android** :
```kotlin
@Composable
fun HomeTabScreen(viewModel: MainViewModel) {
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()

    if (selectedAccountId != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        // Boutons Export/Import CSV
                        Row {
                            IconButton(onClick = { shareCSV() }) {
                                Icon(Icons.Default.Share, "Exporter CSV")
                            }
                            IconButton(onClick = { pickCSVFile() }) {
                                Icon(Icons.Default.FileDownload, "Importer CSV")
                            }
                        }
                    },
                    actions = {
                        // Bouton sélection de compte
                        IconButton(onClick = { showAccountPicker = true }) {
                            Icon(Icons.Default.AccountCircle, "Compte")
                        }
                    }
                )
            }
        ) { padding ->
            HomeScreen(viewModel, Modifier.padding(padding))
        }
    } else {
        NoAccountView(onAddAccount = { showAccountPicker = true })
    }
}
```

- [ ] `HomeTabScreen.kt` — Scaffold + TopAppBar avec icons CSV + account picker

#### Écran 4 : `AnalysesView.swift` → `AnalysesScreen.kt`

**iOS** : Pie chart (Swift Charts SectorMark), picker segmenté Dépenses/Revenus, navigateur de mois, liste de catégories.

**Android** :
```kotlin
@Composable
fun AnalysesScreen(viewModel: MainViewModel, navController: NavController) {
    var analysisType by remember { mutableStateOf(AnalysisType.EXPENSES) }
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }

    LazyColumn {
        // Segmented buttons Dépenses / Revenus
        item {
            SingleChoiceSegmentedButtonRow {
                AnalysisType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = analysisType == type,
                        onClick = { analysisType = type },
                        shape = SegmentedButtonDefaults.itemShape(index, AnalysisType.entries.size)
                    ) { Text(type.label) }
                }
            }
        }
        // Navigateur de mois : < Février 2026 >
        item { MonthNavigator(selectedMonth, selectedYear, onPrev, onNext) }
        // Pie Chart (Canvas ou Vico)
        item { AnalysesPieChart(chartData, total, analysisType, selectedSlice) }
        // Liste des catégories
        items(categoryData) { item ->
            CategoryBreakdownRow(item, totalAmount, isSelected, onClick = { navigateToDetail() })
        }
    }
}
```

- [ ] `AnalysesScreen.kt` — SegmentedButtonRow + MonthNavigator + PieChart + CategoryList
- [ ] `AnalysesPieChart.kt` — **Canvas** custom ou bibliothèque **Vico**
- [ ] `CategoryBreakdownRow.kt` — Row avec icône, label, montant, pourcentage
- [ ] `CategoryTransactionsScreen.kt` — Liste groupée par jour

#### Écran 5 : `CalendrierTabView.swift` → `CalendarContentScreen.kt`

**iOS** : Picker segmenté (Jour/Mois/Année) puis contenu conditionnel.

**Android** :
```kotlin
@Composable
fun CalendarContentScreen(viewModel: MainViewModel, navController: NavController) {
    var mode by remember { mutableStateOf(CalendarViewMode.DAY) }

    Column {
        SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 16.dp)) {
            CalendarViewMode.entries.forEachIndexed { index, m ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = { mode = m },
                    shape = SegmentedButtonDefaults.itemShape(index, CalendarViewMode.entries.size)
                ) { Text(m.label) }
            }
        }
        when (mode) {
            CalendarViewMode.DAY -> AllTransactionsScreen(viewModel, embedded = true)
            CalendarViewMode.MONTH -> MonthsListScreen(viewModel, navController)
            CalendarViewMode.YEAR -> YearsListScreen(viewModel, navController)
        }
    }
}
```

- [ ] `CalendarContentScreen.kt` — Segmented buttons + contenu conditionnel
- [ ] `AllTransactionsScreen.kt` — LazyColumn groupée par jour avec headers
- [ ] `MonthsScreen.kt` — Liste des mois d'une année
- [ ] `TransactionsListScreen.kt` — Transactions d'un mois donné

#### Écran 6 : `PotentialTransactionsView.swift` → `PotentialTransactionsScreen.kt`

**iOS** : List avec 2 sections (récurrentes, futures normales) + swipe actions (valider / supprimer).

**Android** :
```kotlin
@Composable
fun PotentialTransactionsScreen(viewModel: MainViewModel) {
    val recurringTx = viewModel.recurringPotentialTransactions()
    val normalTx = viewModel.normalPotentialTransactions()

    LazyColumn {
        if (recurringTx.isNotEmpty()) {
            item { SectionHeader("Transactions récurrentes") }
            items(recurringTx, key = { it.id }) { tx ->
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(),
                    backgroundContent = { /* Supprimer (rouge) / Valider (vert) */ }
                ) {
                    TransactionRow(tx, onClick = { editTransaction(tx) })
                }
            }
        }
        if (normalTx.isNotEmpty()) {
            item { SectionHeader("Futures") }
            items(normalTx, key = { it.id }) { tx ->
                SwipeToDismissBox(...) { TransactionRow(tx, onClick = { editTransaction(tx) }) }
            }
        }
    }
}
```

- [ ] `PotentialTransactionsScreen.kt` — LazyColumn + SwipeToDismissBox
- [ ] `FutureTabScreen.kt` — Scaffold wrapper

#### Écran 7 : `AccountPickerView.swift` → `AccountPickerScreen.kt`

**iOS** : Sheet modale avec List d'AccountCardView + bouton ajouter + contextMenu (modifier/réinitialiser/supprimer).

**Android** : `ModalBottomSheet` Material 3 :
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn {
            items(viewModel.getAllAccounts()) { account ->
                AccountCard(
                    account = account,
                    solde = viewModel.totalNonPotential(account),
                    futur = viewModel.totalNonPotential(account) + viewModel.totalPotential(account),
                    onClick = {
                        viewModel.selectAccount(account.id)
                        onDismiss()
                    },
                    onLongClick = { showContextMenu = true } // DropdownMenu
                )
            }
            item {
                // Bouton Ajouter un compte
                OutlinedButton(onClick = { showAddAccount = true }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter un compte")
                }
            }
        }
    }
}
```

- [ ] `AccountPickerScreen.kt` — ModalBottomSheet + LazyColumn d'AccountCard
- [ ] `AccountCard.kt` — Card Material 3 avec icône colorée, nom, solde

#### Écran 8 : `AddAccountSheet.swift` → `AddAccountSheet.kt`

**iOS** : Form avec TextField (nom, détail), StylePickerGrid, aperçu, bouton supprimer.

**Android** :
```kotlin
@Composable
fun AddAccountScreen(
    viewModel: MainViewModel,
    accountToEdit: Account? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var detail by remember { mutableStateOf(accountToEdit?.detail ?: "") }
    var style by remember { mutableStateOf(accountToEdit?.style ?: AccountStyle.BANK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (accountToEdit != null) "Modifier" else "Nouveau compte") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Fermer") }
                },
                actions = {
                    TextButton(onClick = { save() }) {
                        Text(if (accountToEdit != null) "OK" else "Créer")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 15) name = it },
                label = { Text("Nom du compte") },
                supportingText = { Text("${name.length}/15") }
            )
            OutlinedTextField(
                value = detail,
                onValueChange = { if (it.length <= 20) detail = it },
                label = { Text("Détail (optionnel)") },
                supportingText = { Text("${detail.length}/20") }
            )
            Text("Icône", style = MaterialTheme.typography.titleSmall)
            StylePickerGrid(selected = style, onSelect = { style = it })
            Text("Aperçu", style = MaterialTheme.typography.titleSmall)
            AccountCard(Account(name = name.ifEmpty { "Nouveau compte" }, detail = detail, style = style), 0.0, 0.0)
        }
    }
}
```

- [ ] `AddAccountSheet.kt` — Scaffold + OutlinedTextFields + StylePickerGrid + Aperçu

#### Écran 9 : `AddTransactionView.swift` → `AddTransactionScreen.kt`

**iOS** : Form avec Picker segmenté (type), champ montant/commentaire, StylePickerGrid (catégorie), Toggle potentielle, DatePicker graphique.

**Android** :
```kotlin
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    transactionToEdit: Transaction? = null,
    onDismiss: () -> Unit
) {
    // States...
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Modifier" else "Nouvelle transaction") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                actions = { TextButton(onClick = { save() }) { Text(if (isEdit) "OK" else "Ajouter") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            // Type (segmented)
            SingleChoiceSegmentedButtonRow { /* Revenu / Dépense */ }
            // Montant
            CurrencyTextField(amount, onValueChange = { amount = it })
            // Commentaire
            OutlinedTextField(value = comment, label = { Text("Commentaire") }, supportingText = { Text("${comment.length}/30") })
            // Catégorie
            StylePickerGrid(selected = category, onSelect = { category = it }, columns = 5)
            // Potentielle (Switch)
            Row { Text("Transaction potentielle"); Switch(checked = isPotentiel, onCheckedChange = { isPotentiel = it }) }
            // DatePicker si non potentielle
            if (!isPotentiel) {
                DatePicker(state = datePickerState)
            }
            // Bouton supprimer (mode édition)
            if (isEdit) {
                TextButton(onClick = { delete() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, null); Text("Supprimer")
                }
            }
        }
    }
}
```

- [ ] `AddTransactionScreen.kt` — formulaire complet
- [ ] `CurrencyTextField.kt` — OutlinedTextField avec suffix "€" et filtre numérique

```kotlin
@Composable
fun CurrencyTextField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    placeholder: String = "Montant"
) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { text -> onValueChange(text.toDoubleOrNull()) },
        label = { Text(placeholder) },
        suffix = { Text("€") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}
```

#### Écran 10 : `AddWidgetShortcutView.swift` → `AddShortcutScreen.kt`

- [ ] `AddShortcutScreen.kt` — même structure que AddTransactionScreen sans date/potentiel

#### Écran 11 : `AddRecurringTransactionView.swift` → `AddRecurringScreen.kt`

- [ ] `AddRecurringScreen.kt` — formulaire avec montant, commentaire, type, fréquence (Picker Material 3 `ExposedDropdownMenuBox`), date de début, catégorie

### 4.3 Composants réutilisables à créer

#### `StylePickerGrid` (SwiftUI LazyVGrid → Compose LazyVerticalGrid)

```kotlin
@Composable
fun <T : StylableEnum> StylePickerGrid(
    selected: T,
    onSelect: (T) -> Unit,
    values: Array<T>,
    columns: Int = 4,
    onManualSelection: (() -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.heightIn(max = 400.dp)
    ) {
        items(values) { style ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        onSelect(style)
                        onManualSelection?.invoke()
                    }
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(style.color.copy(alpha = if (selected == style) 0.3f else 0.1f))
                        .then(
                            if (selected == style)
                                Modifier.border(2.dp, style.color, CircleShape)
                            else Modifier
                        )
                ) {
                    Icon(style.icon, null, tint = style.color)
                }
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected == style) style.color else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
```

- [ ] `StylePickerGrid.kt` — grille de sélection générique
- [ ] `StyleIconView.kt` — icône dans cercle coloré

```kotlin
@Composable
fun StyleIconView(style: StylableEnum, size: Dp = 40.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(style.color.copy(alpha = 0.15f))
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = style.label,
            tint = style.color,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}
```

#### `TransactionRow`

```kotlin
@Composable
fun TransactionRow(transaction: Transaction, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StyleIconView(style = transaction.category, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.comment, style = MaterialTheme.typography.bodyLarge)
            transaction.date?.let { date ->
                Text(
                    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            String.format("%.2f €", transaction.amount),
            color = if (transaction.amount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
```

- [ ] `TransactionRow.kt` — Row avec icône, texte, montant coloré

#### `NoAccountView`

```kotlin
@Composable
fun NoAccountView(onAddAccount: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Aucun compte sélectionné",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onAddAccount) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Ajouter un compte")
        }
    }
}
```

- [ ] `NoAccountView.kt`

### 4.4 Mapping des icônes SF Symbols → Material Icons

| SF Symbol iOS | Material Icon Android | Usage |
|---|---|---|
| `building.columns.fill` | `Icons.Outlined.AccountBalance` | Compte courant |
| `banknote.fill` | `Icons.Outlined.Savings` | Épargne |
| `chart.line.uptrend.xyaxis` | `Icons.Outlined.ShowChart` | Investissements |
| `creditcard.fill` | `Icons.Outlined.CreditCard` | Carte |
| `dollarsign.circle.fill` | `Icons.Outlined.Payments` | Espèces |
| `gift.fill` | `Icons.Outlined.CardGiftcard` | Tirelire/Cadeau |
| `wallet.bifold.fill` | `Icons.Outlined.AccountBalanceWallet` | Portefeuille |
| `briefcase.fill` | `Icons.Outlined.BusinessCenter` | Professionnel/Salaire |
| `house.fill` | `Icons.Outlined.Home` | Loyer |
| `bolt.fill` | `Icons.Outlined.Bolt` | Charges |
| `play.rectangle.fill` | `Icons.Outlined.PlayArrow` | Abonnement |
| `iphone` | `Icons.Outlined.PhoneAndroid` | Téléphone |
| `shield.fill` | `Icons.Outlined.Shield` | Assurance |
| `fork.knife` | `Icons.Outlined.Restaurant` | Restaurant |
| `cart.fill` | `Icons.Outlined.ShoppingCart` | Courses |
| `fuelpump.fill` | `Icons.Outlined.LocalGasStation` | Carburant |
| `car.fill` | `Icons.Outlined.DirectionsCar` | Transport |
| `percent` | `Icons.Outlined.Percent` | Crédit |
| `person.fill` | `Icons.Outlined.Person` | Famille |
| `cross.case.fill` | `Icons.Outlined.LocalHospital` | Santé |
| `heart.fill` | `Icons.Outlined.Favorite` | Soirée |
| `arrow.down.circle.fill` | `Icons.Outlined.ArrowCircleDown` | Revenu |
| `arrow.up.circle.fill` | `Icons.Outlined.ArrowCircleUp` | Dépense |
| `ellipsis.circle.fill` | `Icons.Outlined.MoreHoriz` | Autre |
| `house` | `Icons.Outlined.Home` | Tab Home |
| `chart.pie` | `Icons.Outlined.PieChart` | Tab Analyses |
| `calendar` | `Icons.Outlined.CalendarMonth` | Tab Calendrier |
| `clock.arrow.circlepath` | `Icons.Outlined.Update` | Tab Futur |
| `plus.circle.fill` | `Icons.Default.Add` | FAB Ajouter |
| `person.crop.circle` | `Icons.Default.AccountCircle` | Sélection compte |
| `square.and.arrow.up` | `Icons.Default.Share` | Export CSV |
| `square.and.arrow.down` | `Icons.Default.FileDownload` | Import CSV |
| `trash` | `Icons.Default.Delete` | Supprimer |
| `pencil` | `Icons.Default.Edit` | Modifier |
| `pause.circle` / `play.circle` | `Icons.Default.Pause` / `Icons.Default.PlayArrow` | Pause/Resume récurrence |

---

## 5. Gestion d'état & Navigation

### 5.1 Conversion gestion d'état

| iOS | Android |
|---|---|
| `@StateObject var accountsManager = AccountsManager()` | `val viewModel: MainViewModel = hiltViewModel()` |
| `@ObservedObject var accountsManager` | Paramètre `viewModel` ou `hiltViewModel()` |
| `@State private var showSheet = false` | `var showSheet by remember { mutableStateOf(false) }` |
| `@Published var accounts` | `MutableStateFlow<List<Account>>` dans le ViewModel |
| `@Environment(\.dismiss)` | Callback `onDismiss: () -> Unit` ou `navController.popBackStack()` |
| `@Environment(\.scenePhase)` | `LifecycleEventEffect` ou `ProcessLifecycleOwner` |
| `@Binding var value` | Lambda `onValueChange: (T) -> Unit` |
| `objectWillChange.send()` | StateFlow émet automatiquement sur `.value =` |

### 5.2 Pattern StateFlow dans le ViewModel

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AccountsRepository
) : ViewModel() {

    // État observable
    val accounts: StateFlow<List<Account>> = repository.accounts
    val selectedAccountId: StateFlow<UUID?> = repository.selectedAccountId

    // État dérivé
    val selectedAccount: StateFlow<Account?> = combine(accounts, selectedAccountId) { accs, id ->
        accs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dans les Composables :
    // val account by viewModel.selectedAccount.collectAsStateWithLifecycle()
}
```

### 5.3 Navigation — SwiftUI NavigationStack → Jetpack Navigation Compose

#### 5.3.1 Définition des routes

```kotlin
// Screen.kt — sealed class de routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Analyses : Screen("analyses")
    object Calendar : Screen("calendar")
    object Future : Screen("future")

    // Sous-écrans
    object AllTransactions : Screen("allTransactions")
    object PotentialTransactions : Screen("potential")

    // Écrans avec arguments
    data class TransactionsList(val month: Int, val year: Int) : Screen("transactions/{month}/{year}") {
        companion object {
            const val ROUTE = "transactions/{month}/{year}"
        }
    }
    data class MonthsList(val year: Int) : Screen("months/{year}") {
        companion object {
            const val ROUTE = "months/{year}"
        }
    }
    data class CategoryTransactions(val category: String, val month: Int, val year: Int) : Screen("categoryTx/{category}/{month}/{year}") {
        companion object {
            const val ROUTE = "categoryTx/{category}/{month}/{year}"
        }
    }
}
```

#### 5.3.2 NavHost

```kotlin
@Composable
fun FinoriaNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = Screen.Home.route, modifier = modifier) {
        // Onglets principaux
        composable(Screen.Home.route) { HomeTabScreen(viewModel, navController) }
        composable(Screen.Analyses.route) { AnalysesTabScreen(viewModel, navController) }
        composable(Screen.Calendar.route) { CalendarTabScreen(viewModel, navController) }
        composable(Screen.Future.route) { FutureTabScreen(viewModel, navController) }

        // Sous-écrans
        composable(Screen.AllTransactions.route) { AllTransactionsScreen(viewModel, navController) }
        composable(Screen.PotentialTransactions.route) { PotentialTransactionsScreen(viewModel) }

        // Écrans paramétrés
        composable(
            Screen.TransactionsList.ROUTE,
            arguments = listOf(
                navArgument("month") { type = NavType.IntType },
                navArgument("year") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val month = backStackEntry.arguments?.getInt("month") ?: return@composable
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            TransactionsListScreen(viewModel, month, year, navController)
        }

        composable(
            Screen.MonthsList.ROUTE,
            arguments = listOf(navArgument("year") { type = NavType.IntType })
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            MonthsScreen(viewModel, year, navController)
        }

        composable(
            Screen.CategoryTransactions.ROUTE,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("month") { type = NavType.IntType },
                navArgument("year") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val categoryStr = backStackEntry.arguments?.getString("category") ?: return@composable
            val category = TransactionCategory.valueOf(categoryStr)
            val month = backStackEntry.arguments?.getInt("month") ?: return@composable
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            CategoryTransactionsScreen(viewModel, category, month, year, navController)
        }
    }
}
```

#### 5.3.3 Modales / Sheets

**iOS** utilise `.sheet(isPresented:)` et `.sheet(item:)`. Sur Android :

```kotlin
// Pattern pour les modales/sheets
var showAddTransaction by remember { mutableStateOf(false) }
var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

if (showAddTransaction || transactionToEdit != null) {
    ModalBottomSheet(onDismissRequest = {
        showAddTransaction = false
        transactionToEdit = null
    }) {
        AddTransactionScreen(
            viewModel = viewModel,
            transactionToEdit = transactionToEdit,
            onDismiss = {
                showAddTransaction = false
                transactionToEdit = null
            }
        )
    }
}
```

> **Alternative** : utiliser `Dialog(onDismissRequest = {})` pour les formulaires longs, ou une destination navigable en full-screen.

### 5.4 Lifecycle — scenePhase → Lifecycle

**iOS** : `onChange(of: scenePhase)` dans ContentView pour traiter les récurrences au retour au premier plan.

**Android** :
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    // Équivalent de scenePhase == .active
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.processRecurringTransactions()
    }

    // ... reste du code
}
```

---

## 6. Dépendances

### 6.1 Bibliothèques iOS utilisées → Équivalents Android

| Bibliothèque iOS | Usage | Équivalent Android | Artifact Gradle |
|---|---|---|---|
| **Aucune bibliothèque tierce** | L'app est 100% native | — | — |
| `SwiftUI` (framework natif) | UI | **Jetpack Compose** | `androidx.compose.ui:ui`, `androidx.compose.material3:material3` |
| `Charts` (Swift Charts, natif iOS 16+) | Graphique camembert | **Vico** (recommandé) ou Canvas custom | `com.patrykandpatrick.vico:compose-m3:2.x` |
| `UserNotifications` (natif) | Notifications locales | **WorkManager** + NotificationCompat | `androidx.work:work-runtime-ktx:2.x` |
| `Foundation` (JSON Codable) | Sérialisation | **Kotlinx Serialization** ou Gson | `org.jetbrains.kotlinx:kotlinx-serialization-json:1.x` |
| `UserDefaults` (natif) | Persistance clé-valeur | **DataStore** | `androidx.datastore:datastore-preferences:1.x` |
| `UniformTypeIdentifiers` (natif) | Sélection fichiers | **ActivityResult API** | `androidx.activity:activity-compose:1.x` |
| `UIKit.UIActivityViewController` | Partage | **Intent.ACTION_SEND** | Natif Android |
| `UIKit.UIImpactFeedbackGenerator` | Retour haptique | `view.performHapticFeedback()` | Natif Android |

### 6.2 `build.gradle.kts` (module app) — Dépendances recommandées

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    kotlin("kapt")
}

android {
    namespace = "com.finoria.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.finoria.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore (persistance)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Kotlinx Serialization (JSON)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Charts (Pie Chart)
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-beta.2")

    // WorkManager (notifications programmées)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Activity Result / File Picker
    implementation("androidx.activity:activity-compose:1.9.3")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## 7. Checklist d'implémentation

### Phase 1 — Setup du projet

- [ ] Créer le projet Android Studio (Empty Compose Activity)
- [ ] Configurer `build.gradle.kts` avec toutes les dépendances (§6.2)
- [ ] Créer la structure de packages (§2.2)
- [ ] Configurer Hilt (`@HiltAndroidApp`, `AppModule.kt`)
- [ ] Configurer le thème Material 3 (`Theme.kt`, `Color.kt`, `Type.kt`)
- [ ] Créer `MainActivity.kt` avec `setContent { FinoriaTheme { MainScreen() } }`

### Phase 2 — Data Layer (modèles + persistance)

- [ ] `TransactionType.kt` — enum
- [ ] `TransactionCategory.kt` — enum avec icônes Material, couleurs, labels, `guessFrom()`
- [ ] `AccountStyle.kt` — enum avec icônes, couleurs, labels, `guessFrom()`
- [ ] `StylableEnum.kt` — interface commune `icon`, `color`, `label`
- [ ] `Account.kt` — data class
- [ ] `Transaction.kt` — data class avec `validated()`, `modified()`
- [ ] `RecurrenceFrequency.kt` — enum
- [ ] `RecurringTransaction.kt` — data class avec `occurrences()`, `pendingTransactions()`
- [ ] `WidgetShortcut.kt` — data class
- [ ] `TransactionManager.kt` — classe mutable
- [ ] `AnalysesModels.kt` — `CategoryData`, `AnalysisType`, `CategoryDetailRoute`
- [ ] `StorageService.kt` — DataStore + Kotlinx Serialization (ou Room)
- [ ] `AccountsRepository.kt` — singleton avec StateFlow, logique CRUD complète

### Phase 3 — Domain Layer (services)

- [ ] `CalculationService.kt` — object avec fonctions pures
- [ ] `CsvService.kt` — export/import CSV via FileProvider
- [ ] `RecurrenceEngine.kt` — moteur de récurrences
- [ ] `DateFormatting.kt` — extension `monthName(Int)`
- [ ] `FormatUtils.kt` — `compactAmount()`, `formattedCurrency`

### Phase 4 — ViewModel

- [ ] `MainViewModel.kt` — expose StateFlow, délègue au Repository

### Phase 5 — Navigation

- [ ] `Screen.kt` — sealed class de routes
- [ ] `FinoriaNavHost.kt` — NavHost avec toutes les destinations
- [ ] `CalendarRoute.kt` — routes calendrier

### Phase 6 — UI Components (réutilisables)

- [ ] `StylePickerGrid.kt` — grille de sélection générique
- [ ] `StyleIconView.kt` — icône dans cercle coloré
- [ ] `CurrencyTextField.kt` — OutlinedTextField avec suffix €
- [ ] `TransactionRow.kt` — ligne de transaction
- [ ] `AccountCard.kt` — carte de compte
- [ ] `NoAccountView.kt` — état vide
- [ ] `SnackbarHost.kt` — remplacement des toasts iOS par Snackbar M3

### Phase 7 — Écrans principaux

- [ ] `MainScreen.kt` — Scaffold + NavigationBar + FAB + NavHost
- [ ] `HomeTabScreen.kt` — TopAppBar CSV + AccountPicker
- [ ] `HomeScreen.kt` — LazyColumn (BalanceHeader + QuickCards + Grilles)
- [ ] `BalanceHeader.kt` — solde + indicateur %
- [ ] `QuickCard.kt` — carte cliquable
- [ ] `ShortcutsGrid.kt` — grille de raccourcis 2 colonnes
- [ ] `RecurringGrid.kt` — grille de récurrences 2 colonnes

### Phase 8 — Écrans Analyses

- [ ] `AnalysesTabScreen.kt` — wrapper NavigationStack
- [ ] `AnalysesScreen.kt` — SegmentedButtons + MonthNavigator + PieChart + CategoryList
- [ ] `AnalysesPieChart.kt` — Canvas ou Vico pie chart
- [ ] `CategoryBreakdownRow.kt` — ligne catégorie
- [ ] `CategoryTransactionsScreen.kt` — transactions par catégorie groupées par jour

### Phase 9 — Écrans Calendrier

- [ ] `CalendarTabScreen.kt` — wrapper
- [ ] `CalendarContentScreen.kt` — SegmentedButtons Jour/Mois/Année
- [ ] `AllTransactionsScreen.kt` — toutes transactions groupées par jour
- [ ] `MonthsScreen.kt` — mois d'une année
- [ ] `TransactionsListScreen.kt` — transactions d'un mois

### Phase 10 — Écrans Futur

- [ ] `FutureTabScreen.kt` — wrapper
- [ ] `PotentialTransactionsScreen.kt` — 2 sections avec SwipeToDismiss

### Phase 11 — Formulaires (Sheets/Dialogs)

- [ ] `AccountPickerScreen.kt` — ModalBottomSheet avec liste de comptes
- [ ] `AddAccountSheet.kt` — formulaire création/édition compte
- [ ] `AddTransactionScreen.kt` — formulaire création/édition transaction
- [ ] `AddShortcutScreen.kt` — formulaire création/édition raccourci
- [ ] `AddRecurringScreen.kt` — formulaire création/édition récurrence

### Phase 12 — Fonctionnalités annexes

- [ ] `NotificationHelper.kt` — gestion des permissions notification
- [ ] `WeeklyReminderWorker.kt` — WorkManager pour notification hebdomadaire (dimanche 20h)
- [ ] Export CSV — `Intent.ACTION_SEND` avec `FileProvider`
- [ ] Import CSV — `ActivityResultContracts.OpenDocument()`
- [ ] Haptic feedback — `LocalView.current.performHapticFeedback()` sur tap raccourci
- [ ] Traitement récurrences au retour premier plan — `LifecycleEventEffect(ON_RESUME)`

### Phase 13 — Polish & Tests

- [ ] Thème dynamique (Material You / Dynamic Color)
- [ ] Support Mode Sombre
- [ ] Animations de transition (`AnimatedContent`, `animateContentSize`)
- [ ] Tests unitaires `CalculationService`, `RecurrenceEngine`
- [ ] Tests UI Compose basiques
- [ ] Gestion back button Android (predictive back)
- [ ] Support des locales (formatage €, dates en français)
- [ ] ProGuard / R8 rules pour Kotlinx Serialization

---

## Annexe A — Conversion rapide SwiftUI → Compose

| SwiftUI | Jetpack Compose |
|---|---|
| `VStack(spacing: 16) { ... }` | `Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { ... }` |
| `HStack(spacing: 12) { ... }` | `Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ... }` |
| `ZStack { ... }` | `Box { ... }` |
| `ScrollView { VStack { ... } }` | `Column(Modifier.verticalScroll(rememberScrollState()))` |
| `List { ForEach(items) { ... } }` | `LazyColumn { items(list) { ... } }` |
| `LazyVGrid(columns: [GridItem(.flexible())], count: 2)` | `LazyVerticalGrid(columns = GridCells.Fixed(2))` |
| `Spacer()` | `Spacer(Modifier.weight(1f))` ou `Spacer(Modifier.height(x.dp))` |
| `Text("...").font(.title2.bold())` | `Text("...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)` |
| `Image(systemName: "house")` | `Icon(Icons.Outlined.Home, contentDescription = null)` |
| `.foregroundStyle(.secondary)` | `color = MaterialTheme.colorScheme.onSurfaceVariant` |
| `.background(Color(.systemBackground))` | `Modifier.background(MaterialTheme.colorScheme.surface)` |
| `.clipShape(RoundedRectangle(cornerRadius: 16))` | `Modifier.clip(RoundedCornerShape(16.dp))` |
| `.shadow(radius: 8)` | `Modifier.shadow(8.dp, RoundedCornerShape(16.dp))` |
| `.opacity(0.5)` | `Modifier.alpha(0.5f)` ou `.copy(alpha = 0.5f)` (Color) |
| `.padding(16)` | `Modifier.padding(16.dp)` |
| `.frame(width: 48, height: 48)` | `Modifier.size(48.dp)` |
| `.frame(maxWidth: .infinity)` | `Modifier.fillMaxWidth()` |
| `.onTapGesture { }` | `Modifier.clickable { }` |
| `.onAppear { }` | `LaunchedEffect(Unit) { }` |
| `.onChange(of: value) { }` | `LaunchedEffect(value) { }` ou `snapshotFlow { value }.collect { }` |
| `withAnimation(.spring()) { }` | `animateXAsState()` ou `AnimatedContent` |
| `DispatchQueue.main.asyncAfter(deadline:)` | `delay()` dans `LaunchedEffect` / coroutine |
| `.sheet(isPresented: $show)` | `if (show) { ModalBottomSheet { } }` |
| `.alert(isPresented: $show)` | `if (show) { AlertDialog { } }` |
| `.confirmationDialog` | `if (show) { AlertDialog(...) }` |
| `@State private var x = false` | `var x by remember { mutableStateOf(false) }` |
| `Button { } label: { Label("...", icon) }` | `Button(onClick = {}) { Icon(...); Text("...") }` |
| `NavigationLink(destination: View)` | `navController.navigate("route")` |
| `Color(UIColor.systemGroupedBackground)` | `MaterialTheme.colorScheme.surfaceContainerLow` |
| `Color(UIColor.secondarySystemGroupedBackground)` | `MaterialTheme.colorScheme.surfaceContainer` |

## Annexe B — Notifications locales

**iOS** utilise `UNUserNotificationCenter` avec un `UNCalendarNotificationTrigger` hebdomadaire (dimanche 20h).

**Android** — Utiliser `WorkManager` avec un `PeriodicWorkRequest` :

```kotlin
// WeeklyReminderWorker.kt
class WeeklyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Rappel - Finoria")
            .setContentText("As-tu acheté quelque chose cette semaine ?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "weekly_reminder"
        const val NOTIFICATION_ID = 1001
    }
}

// Dans FinoriaApp.kt (Application.onCreate)
fun scheduleWeeklyReminder(context: Context) {
    // Calculer le délai jusqu'au prochain dimanche 20h
    val now = LocalDateTime.now()
    var nextSunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .withHour(20).withMinute(0).withSecond(0)
    if (nextSunday.isBefore(now)) {
        nextSunday = nextSunday.plusWeeks(1)
    }
    val delay = Duration.between(now, nextSunday).toMillis()

    val request = PeriodicWorkRequestBuilder<WeeklyReminderWorker>(7, TimeUnit.DAYS)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork("weekly_reminder", ExistingPeriodicWorkPolicy.KEEP, request)
}
```

- [ ] Créer le `NotificationChannel` dans `FinoriaApp.onCreate()`
- [ ] Demander la permission `POST_NOTIFICATIONS` (API 33+)
- [ ] Programmer le `PeriodicWorkRequest` hebdomadaire

---

> **Fin du guide** — Ce document couvre l'intégralité du projet Finoria iOS (33 fichiers, ~4 500 lignes). Chaque modèle, service, vue et composant a été analysé et mappé vers son équivalent Android/Kotlin/Compose natif avec Material Design 3.
