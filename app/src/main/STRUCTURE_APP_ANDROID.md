# 📁 STRUCTURE_APP_ANDROID.md — Architecture Technique de Finoria Android

> **Version**: 1.0  
> **Dernière mise à jour**: 2025  
> **Statut**: Production-Ready, AI-Ready  

Ce document est la **carte géographique** de l'application Android. Il est optimisé pour qu'un développeur ou une IA puisse comprendre le projet en une seule lecture.

---

## 🎯 Vue d'Ensemble en 30 Secondes

**Finoria Android** est une application de gestion de finances personnelles construite avec :
- **Jetpack Compose** (100% déclaratif, Material 3)
- **Architecture MAD** (Single Source of Truth via `AppViewModel`)
- **Persistance DataStore** (JSON via kotlinx.serialization)
- **Composition de services** (AppDataStore, RecurrenceEngine, CalculationService, CsvService)

**Principe clé** : `AppViewModel` est un **orchestrateur léger**. Il ne contient aucune logique métier complexe. Il délègue aux services spécialisés et garantit la persistance + mise à jour du StateFlow après chaque mutation.

---

## 📐 Principes d'Architecture

### 1. Boring Architecture is Good Architecture

Pas d'abstractions inutiles. Chaque couche a un rôle clair :

| Couche | Rôle | Exemple |
|--------|------|---------|
| **model/** | Data classes sérialisables | `Transaction`, `Account` |
| **data/** | Persistance et I/O | `AppDataStore`, `CsvService` |
| **domain/** | Logique métier pure, sans état | `CalculationService`, `RecurrenceEngine` |
| **viewmodel/** | État observable + orchestration | `AppViewModel` |
| **ui/** | Interface Compose déclarative | `HomeScreen`, `AnalysesScreen` |
| **utils/** | Utilitaires partagés | `DateExtensions`, `NumberExtensions` |

### 2. Single Source of Truth

```
Composable → appelle méthode → AppViewModel → délègue au Service → saveState() → _uiState.update()
```

> ⚠️ **TOUTE modification de données DOIT passer par `AppViewModel`.**

### 3. Composition over Inheritance

`AppViewModel` orchestre 4 services indépendants :
- `AppDataStore` : persistance DataStore
- `RecurrenceEngine` : génération/validation des transactions récurrentes
- `CalculationService` : tous les calculs financiers (fonctions pures)
- `CsvService` : import/export CSV

---

## 📂 Arborescence des Dossiers

```
app/src/main/java/com/finoria/
│
├── MainActivity.kt              # Point d'entrée, LifecycleObserver
│
├── model/                       # DONNÉES — Structures immuables
│   ├── Account.kt               # data class + AccountStyle enum
│   ├── AppState.kt              # État global sérialisé
│   ├── RecurringTransaction.kt  # + RecurrenceFrequency enum
│   ├── Transaction.kt           # data class + TransactionType enum
│   ├── TransactionCategory.kt   # Enum catégories (StylableEnum)
│   ├── WidgetShortcut.kt        # Raccourci + ShortcutStyle enum
│   └── Serializers.kt           # UUID, LocalDate, Color
│
├── data/                        # LOGIQUE PERSISTANCE
│   ├── AppDataStore.kt          # DataStore Preferences + JSON
│   └── CsvService.kt            # Import/Export CSV
│
├── domain/                      # LOGIQUE MÉTIER — Fonctions pures
│   ├── CalculationService.kt    # Totaux, filtres, pourcentages
│   └── RecurrenceEngine.kt      # Génération des récurrences
│
├── viewmodel/                   # ORCHESTRATION
│   ├── AppViewModel.kt          # StateFlow<AppUiState>, mutations
│   └── AppViewModelFactory.kt   # Factory pour ViewModel
│
├── ui/                          # INTERFACE — Jetpack Compose
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   │
│   ├── navigation/
│   │   ├── AppNavigation.kt     # NavHost + routes + ToastHost
│   │   ├── BottomNavBar.kt      # 4 onglets
│   │   └── Screen.kt            # Routes et icônes
│   │
│   ├── components/
│   │   ├── AnalysesPieChart.kt  # Camembert Canvas
│   │   ├── CurrencyTextField.kt
│   │   ├── EmptyStateView.kt
│   │   ├── StyleIconView.kt
│   │   ├── StylePickerGrid.kt
│   │   ├── ToastHost.kt
│   │   └── TransactionRow.kt
│   │
│   ├── screens/
│   │   ├── account/
│   │   │   └── AddAccountSheet.kt
│   │   ├── analyses/
│   │   │   ├── AnalysesModels.kt
│   │   │   ├── AnalysesScreen.kt
│   │   │   ├── CategoryBreakdownRow.kt
│   │   │   └── CategoryTransactionsScreen.kt
│   │   ├── calendar/
│   │   │   ├── AllTransactionsView.kt
│   │   │   ├── AllTransactionsFullScreen.kt
│   │   │   ├── CalendarScreen.kt
│   │   │   ├── MonthsView.kt
│   │   │   ├── TransactionsListScreen.kt
│   │   ├── future/
│   │   │   └── FutureScreen.kt
│   │   ├── home/
│   │   │   ├── HomeComponents.kt
│   │   │   └── HomeScreen.kt
│   │   ├── recurring/
│   │   │   ├── AddRecurringTransactionScreen.kt
│   │   │   ├── RecurringListScreen.kt
│   │   │   └── RecurringTransactionsGridView.kt
│   │   ├── shortcut/
│   │   │   └── AddShortcutScreen.kt
│   │   └── transaction/
│   │       └── AddTransactionScreen.kt
│   │
│   └── utils/
│       ├── DateExtensions.kt
│       ├── Modifiers.kt
│       ├── NumberExtensions.kt
│       └── StylableEnum.kt
│
└── notifications/
    └── NotificationScheduler.kt # WorkManager + NotificationCompat
```

---

## 🔄 Flux de Données

### Architecture en Couches

```
┌─────────────────────────────────────────────────────────────────┐
│                     UI (Compose)                                 │
│  HomeScreen, AnalysesScreen, CalendarScreen, etc.                │
│  Observent AppViewModel via collectAsState()                     │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Appelle des méthodes publiques
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  AppViewModel (Orchestrateur)                    │
│                                                                  │
│  StateFlow<AppUiState>                                          │
│  accounts, transactionsByAccount, recurringTransactions, ...     │
│                                                                  │
│  Chaque méthode : 1. Muter _uiState  2. dataStore.save()         │
└───────┬──────────┬──────────────┬───────────────┬───────────────┘
        │          │              │               │
        ▼          ▼              ▼               ▼
 ┌────────────┐┌──────────────┐┌──────────────┐┌───────────┐
 │ AppDataStore││ Recurrence  ││ Calculation  ││ CsvService│
 │            ││ Engine      ││ Service      ││           │
 │ save/load  ││ processAll  ││ totalFor...  ││ import/   │
 │            ││ removePot.  ││ getCategory..││ generate  │
 └─────┬──────┘└──────────────┘└──────────────┘└───────────┘
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
fun addTransaction(transaction: Transaction) {
    val currentState = getCurrentAppState()
    val updatedMap = ...
    saveState(currentState.copy(transactionsByAccount = updatedMap))
}

private fun saveState(state: AppState) {
    viewModelScope.launch {
        dataStore.saveAppState(state)
    }
}
```

---

## 📊 Modèles de Données

### Transaction

```kotlin
data class Transaction(
    val id: UUID,
    val amount: Double,           // Positif = revenu, Négatif = dépense
    val comment: String = "",
    val isPotential: Boolean = false,
    val date: LocalDate? = null,
    val category: TransactionCategory,
    val recurringTransactionId: UUID? = null
)
```

### Account

```kotlin
data class Account(
    val id: UUID,
    val name: String,
    val detail: String,
    val style: AccountStyle  // Enum avec icon, color, label
)
```

### RecurringTransaction

```kotlin
data class RecurringTransaction(
    val id: UUID,
    val amount: Double,
    val comment: String,
    val type: TransactionType,      // INCOME / EXPENSE
    val category: TransactionCategory,
    val frequency: RecurrenceFrequency,  // DAILY, WEEKLY, MONTHLY, YEARLY
    val startDate: LocalDate,
    val lastGeneratedDate: LocalDate? = null,
    val isPaused: Boolean = false
)
```

### AppUiState

```kotlin
data class AppUiState(
    val accounts: List<Account>,
    val transactionsByAccount: Map<String, List<Transaction>>,
    val recurringTransactions: List<RecurringTransaction>,
    val shortcuts: List<WidgetShortcut>,
    val selectedAccountId: String?,
    val isLoading: Boolean,
    val toastMessage: String?
)
```

---

## ⚙️ Services — Responsabilités

### AppDataStore

| Méthode | Description |
|---------|-------------|
| `appStateFlow` | Flow qui émet l'AppState à chaque changement |
| `saveAppState(state)` | Encode en JSON → DataStore |

### RecurrenceEngine

| Méthode | Description |
|---------|-------------|
| `processAll(state)` | Génère les transactions futures (<1 mois), auto-valide les passées |
| `removePotentialTransactions(id, transactions)` | Supprime les potentielles liées à une récurrence |

### CalculationService

| Méthode | Description |
|---------|-------------|
| `totalNonPotential(transactions)` | Total des transactions validées |
| `totalPotential(transactions)` | Total des transactions futures |
| `totalForMonth(month, year, transactions)` | Total pour un mois donné |
| `validatedTransactions(year, month, transactions)` | Filtre par année/mois |
| `getCategoryBreakdown(transactions, type)` | Répartition par catégorie |

### CsvService

| Méthode | Description |
|---------|-------------|
| `generateCsv(transactions, accountName)` | Exporte en CSV |
| `importCsv(inputStream)` | Parse CSV → List<Transaction> |
| `saveCsvToFile(context, content)` | Sauvegarde temporaire pour partage |

---

## 🧭 Navigation

### Routes Principales

| Route | Screen | Description |
|-------|--------|-------------|
| `home` | HomeScreen | Accueil, solde, raccourcis, récurrences |
| `analyses` | AnalysesScreen | Camembert, répartition par catégorie |
| `calendar` | CalendarScreen | Jour / Mois / Année |
| `future` | FutureScreen | Transactions potentielles |

### Routes Secondaires

| Route | Description |
|-------|-------------|
| `add_transaction` | Formulaire nouvelle transaction |
| `add_recurring` | Formulaire nouvelle récurrence |
| `edit_recurring/{id}` | Édition récurrence |
| `add_shortcut` | Nouveau raccourci |
| `edit_shortcut/{id}` | Édition raccourci |
| `recurring_list` | Liste des récurrences |
| `all_transactions` | Toutes les transactions |
| `calendar_month/{year}/{month}` | Transactions d'un mois |
| `category_transactions/{name}` | Transactions d'une catégorie |
| `calendar_list/{year}/{month}` | Alias pour calendar_month |

---

## 🔄 Logique de Récurrence

> `processRecurringTransactions()` est appelé :
> - Au **lancement** de l'app
> - Quand l'app **revient au premier plan** (LifecycleObserver)
> - Après chaque **ajout** ou **modification** de récurrence

Le `RecurrenceEngine` effectue :
1. Génère les transactions futures (< 1 mois) comme **transactions potentielles**
2. Vérifie les doublons via `recurringTransactionId` + `date`
3. Valide automatiquement les transactions dont la date est **aujourd'hui ou passée**
4. Met à jour `lastGeneratedDate` pour éviter les regénérations

---

## 📱 Stack Technique

| Composant | Technologie |
|-----------|-------------|
| UI Framework | Jetpack Compose (Material 3) |
| Graphiques | Canvas API (`drawArc`) |
| State Management | `StateFlow`, `collectAsState` |
| Navigation | Navigation Compose |
| Persistance | DataStore Preferences + kotlinx.serialization |
| Notifications | WorkManager + NotificationCompat |
| Partage | Intent.ACTION_SEND + FileProvider |

---

## ⚠️ APIs Expérimentales

Certains écrans et composants utilisent des APIs marquées comme expérimentales. Les annotations `@OptIn` suivantes sont requises :

| Fichier | Annotation | API utilisée |
|---------|------------|--------------|
| `AllTransactionsFullScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` | TopAppBar |
| `RecurringListScreen.kt` | `@OptIn(ExperimentalMaterial3Api::class)` | TopAppBar |
| `ShortcutsGrid` (HomeComponents.kt) | `@OptIn(ExperimentalFoundationApi::class)` | combinedClickable |

Le `ToastHost` utilise `surfaceVariant` / `onSurfaceVariant` pour la compatibilité avec toutes les versions de Material 3.

Le `QuickCard` reçoit `Modifier.weight(1f)` du parent (Row) car `weight` n'est disponible que dans `RowScope`/`ColumnScope`.

---

## 🧪 Points de Test Critiques

### Services (tests unitaires)
1. `AppDataStore` : save/load préserve les données
2. `RecurrenceEngine.processAll` : génère correctement, évite les doublons
3. `CalculationService` : totaux et pourcentages corrects
4. `CsvService` : export/import round-trip

### AppViewModel (tests d'intégration)
5. `addTransaction` → persistance + mise à jour état
6. `deleteAccount` → sélection automatique du suivant
7. `processRecurringTransactions` → génération + auto-validation
8. `pauseRecurringTransaction` / `resumeRecurringTransaction`

---

*Document généré — Finoria Android v1.0*
