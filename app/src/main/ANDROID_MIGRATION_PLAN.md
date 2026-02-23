# 📱 ANDROID_MIGRATION_PLAN.md — Migration Finoria iOS → Android
> **Version**: 1.0  
> **Référence iOS**: Finoria v3.1  
> **Stack Android cible**: Kotlin + Jetpack Compose + MAD (Modern Android Development)  
> **Dépendances tierces**: 0 (uniquement les librairies Jetpack officielles Google)

---

## 1. 🔄 Mapping Technologique — iOS vers Android

| Concept iOS | Technologie iOS | Équivalent Android | Technologie Android |
|---|---|---|---|
| UI Framework | SwiftUI | Jetpack Compose | `@Composable` functions |
| State local | `@State` | État local Compose | `remember { mutableStateOf() }` |
| Observable global | `@ObservedObject` / `ObservableObject` | ViewModel observé | `StateFlow` + `collectAsStateWithLifecycle()` |
| Source de vérité | `AccountsManager` (ObservableObject) | `AppViewModel` (ViewModel) | `ViewModel` + `StateFlow` |
| Persistance | `UserDefaults` + `Codable` (JSON) | DataStore (Preferences) + JSON (Gson/kotlinx.serialization) ou **Room** | `DataStore<Preferences>` ou `Room` |
| Navigation | `NavigationStack` + `TabView` | Navigation Compose | `NavController` + `NavHost` + `BottomNavigation` |
| Graphiques | Swift Charts (`SectorMark`) | Vega-Lite ou dessin Canvas natif | **Canvas API Compose** (PieChart custom, 0 dépendance) |
| Notifications locales | `UNUserNotificationCenter` | NotificationManager | `WorkManager` + `NotificationCompat` |
| Sélecteur de fichiers | `UIDocumentPickerViewController` | Activity Result API | `ActivityResultContracts.OpenDocument` |
| Partage fichier | `UIActivityViewController` | Android ShareSheet | `Intent.ACTION_SEND` + `FileProvider` |
| Struct immuable | `struct` + `modified()` | Data class Kotlin | `data class` + `.copy()` |
| Enum stylisé | `protocol StylableEnum` | Interface Kotlin scellée | `interface StylableEnum` + `sealed class` / `enum class` |
| Extensions utilitaires | `Extension Date`, `Extension Double` | Extension functions Kotlin | `fun Date.dayHeaderFormatted()`, `fun Double.formattedCurrency()` |
| View Modifiers | `ViewModifier` protocol | Composable wrappers | Composables réutilisables + `Modifier` |
| Injection de dépendance | `@EnvironmentObject` | ViewModel partagé | `viewModel()` hissé au NavGraph |
| Tests unitaires | XCTest | JUnit 4/5 + Turbine | `@Test` + `kotlinx.coroutines.test` |

---

### Décision Persistance : DataStore + JSON vs Room

Pour coller au maximum à la philosophie iOS (UserDefaults + JSON, zéro schéma relationnel), nous utiliserons **DataStore (Proto ou Preferences) + kotlinx.serialization (JSON)**. C'est le mapping le plus direct, sans overhead relationnel. Si des besoins de requêtage complexe apparaissent plus tard, une migration vers Room sera aisée.

---

## 2. 🗂️ Arborescence Complète des Packages Android

```
app/
└── src/
    └── main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── values/
        │   │   ├── strings.xml         # Toutes les chaînes localisées
        │   │   ├── colors.xml
        │   │   └── themes.xml
        │   └── xml/
        │       └── file_paths.xml      # FileProvider pour partage CSV
        │
        └── java/com/finoria/
            │
            ├── FinoriaApp.kt           # Application class (@HiltAndroidApp si DI manuelle, sinon simple)
            ├── MainActivity.kt         # Activité unique, hôte du NavHost Compose
            │
            ├── model/                  # 📦 MODÈLES DE DONNÉES (équiv. Models/)
            │   ├── Account.kt              # data class Account + enum AccountStyle
            │   ├── Transaction.kt          # data class Transaction + enum TransactionType
            │   ├── RecurringTransaction.kt # data class RecurringTransaction + enum RecurrenceFrequency
            │   ├── TransactionCategory.kt  # enum TransactionCategory (icône, couleur, label)
            │   └── WidgetShortcut.kt       # data class WidgetShortcut
            │
            ├── data/                   # 📦 COUCHE DATA (équiv. Services/ persistance)
            │   ├── AppDataStore.kt         # Wrapper DataStore : save/load JSON (équiv. StorageService)
            │   └── CsvService.kt           # Import / Export CSV (équiv. CSVService)
            │
            ├── domain/                 # 📦 COUCHE DOMAINE (logique métier pure)
            │   ├── RecurrenceEngine.kt     # Génération/validation des récurrences (équiv. RecurrenceEngine)
            │   └── CalculationService.kt   # Calculs financiers purs (équiv. CalculationService)
            │
            ├── viewmodel/              # 📦 VIEWMODELS (équiv. AccountsManager)
            │   └── AppViewModel.kt         # Orchestrateur : StateFlow, mutations, délégation aux services
            │
            ├── ui/                     # 📦 INTERFACE UTILISATEUR (équiv. Views/)
            │   │
            │   ├── theme/                  # Thème Material 3
            │   │   ├── Color.kt
            │   │   ├── Theme.kt
            │   │   └── Type.kt
            │   │
            │   ├── navigation/             # Équiv. ContentView.swift + CalendrierRoute
            │   │   ├── AppNavigation.kt        # NavHost principal + routes
            │   │   └── BottomNavBar.kt          # Barre de navigation 4 onglets
            │   │
            │   ├── components/             # Équiv. Components/ + Extensions ViewModifiers
            │   │   ├── CurrencyTextField.kt     # Champ montant avec devise
            │   │   ├── StylePickerGrid.kt       # Grille sélection icône/couleur (générique)
            │   │   ├── StyleIconView.kt         # Icône ronde colorée
            │   │   ├── TransactionRow.kt        # Ligne d'affichage transaction
            │   │   ├── AccountCardView.kt       # Carte visuelle d'un compte
            │   │   ├── ToastHost.kt             # Toast / Snackbar éphémère
            │   │   └── EmptyStateView.kt        # État vide (aucun compte)
            │   │
            │   ├── screens/
            │   │   │
            │   │   ├── home/               # Équiv. TabView/Home
            │   │   │   ├── HomeScreen.kt           # Écran principal (solde, raccourcis, récurrences)
            │   │   │   ├── HomeComponents.kt       # BalanceHeader, QuickCard, ShortcutsGrid
            │   │   │   └── HomeViewModel.kt        # (optionnel, si logique dérivée spécifique)
            │   │   │
            │   │   ├── analyses/           # Équiv. TabView/Analyses
            │   │   │   ├── AnalysesScreen.kt       # Vue principale (navigation mois + camembert)
            │   │   │   ├── PieChartCanvas.kt       # Camembert custom via Canvas API
            │   │   │   ├── CategoryBreakdownRow.kt # Ligne détaillée par catégorie
            │   │   │   └── CategoryTransactionsScreen.kt # Transactions d'une catégorie
            │   │   │
            │   │   ├── calendar/           # Équiv. TabView/Calendrier
            │   │   │   ├── CalendarScreen.kt       # Wrapper + Segmented Control (Jour/Mois/Année)
            │   │   │   ├── AllTransactionsView.kt  # Transactions groupées par jour
            │   │   │   ├── MonthsView.kt           # Liste des mois d'une année
            │   │   │   └── TransactionsListScreen.kt # Transactions d'un mois
            │   │   │
            │   │   ├── future/             # Équiv. TabView/FutureTabView
            │   │   │   └── FutureScreen.kt         # Transactions potentielles + récurrentes futures
            │   │   │
            │   │   ├── account/            # Équiv. Views/Account
            │   │   │   ├── AddAccountSheet.kt      # Bottom Sheet création/édition compte
            │   │   │   └── AccountPickerSheet.kt   # Sélecteur de compte (Bottom Sheet)
            │   │   │
            │   │   ├── transaction/        # Équiv. Views/Transactions
            │   │   │   └── AddTransactionScreen.kt # Formulaire ajout/édition transaction
            │   │   │
            │   │   ├── recurring/          # Équiv. Views/Recurring
            │   │   │   ├── AddRecurringTransactionScreen.kt
            │   │   │   └── RecurringTransactionsGridView.kt
            │   │   │
            │   │   └── shortcut/           # Équiv. Views/Widget
            │   │       └── AddShortcutScreen.kt    # Formulaire création/édition raccourci
            │   │
            │   └── utils/                  # Équiv. Extensions/
            │       ├── DateExtensions.kt       # fun Date.dayHeaderFormatted(), monthName()
            │       ├── NumberExtensions.kt     # fun Double.formattedCurrency, compactAmount()
            │       └── StylableEnum.kt         # Interface StylableEnum (icon, color, label)
            │
            └── notifications/          # Équiv. Notifications.swift
                └── NotificationScheduler.kt    # WorkManager + NotificationCompat
```

---

## 3. 🏗️ Mapping des Responsabilités

| Fichier iOS | Fichier Android | Rôle |
|---|---|---|
| `AccountsManager.swift` | `AppViewModel.kt` | Orchestrateur central, Single Source of Truth |
| `StorageService.swift` | `AppDataStore.kt` | Persistance (DataStore + JSON) |
| `RecurrenceEngine.swift` | `RecurrenceEngine.kt` | Logique de génération des récurrences |
| `CalculationService.swift` | `CalculationService.kt` | Calculs financiers purs |
| `CSVService.swift` | `CsvService.kt` | Import / Export CSV |
| `ContentView.swift` | `AppNavigation.kt` + `BottomNavBar.kt` | Navigation et onglets |
| `ViewModifiers.swift` | `ui/components/` + `ui/utils/` | Composants et utilitaires partagés |
| `StylableEnum.swift` | `StylableEnum.kt` + `StylePickerGrid.kt` | Interface générique + composant |

---

## 4. 📋 Plan d'Action — Étapes de Développement

### Étape 1 — Modèles de données (`model/`)
Créer toutes les `data class` et `enum class` Kotlin.  
Objectif : avoir une représentation immuable et sérialisable de chaque entité métier.

- `Account.kt` → `data class Account` + `enum class AccountStyle : StylableEnum`
- `Transaction.kt` → `data class Transaction` + `enum class TransactionType`
- `RecurringTransaction.kt` → `data class RecurringTransaction` + `enum class RecurrenceFrequency`
- `TransactionCategory.kt` → `enum class TransactionCategory : StylableEnum`
- `WidgetShortcut.kt` → `data class WidgetShortcut`
- `StylableEnum.kt` → `interface StylableEnum`

**Convention clé** : toute mutation se fait via `.copy()`, jamais de `var` dans les data classes (sauf cas justifié).

---

### Étape 2 — Couche Data (`data/`)
Mettre en place la persistance locale et l'import/export CSV.

- `AppDataStore.kt` → Wrapper autour de `DataStore<Preferences>` ou fichier JSON dans `filesDir`. Expose des `suspend fun save(...)` et `suspend fun load(): AppState`.
- `CsvService.kt` → Lecture/écriture de fichiers CSV via `BufferedReader` / `BufferedWriter` natif.

---

### Étape 3 — Couche Domaine (`domain/`)
Services purs, sans état, sans Android dependency (testables en JVM pur).

- `CalculationService.kt` → `object CalculationService` avec fonctions statiques pures.
- `RecurrenceEngine.kt` → `object RecurrenceEngine` : génération, dédoublonnage, auto-validation.

---

### Étape 4 — ViewModel (`viewmodel/`)
Orchestrateur central qui remplace `AccountsManager`.

- `AppViewModel.kt` → `class AppViewModel : ViewModel()` 
  - Expose `val uiState: StateFlow<AppUiState>`
  - Charge les données via `viewModelScope.launch`
  - Délègue à `AppDataStore`, `RecurrenceEngine`, `CalculationService`, `CsvService`
  - Chaque mutation suit le pattern : `_uiState.update { ... }` puis `dataStore.save(...)`

---

### Étape 5 — Thème et Navigation (`ui/theme/` + `ui/navigation/`)
- Définir les couleurs, typographies Material 3.
- Mettre en place `NavHost` avec les 4 routes principales + routes secondaires (détail catégorie, liste mois, etc.).
- `BottomNavBar.kt` avec 4 onglets (Home, Analyses, Calendrier, Futur).

---

### Étape 6 — Composants partagés (`ui/components/` + `ui/utils/`)
Les briques réutilisables avant de construire les écrans.

- `CurrencyTextField.kt`, `TransactionRow.kt`, `AccountCardView.kt`
- `StylePickerGrid.kt`, `StyleIconView.kt`
- `ToastHost.kt`, `EmptyStateView.kt`
- `DateExtensions.kt`, `NumberExtensions.kt`

---

### Étape 7 — Écrans principaux (`ui/screens/home/`, `ui/screens/future/`)
Les deux onglets les plus utilisés.

- `HomeScreen.kt` : solde, raccourcis rapides, récurrences du mois, navigation.
- `FutureScreen.kt` : liste des transactions potentielles + récurrentes futures (swipe to validate/delete).

---

### Étape 8 — Écrans Analyses (`ui/screens/analyses/`)
- `AnalysesScreen.kt` : navigation temporelle + segmented control Dépenses/Revenus.
- `PieChartCanvas.kt` : camembert interactif dessiné avec `Canvas` Compose (0 librairie graphique).
- `CategoryBreakdownRow.kt` + `CategoryTransactionsScreen.kt`.

---

### Étape 9 — Écran Calendrier (`ui/screens/calendar/`)
- `CalendarScreen.kt` : segmented control Jour/Mois/Année.
- `AllTransactionsView.kt`, `MonthsView.kt`, `TransactionsListScreen.kt`.

---

### Étape 10 — Sheets & Formulaires
Bottom Sheets et formulaires d'édition.

- `AddAccountSheet.kt`, `AccountPickerSheet.kt`
- `AddTransactionScreen.kt`
- `AddRecurringTransactionScreen.kt`, `RecurringTransactionsGridView.kt`
- `AddShortcutScreen.kt`

---

### Étape 11 — Notifications & CSV I/O
- `NotificationScheduler.kt` : notification hebdomadaire via `WorkManager`.
- Intégration de l'import CSV (Activity Result API + FileProvider).
- Intégration de l'export CSV (partage via `Intent.ACTION_SEND`).

---

### Étape 12 — Tests
- Tests unitaires des services (`CalculationService`, `RecurrenceEngine`, `AppDataStore`).
- Tests du ViewModel avec `kotlinx.coroutines.test` + `Turbine`.
- Tests UI avec `ComposeTestRule` pour les parcours critiques.

---

## 5. 🧱 État Global — Structure de `AppUiState`

```kotlin
data class AppUiState(
    val accounts: List<Account> = emptyList(),
    val transactionsByAccount: Map<UUID, List<Transaction>> = emptyMap(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val shortcuts: List<WidgetShortcut> = emptyList(),
    val selectedAccountId: UUID? = null,
    val isLoading: Boolean = true,
    val toastMessage: String? = null
)
```

Ce `StateFlow<AppUiState>` est la **Single Source of Truth** Android, équivalent direct du `@Published` d'`AccountsManager`.

---

## 6. ⚠️ Points d'Attention Spécifiques à la Migration

| Point iOS | Adaptation Android |
|---|---|
| `scenePhase .active` (retour au premier plan) | `LifecycleObserver` dans `MainActivity` → appel `viewModel.processRecurringTransactions()` |
| `UIDocumentPickerViewController` | `ActivityResultContracts.OpenDocument(arrayOf("text/csv"))` |
| `UIActivityViewController` (partage) | `Intent.ACTION_SEND` + `FileProvider` (uri exposée) |
| Swift Charts `SectorMark` | Canvas Compose custom avec `drawArc()` |
| `@EnvironmentObject` (injection globale) | `viewModel()` déclaré au niveau du `NavHost`, passé en paramètre aux sous-écrans |
| Schéma versioning (StorageService.schemaVersion) | Champ `schemaVersion: Int` dans le JSON sérialisé, géré dans `AppDataStore` |

---

*ANDROID_MIGRATION_PLAN.md — Finoria Android v1.0*
