# 📁 STRUCTURE_APP.md — Architecture Technique de Finoria

> **Version**: 3.1  
> **Dernière mise à jour**: Février 2026  
> **Statut**: Production-Ready, AI-Ready  

Ce document est la **carte géographique** de l'application. Il est optimisé pour qu'un développeur ou une IA puisse comprendre le projet en une seule lecture.

---

## 🎯 Vue d'Ensemble en 30 Secondes

**Finoria** est une application iOS de gestion de finances personnelles construite avec :
- **SwiftUI** (100% déclaratif, iOS 16+)
- **Architecture Observable** (Single Source of Truth via `AccountsManager`)
- **Persistance UserDefaults** (JSON encodé via `Codable`)
- **Composition de services** (StorageService, RecurrenceEngine, CalculationService, CSVService)

**Principe clé** : `AccountsManager` est un **orchestrateur léger**. Il ne contient aucune logique métier complexe. Il délègue aux services spécialisés et garantit la persistance + notification SwiftUI après chaque mutation.

---

## 📐 Principes d'Architecture

### 1. Boring Architecture is Good Architecture

Pas d'abstractions inutiles. Pas de protocol-oriented-everything. Chaque couche a un rôle clair :

| Couche | Rôle | Exemple |
|--------|------|---------|
| **Models** | Structures de données `Codable` | `Transaction`, `Account` |
| **Services** | Logique métier pure, sans état | `CalculationService`, `RecurrenceEngine` |
| **Store** | État observable + orchestration | `AccountsManager` |
| **Views** | Interface SwiftUI déclarative | `HomeView`, `AnalysesView` |
| **Extensions** | Utilitaires partagés | `ViewModifiers`, `DateFormatting` |

### 2. Single Source of Truth

```
Vue → appelle méthode → AccountsManager → délègue au Service → persist() → objectWillChange.send()
```

> ⚠️ **TOUTE modification de données DOIT passer par `AccountsManager`.**

### 3. Composition over Inheritance

`AccountsManager` orchestre 4 services indépendants :
- `StorageService` : persistance UserDefaults
- `RecurrenceEngine` : génération/validation des transactions récurrentes
- `CalculationService` : tous les calculs financiers (fonctions pures)
- `CSVService` : import/export CSV

---

## 📂 Arborescence des Dossiers

```
Finoria-app/
│
├── 📱 FinoriaApp.swift          # Point d'entrée (@main)
├── 🔔 Notifications.swift          # Notifications locales hebdomadaires
│
├── 🧩 Models/                      # DONNÉES — Structures immuables
│   ├── Account.swift               # Modèle compte + AccountStyle enum
│   ├── AccountsManager.swift       # 🔑 ORCHESTRATEUR (Single Source of Truth)
│   ├── RecurringTransaction.swift  # Transaction récurrente + RecurrenceFrequency
│   ├── Transaction.swift           # Struct immuable + TransactionType enum
│   ├── TransactionCategory.swift   # Catégorie unifiée (transactions, raccourcis, récurrences)
│   ├── TransactionManager.swift    # Conteneur de données par compte (non observable)
│   └── WidgetShortcut.swift        # Raccourci rapide
│
├── ⚙️ Services/                    # LOGIQUE MÉTIER — Fonctions pures, sans état
│   ├── CalculationService.swift    # Calculs financiers (totaux, filtres, pourcentages)
│   ├── CSVService.swift            # Import/Export CSV
│   ├── RecurrenceEngine.swift      # 🆕 Moteur de génération des récurrences
│   └── StorageService.swift        # 🆕 Persistance UserDefaults
│
├── 🔧 Extensions/                  # UTILITAIRES — Code partagé et réutilisable
│   ├── DateFormatting.swift        # Extension Date (noms de mois)
│   ├── StylableEnum.swift          # Protocole StylableEnum + composants génériques + compactAmount()
│   └── ViewModifiers.swift         # 🆕 Modifiers partagés (fond adaptatif, toolbar, formatage)
│
└── 🖼️ Views/                       # INTERFACE — Composants SwiftUI
    ├── ContentView.swift           # TabView principal (4 onglets + bouton ajout)
    ├── NoAccountView.swift         # État vide (aucun compte)
    ├── DocumentPicker.swift        # Sélecteur de fichiers iOS (UIKit bridge)
    │
    ├── Account/                    # Gestion des comptes
    │   ├── AccountCardView.swift   # Carte visuelle d'un compte
    │   ├── AccountPickerView.swift # Sélecteur de compte (sheet)
    │   └── AddAccountSheet.swift   # Formulaire création/édition compte
    │
    ├── Transactions/               # Gestion des transactions
    │   ├── AddTransactionView.swift # Formulaire ajout/édition
    │   └── TransactionRow.swift    # Ligne d'affichage transaction
    │
    ├── Components/                 # Composants UI réutilisables
    │   └── CurrencyTextField.swift # Champ montant avec €
    │
    ├── Widget/                     # Raccourcis rapides
    │   ├── AddWidgetShortcutView.swift # Formulaire création/édition raccourci
    │   └── Toast/                  # Notifications visuelles éphémères
    │       ├── ToastCard.swift
    │       ├── ToastData.swift
    │       └── ToastView.swift
    │
    ├── Recurring/                  # Transactions récurrentes
    │   ├── AddRecurringTransactionView.swift  # Formulaire création/édition
    │   └── RecurringTransactionsGridView.swift # Grille d'affichage
    │
    └── TabView/                    # Les 4 onglets principaux
        ├── HomeTabView.swift       # Wrapper onglet Accueil (+ CSV import/export)
        ├── HomeView.swift          # Contenu Accueil (solde, raccourcis, récurrences)
        ├── FutureTabView.swift     # Wrapper onglet Futur
        ├── PotentialTransactionsView.swift # Transactions à venir
        │
        ├── Home/                   # Composants de l'accueil
        │   ├── HomeComponents.swift    # BalanceHeader, QuickCard, ToastStack
        │   └── ShortcutsGridView.swift # Grille de raccourcis
        │
        ├── Analyses/               # Onglet Analyses
        │   ├── AnalysesTabView.swift       # Wrapper avec NavigationStack
        │   ├── AnalysesView.swift          # Vue principale (navigation mois + liste)
        │   ├── AnalysesModels.swift        # Modèles (CategoryData, AnalysisType, Route)
        │   ├── AnalysesPieChart.swift      # Camembert interactif (Charts)
        │   ├── CategoryBreakdownRow.swift  # Ligne détaillée par catégorie
        │   └── CategoryTransactionsView.swift # Transactions d'une catégorie
        │
        └── Calendrier/             # Onglet Navigation temporelle
            ├── CalendrierMainView.swift  # Wrapper avec toolbar
            ├── CalendrierTabView.swift   # Contenu (Jour/Mois/Année)
            ├── CalendrierRoute.swift     # Enum de navigation
            ├── MonthsView.swift          # Liste des mois d'une année
            ├── TransactionsListView.swift # Transactions d'un mois
            └── AllTransactionsView.swift  # Toutes les transactions groupées par jour
```

---

## 🔄 Flux de Données

### Architecture en Couches

```
┌─────────────────────────────────────────────────────────────────┐
│                     VIEWS (SwiftUI)                             │
│  HomeView, AnalysesView, CalendrierTabView, etc.                │
│  Observent AccountsManager via @ObservedObject                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Appelle des méthodes publiques
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  AccountsManager (Orchestrateur)                │
│                     ObservableObject                            │
│                                                                 │
│  @Published accounts: [Account]                                 │
│  @Published transactionManagers: [UUID: TransactionManager]     │
│  @Published selectedAccountId: UUID?                            │
│                                                                 │
│  ┌─────────────┐  Chaque méthode publique suit le même schéma : │
│  │  persist()   │  1. Muter l'état                              │
│  │  ┌────────┐  │  2. storage.save(...)                         │
│  │  │ save() │  │  3. objectWillChange.send()                   │
│  │  │notify()│  │                                               │
│  │  └────────┘  │                                               │
│  └─────────────┘                                                │
└───────┬──────────┬──────────────┬───────────────┬───────────────┘
        │          │              │               │
        ▼          ▼              ▼               ▼
 ┌────────────┐┌──────────────┐┌──────────────┐┌───────────┐
 │  Storage   ││  Recurrence  ││ Calculation  ││    CSV    │
 │  Service   ││   Engine     ││   Service    ││  Service  │
 │            ││              ││              ││           │
 │ save()     ││ processAll() ││ totalFor...()││ generate()│
 │ load()     ││ removePot.() ││ available..()││ import()  │
 │            ││              ││ validated..()││           │
 └─────┬──────┘└──────────────┘└──────────────┘└───────────┘
       │
       ▼
 ┌────────────┐
 │ UserDefaults│
 │ (JSON)     │
 │            │
 │ Key:       │
 │ accounts_  │
 │ data_v2    │
 └────────────┘
```

### Cycle de Vie d'une Mutation

```swift
// Exemple : ajouter une transaction
func addTransaction(_ transaction: Transaction) {
    currentManager?.add(transaction)  // 1. Muter
    persist()                         // 2. Sauvegarder + Notifier
}

private func persist() {
    storage.save(accounts: accounts, managers: transactionManagers)
    objectWillChange.send()
}
```

---

## 📊 Modèles de Données

### Transaction (Struct Immuable)

```swift
struct Transaction: Identifiable, Codable, Equatable {
    let id: UUID
    var amount: Double                    // Positif = revenu, Négatif = dépense
    var comment: String
    var potentiel: Bool                   // true = future, false = validée
    var date: Date?                       // nil si potentielle sans date prévue
    var category: TransactionCategory     // Catégorie (obligatoire, défaut: .other)
    var recurringTransactionId: UUID?     // Lien vers la récurrence source
    
    func validated(at date: Date) -> Transaction  // Copie validée
    func modified(...) -> Transaction             // Copie modifiée
}
```

### Account (Struct)

```swift
struct Account: Identifiable, Codable, Equatable {
    let id: UUID
    var name: String
    var detail: String
    var style: AccountStyle  // Enum avec icon + color + label
}
```

### RecurringTransaction (Struct)

```swift
struct RecurringTransaction: Identifiable, Codable, Equatable {
    let id: UUID
    let amount: Double
    let comment: String
    let type: TransactionType             // .income / .expense
    let category: TransactionCategory
    let frequency: RecurrenceFrequency    // .daily, .weekly, .monthly, .yearly
    let startDate: Date
    var lastGeneratedDate: Date?          // Anti-doublons
    var isPaused: Bool                    // Pause = aucune génération
    
    func pendingTransactions() -> [(date: Date, transaction: Transaction)]
}
```

### Enums de Style (Conformes à StylableEnum)

```swift
protocol StylableEnum: RawRepresentable, CaseIterable, Identifiable, Codable {
    var icon: String { get }   // SF Symbol
    var color: Color { get }
    var label: String { get }
}

// AccountStyle : bank, savings, investment, card, cash, piggy, wallet, business
// TransactionCategory : salary, income, rent, utilities, subscription, phone, insurance,
//   food, shopping, fuel, transport, loan, savings, family, health, gift, party, expense, other
```

---

## ⚙️ Services — Responsabilités

### StorageService (Persistance)

| Méthode | Description |
|---------|-------------|
| `save(accounts:managers:)` | Encode tout en JSON → UserDefaults + sauve `schemaVersion` |
| `load()` | Décode JSON → (accounts, managers), préparé pour futures migrations |
| `saveSelectedAccountId(_:)` | Persiste l'ID du compte sélectionné |
| `loadSelectedAccountId()` | Charge le dernier compte sélectionné |
| `schemaVersion` (static) | Version du schéma de données (actuellement `1`) |

### RecurrenceEngine (Traitement des récurrences)

| Méthode | Description |
|---------|-------------|
| `processAll(accounts:managers:)` | Génère les transactions futures (<1 mois) et auto-valide les passées |
| `removePotentialTransactions(for:from:)` | Nettoie les potentielles d'une récurrence |

### CalculationService (Calculs financiers)

| Méthode | Description |
|---------|-------------|
| `totalNonPotential(transactions:)` | Total des transactions validées |
| `totalPotential(transactions:)` | Total des transactions futures |
| `totalForMonth(_:year:transactions:)` | Total pour un mois donné |
| `availableYears(transactions:)` | Années distinctes avec transactions |
| `monthlyChangePercentage(transactions:)` | Variation % mois courant vs précédent |
| `validatedTransactions(from:year:month:)` | Filtre par année/mois |

### CSVService (Import/Export)

| Méthode | Description |
|---------|-------------|
| `generateCSV(transactions:accountName:)` | Exporte en fichier CSV temporaire |
| `importCSV(from:)` | Parse un fichier CSV → [Transaction] |

---

## 🔧 Extensions Partagées

### ViewModifiers.swift

| Composant | Usage |
|-----------|-------|
| `.adaptiveGroupedBackground()` | Fond noir (dark) / systemGroupedBackground (light) |
| `.accountPickerToolbar(isPresented:accountsManager:)` | Bouton compte dans la toolbar + sheet |
| `.if(_:transform:)` | Modifier conditionnel |
| `Date.dayHeaderFormatted()` | "Aujourd'hui", "Hier", ou "Lundi 5 février 2026" |
| `Double.formattedCurrency` | Montant formaté en EUR |

### StylableEnum.swift

| Composant | Usage |
|-----------|-------|
| `StylePickerGrid<Style>` | Grille de sélection d'icône/couleur |
| `StyleIconView<Style>` | Icône ronde avec fond coloré |
| `compactAmount(_:)` | Montant compact : 2 850 € → 2,85k € |

### DateFormatting.swift

| Composant | Usage |
|-----------|-------|
| `Date.monthName(_:)` | Numéro de mois → "Février" |

---

## 🧭 Navigation de l'Application

### Structure des Onglets (TabView)

```
ContentView (TabView)
│
├── Tab 1: HomeTabView
│   └── NavigationStack
│       ├── HomeView (racine)
│       │   ├── → AllTransactionsView (tap solde total)
│       │   ├── → TransactionsListView (tap "Solde du mois")
│       │   └── → PotentialTransactionsView (tap "À venir")
│       └── [Toolbar: Export/Import CSV, Account Picker]
│
├── Tab 2: AnalysesTabView
│   └── NavigationStack
│       ├── AnalysesView (racine)
│       │   ├── Segmented Control: Dépenses / Revenus
│       │   ├── Navigation mensuelle (chevrons < Mois Année >)
│       │   ├── Graphique camembert interactif (tap slice = sélection)
│       │   └── Liste détaillée par catégorie (CategoryBreakdownRow)
│       └── → CategoryTransactionsView (tap catégorie = transactions groupées par jour)
│
├── Tab 3: CalendrierMainView
│   └── NavigationStack + Segmented Control
│       ├── Mode "Jour" → AllTransactionsView (embedded)
│       ├── Mode "Mois" → CalendrierMonthsContentView
│       │   └── → TransactionsListView (tap mois)
│       └── Mode "Année" → CalendrierYearsContentView
│           └── → MonthsView (tap année)
│               └── → TransactionsListView (tap mois)
│
└── Tab 4: FutureTabView
    └── NavigationStack
        └── PotentialTransactionsView
            ├── Section "Transactions récurrentes" (groupées par jour, décroissant)
            ├── Section "Futures" (ordre d'ajout inversé)
            └── [Swipe: Valider / Supprimer + confirmation si récurrence]
```

---

## 🔗 Graphe de Dépendances

### Qui Dépend de Qui ?

```
Views ──────▶ AccountsManager ──────▶ StorageService
                    │                        │
                    ├──────▶ RecurrenceEngine │
                    │                        ▼
                    ├──────▶ CalculationService   UserDefaults
                    │
                    └──────▶ CSVService

Views ──────▶ StylableEnum (StylePickerGrid, StyleIconView)
Views ──────▶ ViewModifiers (adaptiveGroupedBackground, accountPickerToolbar)
```

### Règle de Dépendance

| Couche | Peut importer | Ne peut PAS importer |
|--------|---------------|---------------------|
| Models | Foundation | SwiftUI, Services, Views |
| Services | Foundation, Models | SwiftUI, Views |
| Extensions | SwiftUI, Foundation | Services, Views |
| Views | Tout | — |
| AccountsManager | Foundation, Services | SwiftUI (sauf ObservableObject) |

---

## 🔄 Logique de Récurrence

> `processRecurringTransactions()` est appelé :
> - Au **lancement** de l'app
> - Quand l'app **revient au premier plan** (scenePhase .active)
> - Après chaque **ajout** ou **modification** de récurrence
>
> Le `RecurrenceEngine` effectue :
> 1. Génère les transactions futures (< 1 mois) comme **transactions potentielles**
> 2. Vérifie les doublons via `recurringTransactionId` + `date` avant d'ajouter
> 3. Valide automatiquement les transactions dont la date est **aujourd'hui ou passée**
> 4. Met à jour `lastGeneratedDate` pour éviter les regénérations
>
> Cas particuliers :
> - **Suppression** : les transactions potentielles liées sont supprimées
> - **Modification** : les potentielles sont supprimées puis regénérées
> - **Pause** : les potentielles sont supprimées, `isPaused = true`
> - **Réactivation** : `isPaused = false`, `lastGeneratedDate` = hier (pas de rattrapage)

---

## 📱 Stack Technique

| Composant | Technologie |
|-----------|-------------|
| UI Framework | SwiftUI (iOS 16+) |
| Graphiques | Swift Charts (`SectorMark`) |
| State Management | `@Published`, `@ObservedObject`, `@State` |
| Navigation | `NavigationStack`, `NavigationLink`, `.navigationDestination` |
| Persistance | `UserDefaults` + `Codable` (via `StorageService`) |
| Notifications | `UNUserNotificationCenter` |
| Partage | `UIActivityViewController` |
| Fichiers | `UIDocumentPickerViewController` |

---

## 🧪 Points de Test Critiques

### Services (tests unitaires)

1. `StorageService` : save/load préserve les données sans perte
2. `RecurrenceEngine.processAll` : génère les bonnes transactions, évite les doublons
3. `RecurrenceEngine.removePotentialTransactions` : ne supprime que les potentielles liées
4. `CalculationService.totalForMonth` : retourne les bonnes valeurs
5. `CalculationService.monthlyChangePercentage` : calcul correct (y compris edge cases)
6. `CSVService` : export/import round-trip sans perte

### AccountsManager (tests d'intégration)

7. `addTransaction` → transaction ajoutée + persistance + notification
8. `deleteAccount` → sélection automatique du suivant
9. `processRecurringTransactions` → génération + auto-validation
10. `pauseRecurringTransaction` → potentielles supprimées, flag isPaused = true
11. `resumeRecurringTransaction` → pas de rattrapage rétroactif

### UI (tests fonctionnels)

12. Navigation complète entre les 4 onglets
13. Le graphique camembert affiche la bonne répartition
14. Swipe actions (supprimer/valider) avec confirmation pour récurrences
15. Schéma versioning : `schemaVersion` est sauvegardé et prêt pour les migrations futures

---

## 🏗️ Convention de Nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Structs / Classes | UpperCamelCase | `AccountsManager`, `Transaction` |
| Protocoles | UpperCamelCase | `StylableEnum` |
| Fonctions | lowerCamelCase | `addTransaction()`, `totalForMonth()` |
| Variables | lowerCamelCase | `selectedAccountId`, `currentMonth` |
| Enums | UpperCamelCase, cases lowerCamelCase | `AccountStyle.bank` |
| ViewModifiers | UpperCamelCase (struct), lowerCamelCase (extension) | `AdaptiveGroupedBackground` / `.adaptiveGroupedBackground()` |

---

*Document généré le 12 février 2026 — Finoria v3.1*
