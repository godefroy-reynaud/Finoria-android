# 📁 STRUCTURE_APP.md — Architecture Technique de Finoria Android

> **Version**: 2.0  
> **Dernière mise à jour**: 2026-02-24  
> **Statut**: Production-Ready, AI-Ready  

Ce document est la **carte géographique** de l'application Android. Il est optimisé pour qu'un développeur ou une IA puisse comprendre le projet en une seule lecture.

---

## 🎯 Vue d'Ensemble en 30 Secondes

**Finoria Android** est une application de gestion de finances personnelles construite avec :
- **Jetpack Compose** (100% déclaratif, Material 3)
- **Architecture MAD** avec **Hilt** pour l'injection de dépendances
- **Repository Pattern** : `AccountsRepository` comme source de vérité
- **Persistance DataStore** (JSON via kotlinx.serialization)
- **Services purs** : `RecurrenceEngine`, `CalculationService`, `CsvService`

**Principe clé** : `MainViewModel` est un **orchestrateur léger** injecté via Hilt. Il délègue au `AccountsRepository` pour le CRUD et la persistance, et à `CalculationService` pour les calculs purs.

---

## 📐 Principes d'Architecture

### 1. Boring Architecture is Good Architecture

Pas d'abstractions inutiles. Chaque couche a un rôle clair :

| Couche | Rôle | Exemple |
|--------|------|---------|
| **data/model/** | Data classes sérialisables | `Transaction`, `Account`, `TransactionManager` |
| **data/local/** | Persistance I/O | `StorageService` |
| **data/repository/** | CRUD + orchestration données | `AccountsRepository` |
| **domain/service/** | Logique métier pure, sans état | `CalculationService`, `RecurrenceEngine`, `CsvService` |
| **di/** | Configuration Hilt | `AppModule` |
| **viewmodel/** | État observable + délégation | `MainViewModel` |
| **ui/** | Interface Compose déclarative | `HomeScreen`, `AnalysesScreen` |
| **util/** | Utilitaires partagés | `DateFormatting`, `FormatUtils` |

### 2. Single Source of Truth

```
Composable → appelle méthode → MainViewModel → AccountsRepository → updateManager() → persist()
                                                                                    ↓
                                                               StateFlow émet la nouvelle valeur
```

> ⚠️ **TOUTE modification de données DOIT passer par `MainViewModel` → `AccountsRepository`.**

### 3. Injection de Dépendances (Hilt)

- `FinoriaApp.kt` : `@HiltAndroidApp`
- `MainActivity.kt` : `@AndroidEntryPoint`
- `MainViewModel` : `@HiltViewModel` avec `@Inject constructor`
- `AccountsRepository` : `@Singleton` avec `@Inject constructor`
- `StorageService` : fourni via `AppModule` (`@Provides`)

---

## 📂 Arborescence des Dossiers

```
app/src/main/java/com/finoria/app/
│
├── FinoriaApp.kt                    # @HiltAndroidApp
├── MainActivity.kt                  # @AndroidEntryPoint, setContent → MainScreen
│
├── data/
│   ├── local/
│   │   └── StorageService.kt       # DataStore Preferences + JSON serialization
│   ├── model/
│   │   ├── serializers/
│   │   │   └── Serializers.kt      # UUID, LocalDate, Color serializers
│   │   ├── Account.kt              # data class Account
│   │   ├── AccountStyle.kt         # Enum styles de compte (icon, color, label)
│   │   ├── AnalysesModels.kt       # AnalysisType enum, CategoryData
│   │   ├── RecurrenceFrequency.kt  # DAILY, WEEKLY, MONTHLY, YEARLY
│   │   ├── RecurringTransaction.kt # Transactions récurrentes
│   │   ├── Transaction.kt          # data class Transaction
│   │   ├── TransactionCategory.kt  # Enum catégories (StylableEnum) + guessFrom()
│   │   ├── TransactionManager.kt   # Mutable container par compte
│   │   ├── TransactionType.kt      # INCOME / EXPENSE
│   │   └── WidgetShortcut.kt       # Raccourcis rapides
│   └── repository/
│       └── AccountsRepository.kt   # @Singleton, CRUD + persistance + récurrences
│
├── di/
│   └── AppModule.kt                # @Module @InstallIn(SingletonComponent)
│
├── domain/
│   └── service/
│       ├── CalculationService.kt   # object — Totaux, filtres, pourcentages
│       ├── CsvService.kt           # object — Import/Export CSV via FileProvider
│       └── RecurrenceEngine.kt     # object — Génération des récurrences
│
├── navigation/
│   ├── FinoriaNavHost.kt           # NavHost avec toutes les routes
│   └── Screen.kt                   # sealed class Screen + BottomNavItem enum
│
├── notifications/
│   └── WeeklyReminderWorker.kt     # WorkManager Worker pour rappels hebdo
│
├── ui/
│   ├── MainScreen.kt               # Scaffold + BottomNav + FAB + Sheets
│   ├── account/
│   │   ├── AccountCard.kt          # Carte de compte (AccountPickerSheet)
│   │   ├── AccountPickerSheet.kt   # Bottom sheet sélection de compte
│   │   └── AddAccountSheet.kt      # Création/édition de compte
│   ├── analyses/
│   │   ├── AnalysesPieChart.kt     # Camembert Canvas (drawArc)
│   │   ├── AnalysesScreen.kt       # Contenu analyses
│   │   ├── AnalysesTabScreen.kt    # Tab wrapper avec TopAppBar
│   │   ├── CategoryBreakdownRow.kt # Ligne répartition catégorie
│   │   └── CategoryTransactionsScreen.kt  # Transactions d'une catégorie
│   ├── calendar/
│   │   ├── AllTransactionsScreen.kt    # Toutes transactions (standalone/embedded)
│   │   ├── CalendarContentScreen.kt    # Contenu calendrier
│   │   ├── CalendarTabScreen.kt        # Tab wrapper
│   │   ├── MonthsScreen.kt            # Liste des mois d'une année
│   │   └── TransactionsListScreen.kt  # Transactions d'un mois
│   ├── components/
│   │   ├── CurrencyTextField.kt       # Champ montant formaté
│   │   ├── NoAccountView.kt           # Vue "aucun compte"
│   │   ├── StylableEnum.kt            # Interface StylableEnum
│   │   ├── StyleIconView.kt           # Icône avec fond coloré
│   │   ├── StylePickerGrid.kt         # Grille de sélection de style
│   │   ├── SwipeableTransactionRow.kt # Swipe card → edit/delete underneath
│   │   └── TransactionRow.kt          # Ligne transaction (icon + texte + montant)
│   ├── future/
│   │   ├── FutureTabScreen.kt         # Tab wrapper futur
│   │   └── PotentialTransactionsScreen.kt  # Liste transactions potentielles
│   ├── home/
│   │   ├── CsvImportPreviewScreen.kt  # Prévisualisation import CSV + bouton retour
│   │   ├── HomeComponents.kt          # BalanceHeader, QuickCard
│   │   ├── HomeScreen.kt              # LazyColumn home content
│   │   └── HomeTabScreen.kt           # Tab wrapper + TopAppBar + CSV + modales
│   ├── recurring/
│   │   ├── AddRecurringScreen.kt      # Formulaire récurrence
│   │   └── RecurringGrid.kt           # Grille de récurrences
│   ├── shortcut/
│   │   ├── AddShortcutScreen.kt       # Formulaire raccourci
│   │   └── ShortcutsGrid.kt           # Grille de raccourcis
│   ├── theme/
│   │   ├── Color.kt                   # Palette de couleurs
│   │   ├── Theme.kt                   # Material 3 theme (light/dark)
│   │   └── Type.kt                    # Typographie
│   └── transaction/
│       └── AddTransactionScreen.kt    # Formulaire transaction (ajout/édition)
│
├── util/
│   ├── DateFormatting.kt              # Extensions : dayHeaderFormatted, shortFormatted
│   └── FormatUtils.kt                # Extensions : formattedCurrency
│
└── viewmodel/
    └── MainViewModel.kt              # @HiltViewModel, orchestrateur principal
```

---

## 🔄 Flux de Données

### Architecture en Couches

```
┌─────────────────────────────────────────────────────────────────┐
│                       UI (Compose)                               │
│  MainScreen, HomeTabScreen, AnalysesTabScreen, etc.              │
│  Observent MainViewModel via collectAsStateWithLifecycle()       │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Appelle des méthodes publiques
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              MainViewModel (@HiltViewModel)                      │
│                                                                  │
│  Expose StateFlow : accounts, currentTransactions,               │
│  currentShortcuts, currentRecurring, selectedAccount...          │
│                                                                  │
│  Délègue au repository + CalculationService                      │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│            AccountsRepository (@Singleton)                       │
│                                                                  │
│  MutableStateFlow : _accounts, _transactionManagers,             │
│  _selectedAccountId, _isInitialized                              │
│                                                                  │
│  updateManager() → deep copy → mutate copy → emit → persist()   │
└───────┬──────────┬──────────────┬───────────────────────────────┘
        │          │              │
        ▼          ▼              ▼
 ┌────────────┐┌──────────────┐┌──────────────┐
 │ Storage    ││ Recurrence  ││ Calculation  │
 │ Service    ││ Engine      ││ Service      │
 │            ││             ││              │
 │ save/load  ││ processAll  ││ totalFor...  │
 └─────┬──────┘└──────────────┘└──────────────┘
       │
       ▼
 ┌────────────┐
 │ DataStore  │
 │ Preferences│
 │ (JSON)     │
 └────────────┘
```

### Cycle de Vie d'une Mutation

```kotlin
// Exemple : ajouter une transaction
// 1. UI appelle viewModel.addTransaction(transaction)
// 2. MainViewModel récupère selectedAccountId et délègue :
fun addTransaction(transaction: Transaction) {
    val accountId = selectedAccountId.value ?: return
    viewModelScope.launch { repository.addTransaction(accountId, transaction) }
}

// 3. AccountsRepository.addTransaction appelle updateManager :
suspend fun addTransaction(accountId: UUID, transaction: Transaction) {
    updateManager(accountId) { it.addTransaction(transaction) }
}

// 4. updateManager crée un deep copy AVANT mutation (pour StateFlow detection) :
private suspend fun updateManager(accountId: UUID, action: (TransactionManager) -> Unit) {
    val manager = _transactionManagers.value[accountId] ?: return
    val newManager = manager.copy(
        transactions = manager.transactions.toMutableList(),
        // ... deep copy des listes
    )
    action(newManager)  // mutation sur la copie uniquement
    _transactionManagers.value = newMap  // StateFlow émet (old != new)
    persist()
}
```

---

## 📊 Modèles de Données

### Transaction

```kotlin
@Serializable
data class Transaction(
    val id: @Serializable(UUIDSerializer::class) UUID = UUID.randomUUID(),
    val amount: Double,              // Positif = revenu, Négatif = dépense
    val comment: String = "",
    val potentiel: Boolean = false,  // Transaction future/planifiée
    val date: @Serializable(LocalDateSerializer::class) LocalDate? = null,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val recurringTransactionId: @Serializable(UUIDSerializer::class) UUID? = null
)
```

### Account

```kotlin
@Serializable
data class Account(
    val id: @Serializable(UUIDSerializer::class) UUID = UUID.randomUUID(),
    val name: String,
    val detail: String = "",
    val style: AccountStyle = AccountStyle.WALLET
)
```

### TransactionManager

```kotlin
@Serializable
data class TransactionManager(
    val accountName: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val widgetShortcuts: MutableList<WidgetShortcut> = mutableListOf(),
    val recurringTransactions: MutableList<RecurringTransaction> = mutableListOf()
)
```

> **Note** : `TransactionManager` utilise des `MutableList` pour les mutations internes. Le `AccountsRepository` crée des copies profondes (deep copy via `toMutableList()`) AVANT chaque mutation pour garantir que `StateFlow` détecte les changements (comparaison par `equals()`).

### RecurringTransaction

```kotlin
@Serializable
data class RecurringTransaction(
    val id: @Serializable(UUIDSerializer::class) UUID = UUID.randomUUID(),
    val amount: Double,
    val comment: String = "",
    val type: TransactionType,
    val category: TransactionCategory,
    val frequency: RecurrenceFrequency,
    val startDate: @Serializable(LocalDateSerializer::class) LocalDate,
    val lastGeneratedDate: @Serializable(LocalDateSerializer::class) LocalDate? = null,
    val isPaused: Boolean = false
)
```

---

## ⚙️ Services — Responsabilités

### StorageService (data/local/)

| Méthode | Description |
|---------|-------------|
| `load()` | Charge comptes + TransactionManagers depuis DataStore (JSON) |
| `save(accounts, managers)` | Sérialise et sauvegarde dans DataStore |
| `loadSelectedAccountId()` | Charge l'ID du compte sélectionné |
| `saveSelectedAccountId(id)` | Sauvegarde l'ID du compte sélectionné |

### AccountsRepository (data/repository/)

| Méthode | Description |
|---------|-------------|
| `init()` | Charge les données + process récurrences |
| `addAccount/updateAccount/deleteAccount` | CRUD comptes |
| `addTransaction/updateTransaction/removeTransaction` | CRUD transactions |
| `validateTransaction` | Marque une transaction potentielle comme validée |
| `addShortcut/updateShortcut/removeShortcut` | CRUD raccourcis |
| `addRecurring/updateRecurring/removeRecurring` | CRUD récurrences |
| `togglePauseRecurring` | Pause/reprend une récurrence |
| `importTransactions` | Import batch de transactions (CSV) |
| `processRecurrences` | Déclenche RecurrenceEngine |

### RecurrenceEngine (domain/service/)

| Méthode | Description |
|---------|-------------|
| `processAll(accounts, managers)` | Génère les transactions futures, auto-valide les passées |
| `removePotentialTransactions(id, transactions)` | Supprime les potentielles liées à une récurrence |

### CalculationService (domain/service/)

| Méthode | Description |
|---------|-------------|
| `totalNonPotential(transactions)` | Total des transactions validées |
| `totalPotential(transactions)` | Total des transactions futures |
| `monthlyChangePercentage(transactions)` | Variation mois courant vs précédent |
| `totalForMonth(month, year, transactions)` | Total pour un mois donné |
| `totalForYear(year, transactions)` | Total pour une année |
| `availableYears(transactions)` | Années avec des transactions |
| `validatedTransactions(transactions, year, month)` | Filtre validées par période |
| `potentialTransactions(transactions)` | Filtre les potentielles |
| `getCategoryBreakdown(transactions, type, month, year)` | Répartition par catégorie |

### CsvService (domain/service/)

| Méthode | Description |
|---------|-------------|
| `generateCsv(transactions, accountName, context)` | Exporte CSV → URI FileProvider |
| `importCsv(uri, context)` | Parse CSV → `List<Transaction>` |

---

## 🧭 Navigation

### Structure

- `MainScreen.kt` : Scaffold avec `BottomNavigationBar` (4 onglets) + FAB + modales
- `FinoriaNavHost.kt` : NavHost avec toutes les routes
- `Screen.kt` : sealed class des routes + enum `BottomNavItem`

### Routes Principales (Onglets)

| Route | Screen | Description |
|-------|--------|-------------|
| `home` | HomeTabScreen | Accueil, solde, raccourcis, récurrences |
| `analyses` | AnalysesTabScreen | Camembert, répartition par catégorie |
| `calendar` | CalendarTabScreen | Historique par année / mois |
| `future` | FutureTabScreen | Transactions potentielles |

### Routes Secondaires (Navigation push)

| Route | Screen | Description |
|-------|--------|-------------|
| `allTransactions` | AllTransactionsScreen | Toutes les transactions validées |
| `potential` | PotentialTransactionsScreen | Transactions potentielles (liste) |
| `transactions/{month}/{year}` | TransactionsListScreen | Transactions d'un mois |
| `months/{year}` | MonthsScreen | Mois d'une année |
| `categoryTx/{category}/{month}/{year}` | CategoryTransactionsScreen | Transactions d'une catégorie |

### Modales (Bottom Sheets)

| Modale | Déclencheur | Description |
|--------|------------|-------------|
| AddTransactionScreen | FAB (+) | Ajout/édition transaction |
| AccountPickerSheet | Icône compte | Sélection de compte |
| AddAccountSheet | Bouton dans Account Picker | Création/édition compte |
| AddShortcutScreen | Bouton (+) dans ShortcutsGrid | Ajout/édition raccourci |
| AddRecurringScreen | Bouton (+) dans RecurringGrid | Ajout/édition récurrence |
| CsvImportPreviewScreen | Bouton import CSV | Prévisualisation + confirmation |

---

## 🔄 Logique de Récurrence

> `processRecurringTransactions()` est appelé :
> - Au **lancement** de l'app (dans `repository.init()`)
> - Quand l'app **revient au premier plan** (`LifecycleEventEffect ON_RESUME`)
> - Après chaque **ajout** ou **modification** de récurrence

Le `RecurrenceEngine` effectue :
1. Crée des copies profondes des managers pour éviter les conflits StateFlow
2. Génère les transactions futures (< 1 mois) comme **transactions potentielles**
3. Vérifie les doublons via `recurringTransactionId` + `date`
4. Valide automatiquement les transactions dont la date est **aujourd'hui ou passée**
5. Met à jour `lastGeneratedDate` pour éviter les regénérations

---

## 📱 Stack Technique

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Plateforme | Android 8.0+ (API 26, cible 35) | SDK 35 |
| Langage | Kotlin | 2.0.21 |
| UI | Jetpack Compose Material 3 | BOM 2024.12.01 |
| Graphiques | Canvas API (`drawArc`) | — |
| State | `StateFlow`, `collectAsStateWithLifecycle` | Lifecycle 2.8.7 |
| Navigation | Navigation Compose | 2.8.5 |
| DI | Hilt Android + KSP | 2.59.2 |
| Persistance | DataStore Preferences + kotlinx.serialization | 1.1.1 / 1.7.3 |
| Background | WorkManager | 2.10.0 |
| Build | AGP + KSP | 9.0.1 / 2.0.21-1.0.28 |

---

## ⚠️ APIs Expérimentales

| Composant | Annotation | API utilisée |
|-----------|------------|--------------|
| `MainScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` | ModalBottomSheet, TopAppBar |
| `HomeTabScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` | TopAppBar, ModalBottomSheet |
| `AllTransactionsScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` | TopAppBar |
| `AddTransactionScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` | DatePicker, SegmentedButton |
| `AddRecurringScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` | TopAppBar |
| `AnalysesTabScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` | TopAppBar |

---

## 🧪 Points de Test Critiques

### Services (tests unitaires)
1. `StorageService` : save/load préserve les données (JSON round-trip)
2. `RecurrenceEngine.processAll` : génère correctement, évite les doublons
3. `CalculationService` : totaux, pourcentages, filtres corrects
4. `CsvService` : export/import round-trip

### Repository (tests d'intégration)
5. `addTransaction` → updateManager deep copy → StateFlow émet → persist
6. `deleteAccount` → sélection automatique du suivant
7. `processRecurrences` → deep copy → génération + auto-validation
8. `importTransactions` → batch add → StateFlow émet

### ViewModel
9. `currentTransactions` se met à jour immédiatement après ajout
10. `currentShortcuts` se met à jour immédiatement après ajout

---

*Document généré — Finoria Android v2.0 — 2026-02-24*
