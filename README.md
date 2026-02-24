# 💰 Finoria Android

> Application Android de gestion de finances personnelles — Kotlin, Jetpack Compose, Hilt, MAD (Modern Android Development)

![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Hilt](https://img.shields.io/badge/DI-Hilt-FF6F00)
![License](https://img.shields.io/badge/License-Private-lightgrey)

---

## 🎯 Vision

**Finoria Android** est une application de gestion budgétaire personnelle, conçue pour être :

- **📱 100% Native** — Kotlin + Jetpack Compose, Material 3
- **⚡ Réactive** — État centralisé via `StateFlow`, rafraîchissement instantané
- **🔒 Privée** — Données stockées uniquement en local (DataStore)
- **🧩 Maintenable** — Architecture MAD avec Hilt DI, Repository Pattern, testable, DRY

### Fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| Multi-comptes | Gérez plusieurs comptes avec styles personnalisés |
| Transactions récurrentes | Automatisez loyer, salaire, abonnements… |
| Transactions potentielles | Planifiez vos dépenses/revenus futurs |
| Édition complète transaction | Type (+/-), catégorie, date, statut potentiel, suppression |
| Calendrier financier | Historique par année / mois avec navigation |
| Analyses | Répartition par catégorie (camembert Canvas) |
| Raccourcis rapides | Ajoutez une transaction récurrente en un tap |
| Export / Import CSV | Exportez et importez avec prévisualisation |
| Notifications | Rappels hebdomadaires via WorkManager |
| Swipe actions | Swipe pour modifier/supprimer les transactions |

---

## 🏗️ Architecture

### Repository Pattern + Hilt DI

```
┌──────────────────┐     observe      ┌──────────────────┐
│    Composables   │ ◀─────────────── │   MainViewModel  │
│   (Compose UI)   │                  │  (Orchestrateur) │
└──────────────────┘ ───────────────▶ └──────────────────┘
                      appelle méthodes        │
                                              ▼
                                  ┌───────────────────────┐
                                  │  AccountsRepository   │
                                  │  (CRUD + persistance) │
                                  └───────┬───────────────┘
                        ┌─────────────────┼─────────────────┐
                        ▼                 ▼                 ▼
               ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
               │ StorageService │ │RecurrenceEngine│ │CalculationSvc  │
               │  (DataStore)   │ │  (Récurrences) │ │  (Calculs)     │
               └────────────────┘ └────────────────┘ └────────────────┘
                                                      ┌────────────────┐
                                                      │   CsvService   │
                                                      │ (Import/Export)│
                                                      └────────────────┘
```

**Injection de dépendances** : Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Singleton`)

**Principe** : `MainViewModel` est un orchestrateur qui :
1. **Expose** les `StateFlow` de `AccountsRepository`
2. **Délègue** les opérations CRUD au `AccountsRepository`
3. **Délègue** les calculs purs à `CalculationService`
4. **Scope** toutes les données (transactions, raccourcis, récurrences) par compte via `TransactionManager`

### Structure des Dossiers

```
app/src/main/java/com/finoria/app/
│
├── FinoriaApp.kt                    # @HiltAndroidApp
├── MainActivity.kt                  # @AndroidEntryPoint, point d'entrée
│
├── data/
│   ├── local/
│   │   └── StorageService.kt       # DataStore Preferences + JSON
│   ├── model/
│   │   ├── serializers/
│   │   │   └── Serializers.kt      # UUID, LocalDate, Color
│   │   ├── Account.kt              # data class + AccountStyle enum
│   │   ├── AccountStyle.kt         # Enum styles de compte
│   │   ├── AnalysesModels.kt       # AnalysisType, CategoryData
│   │   ├── RecurrenceFrequency.kt  # DAILY, WEEKLY, MONTHLY, YEARLY
│   │   ├── RecurringTransaction.kt # Transactions récurrentes
│   │   ├── Transaction.kt          # data class Transaction
│   │   ├── TransactionCategory.kt  # Enum catégories (StylableEnum)
│   │   ├── TransactionManager.kt   # Gestionnaire par compte (mutable)
│   │   ├── TransactionType.kt      # INCOME / EXPENSE
│   │   └── WidgetShortcut.kt       # Raccourcis rapides
│   └── repository/
│       └── AccountsRepository.kt   # Singleton, CRUD + persistance
│
├── di/
│   └── AppModule.kt                # @Module Hilt (provides StorageService)
│
├── domain/
│   └── service/
│       ├── CalculationService.kt   # Totaux, filtres, pourcentages
│       ├── CsvService.kt           # Import/Export CSV via FileProvider
│       └── RecurrenceEngine.kt     # Génération des récurrences
│
├── navigation/
│   ├── FinoriaNavHost.kt           # NavHost + routes
│   └── Screen.kt                   # Routes, BottomNavItem
│
├── notifications/
│   └── WeeklyReminderWorker.kt     # WorkManager pour rappels
│
├── ui/
│   ├── MainScreen.kt               # Scaffold + BottomNav + FAB + Modales
│   ├── account/
│   │   ├── AccountCard.kt
│   │   ├── AccountPickerSheet.kt   # Bottom sheet sélection compte
│   │   └── AddAccountSheet.kt      # Création/édition compte
│   ├── analyses/
│   │   ├── AnalysesPieChart.kt     # Camembert Canvas
│   │   ├── AnalysesScreen.kt
│   │   ├── AnalysesTabScreen.kt
│   │   ├── CategoryBreakdownRow.kt
│   │   └── CategoryTransactionsScreen.kt
│   ├── calendar/
│   │   ├── AllTransactionsScreen.kt
│   │   ├── CalendarContentScreen.kt
│   │   ├── CalendarTabScreen.kt
│   │   ├── MonthsScreen.kt
│   │   └── TransactionsListScreen.kt
│   ├── components/
│   │   ├── CurrencyTextField.kt
│   │   ├── NoAccountView.kt
│   │   ├── StylableEnum.kt
│   │   ├── StyleIconView.kt
│   │   ├── StylePickerGrid.kt
│   │   ├── SwipeableTransactionRow.kt  # Swipe pour edit/delete
│   │   └── TransactionRow.kt
│   ├── future/
│   │   ├── FutureTabScreen.kt
│   │   └── PotentialTransactionsScreen.kt
│   ├── home/
│   │   ├── CsvImportPreviewScreen.kt  # Prévisualisation import CSV
│   │   ├── HomeComponents.kt          # BalanceHeader, QuickCard
│   │   ├── HomeScreen.kt
│   │   └── HomeTabScreen.kt           # TopAppBar + CSV + Account picker
│   ├── recurring/
│   │   ├── AddRecurringScreen.kt
│   │   └── RecurringGrid.kt
│   ├── shortcut/
│   │   ├── AddShortcutScreen.kt
│   │   └── ShortcutsGrid.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── transaction/
│       └── AddTransactionScreen.kt
│
├── util/
│   ├── DateFormatting.kt            # Extensions de date
│   └── FormatUtils.kt              # formattedCurrency(), compactAmount()
│
└── viewmodel/
    └── MainViewModel.kt            # @HiltViewModel, orchestrateur
```

📚 Documentation technique complète → [STRUCTURE_APP.md](app/STRUCTURE_APP.md)

---

## 📐 Principes de Développement

### 1. Nommage (Anglais, camelCase)

```kotlin
// ✅ Correct
fun addTransaction(transaction: Transaction)
var selectedAccountId: UUID?

// ❌ À éviter
fun ajouterTransaction(t: Transaction)
var selected_account_id: String?
```

### 2. Responsabilité Unique (SRP)

| Couche | Responsabilité UNIQUE |
|--------|----------------------|
| `MainViewModel` | Orchestration, exposition StateFlow, délégation |
| `AccountsRepository` | CRUD comptes + transactions + persistance |
| `StorageService` | Persistance DataStore + JSON |
| `RecurrenceEngine` | Génération & validation des récurrences |
| `CalculationService` | Calculs financiers purs |
| `CsvService` | Import / Export fichiers CSV |
| Composables | Affichage uniquement |

### 3. Immutabilité

```kotlin
// ❌ INTERDIT
transaction.amount = 50.0

// ✅ CORRECT
val updated = transaction.copy(amount = 50.0)
viewModel.updateTransaction(updated)
```

### 4. DRY via Extensions

```kotlin
date.dayHeaderFormatted()    // "Aujourd'hui", "Hier", "Lundi 14 juillet 2025"
date.shortFormatted()        // "14 juil."
amount.formattedCurrency()   // "1 234,56 €"
```

---

## 📱 Stack Technique

| Composant | Technologie |
|-----------|-------------|
| **Plateforme** | Android 8.0+ (API 26, cible SDK 35) |
| **Langage** | Kotlin 2.0.21 |
| **UI** | Jetpack Compose (Material 3, BOM 2024.12.01) |
| **Graphiques** | Canvas API (camembert custom) |
| **État** | `StateFlow`, `collectAsStateWithLifecycle` |
| **Navigation** | Navigation Compose 2.8.5 |
| **DI** | Hilt 2.59.2 + KSP |
| **Persistance** | DataStore Preferences + kotlinx.serialization 1.7.3 |
| **Notifications** | WorkManager 2.10.0 |
| **Build** | AGP 9.0.1, Kotlin 2.0.21, KSP 2.0.21-1.0.28 |

---

## 🚀 Développement Local

### Prérequis

- Android Studio Ladybug (2024.2.1) ou plus récent
- JDK 17
- SDK Android 35
- Émulateur ou appareil Android 8.0+

### Lancer

```bash
./gradlew assembleDebug
# ou depuis Android Studio : Run ▶
```

### Tests

```bash
./gradlew test           # Tests unitaires
./gradlew connectedTest  # Tests instrumentés
```

---

## 📋 Checklist Qualité

Avant chaque commit :

- [ ] Nommage **anglais camelCase** partout
- [ ] Aucune modification directe de data class — utiliser `.copy()`
- [ ] Toute mutation passe par `MainViewModel` → `AccountsRepository`
- [ ] Pas de code dupliqué — extraire en service ou extension
- [ ] Les Composables n'ont **aucune logique métier**
- [ ] Injection via Hilt (`@Inject`, `@HiltViewModel`)

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [STRUCTURE_APP.md](app/STRUCTURE_APP.md) | Architecture technique détaillée |

---

## 📜 Licence

Projet personnel — Tous droits réservés.

---

*Finoria Android — Développé avec Kotlin et Jetpack Compose*
