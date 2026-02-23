# 💰 Finoria

> Application iOS de gestion de finances personnelles — Simple, Native, Efficace

![Swift](https://img.shields.io/badge/Swift-5.9+-F05138?logo=swift&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-16+-000000?logo=apple&logoColor=white)
![SwiftUI](https://img.shields.io/badge/UI-SwiftUI-007AFF?logo=swift&logoColor=white)
![Dependencies](https://img.shields.io/badge/Dependencies-0-brightgreen)
![License](https://img.shields.io/badge/License-Private-lightgrey)

---

## 🎯 Vision

**Finoria** est une application de gestion budgétaire conçue pour être :

- **📱 100% Native** — SwiftUI pur, zéro dépendance externe
- **⚡ Réactive** — État centralisé, rafraîchissement instantané
- **🔒 Privée** — Données stockées uniquement en local (UserDefaults)
- **🧩 Maintenable** — Architecture composée, testable, DRY

### Fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| Multi-comptes | Gérez plusieurs comptes avec styles personnalisés |
| Transactions récurrentes | Automatisez loyer, salaire, abonnements… |
| Transactions potentielles | Planifiez vos dépenses/revenus futurs |
| Calendrier financier | Historique par année / mois avec navigation |
| Analyses | Répartition par catégorie (camembert Swift Charts) |
| Raccourcis rapides | Ajoutez une transaction récurrente en un tap |
| Export / Import CSV | Sauvegardez et restaurez vos données |

---

## 🏗️ Architecture

### Composition de Services (v3.0)

```
┌──────────────┐     observe      ┌──────────────────┐
│    Views     │ ◀─────────────── │  AccountsManager │
│  (SwiftUI)   │                  │  (Orchestrateur) │
└──────────────┘ ───────────────▶ └──────────────────┘
                   appelle méthodes       │
                        ┌─────────────────┼─────────────────┐
                        ▼                 ▼                 ▼
               ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
               │ StorageService │ │RecurrenceEngine│ │CalculationSvc  │
               │  (Persistance) │ │  (Récurrences) │ │  (Calculs)     │
               └────────────────┘ └────────────────┘ └────────────────┘
                                                      ┌────────────────┐
                                                      │   CSVService   │
                                                      │ (Import/Export)│
                                                      └────────────────┘
```

**Principe** : `AccountsManager` est un orchestrateur léger (~240 lignes) qui :
1. **Délègue** la persistance à `StorageService`
2. **Délègue** la génération récurrente à `RecurrenceEngine`
3. **Délègue** les calculs à `CalculationService` / `CSVService`
4. **Notifie** SwiftUI via `objectWillChange`
5. **Persiste** automatiquement via son helper `persist()`

### Structure des Dossiers

```
Finoria-app/
├── Models/         → Données & orchestration (Account, Transaction, AccountsManager…)
├── Services/       → Logique métier (StorageService, RecurrenceEngine, CalculationService, CSVService)
├── Extensions/     → Utilitaires partagés (DateFormatting, StylableEnum, ViewModifiers)
└── Views/          → Interface utilisateur (SwiftUI)
    ├── Account/        Gestion comptes
    ├── Components/     Composants réutilisables
    ├── Recurring/      Transactions récurrentes
    ├── TabView/        Onglets principaux (Home, Calendrier, Analyses, Future)
    ├── Transactions/   Ajout / ligne de transaction
    └── Widget/         Raccourcis & Toast
```

📚 Documentation technique complète → [STRUCTURE_APP.md](STRUCTURE_APP.md)

---

## 📐 Principes de Développement

### 1. Nommage (Anglais, camelCase)

```swift
// ✅ Correct
func addTransaction(_ transaction: Transaction)
var selectedAccountId: UUID?

// ❌ À éviter
func ajouterTransaction(_ t: Transaction)
var selected_account_id: UUID?
```

### 2. Responsabilité Unique (SRP)

| Couche | Responsabilité UNIQUE |
|--------|----------------------|
| `AccountsManager` | Orchestration, état global, notifications SwiftUI |
| `StorageService` | Encodage / décodage UserDefaults |
| `RecurrenceEngine` | Génération & auto-validation des récurrences |
| `TransactionManager` | CRUD par compte (collection de transactions) |
| `CalculationService` | Calculs financiers purs |
| `CSVService` | Import / Export fichiers |
| `ViewModifiers` | Modifiers & formatters partagés |
| Vues | Affichage uniquement |

### 3. Immutabilité des Transactions

```swift
// ❌ INTERDIT (Transaction est un struct)
transaction.amount = 50.0

// ✅ CORRECT
let updated = transaction.modified(amount: 50.0)
accountsManager.updateTransaction(updated)
```

### 4. DRY via Extensions Partagées

```swift
// Modifier partagé — plus de duplication de toolbar account-picker
.accountPickerToolbar(isPresented: $showSheet, accountsManager: mgr)

// Background adaptatif — remplace le code répété dans 3+ vues
.adaptiveGroupedBackground()

// Formatting centralisé
date.dayHeaderFormatted()   // "Lundi 14 Juillet 2025"
amount.formattedCurrency    // "1 234,56"
```

### 5. Protocoles Génériques

```swift
protocol StylableEnum: CaseIterable, Identifiable, Hashable {
    var icon: String { get }
    var color: Color { get }
    var label: String { get }
}
// → StylePickerGrid<AccountStyle>, StylePickerGrid<ShortcutStyle>
```

---

## 🔧 Guide de Maintenance

### Ajouter un Nouveau Service

1. Créer `Services/NewService.swift` avec **fonctions statiques pures**
2. Appeler depuis `AccountsManager`, jamais depuis les vues
3. Documenter dans `STRUCTURE_APP.md`

```swift
struct NewService {
    static func compute(_ data: [Transaction]) -> Double { /* … */ }
}
```

### Ajouter une Nouvelle Vue

1. Créer dans le sous-dossier `Views/` approprié
2. Injecter via `@EnvironmentObject var accountsManager: AccountsManager`
3. Utiliser les modifiers partagés (`.adaptiveGroupedBackground()`, `.accountPickerToolbar(…)`)
4. Aucune logique métier dans la vue — déléguer au manager

### Ajouter un Style (Compte / Raccourci)

Ajouter un `case` dans l'enum `StylableEnum` concerné + ses propriétés `icon`, `color`, `label`. Le `StylePickerGrid` l'affichera automatiquement.

---

## 📱 Stack Technique

| Composant | Technologie |
|-----------|-------------|
| **Plateforme** | iOS 16+ |
| **Langage** | Swift 5.9+ |
| **UI** | SwiftUI (100%) |
| **Graphiques** | Swift Charts |
| **État** | `@Published`, `@EnvironmentObject`, `@State` |
| **Navigation** | `NavigationStack` + `navigationDestination` |
| **Persistance** | `UserDefaults` + `Codable` (JSON) via `StorageService` (schema v1) |
| **Notifications** | `UNUserNotificationCenter` |
| **Dépendances** | **0** — 100% natif Apple |

---

## 🚀 Développement Local

### Prérequis

- macOS 13+ (Ventura)
- Xcode 15+
- iOS Simulator ou appareil physique iOS 16+

### Lancer

```bash
open Finoria.xcodeproj   # Ouvrir dans Xcode
# ⌘R pour compiler et lancer
```

### Schémas Xcode

| Schéma | Cible |
|--------|-------|
| `Finoria` | Application principale |
| `Finoria-appTests` | Tests unitaires |
| `Finoria-appUITests` | Tests d'interface |

---

## 📋 Checklist Qualité

Avant chaque commit :

- [ ] Nommage **anglais camelCase** partout
- [ ] Aucune modification directe de struct — utiliser `modified()`
- [ ] Toute mutation passe par `AccountsManager`
- [ ] Pas de code dupliqué — extraire en service, modifier ou extension
- [ ] Les vues n'ont **aucune logique métier**
- [ ] Schema versioning cohérent (StorageService.schemaVersion)

---

## 📊 Métriques v3.1

| Métrique | v1 | v3.1 | Delta |
|----------|-----|------|-------|
| Lignes AccountsManager | ~500 | ~240 | **−52%** |
| Services extraits | 2 | 4 | **+2** (StorageService, RecurrenceEngine) |
| View Modifiers partagés | 0 | 5 | ✅ DRY |
| Fichiers Analyses | 1 (361 lig.) | 4 | ✅ SRP |
| Fonctions dupliquées | ~15 | 0 | ✅ Éliminées |
| Nommage anglais | ~40% | 100% | ✅ Harmonisé |
| Fichiers de code mort | 3 | 0 | ✅ Supprimés |

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [STRUCTURE_APP.md](STRUCTURE_APP.md) | Architecture technique détaillée v3.1 (AI-Ready) |
| Ce README | Vision, principes, guide de maintenance |

---

## 📜 Licence

Projet personnel — Tous droits réservés.

---

*Finoria v3.1 — Développé avec ❤️ en Swift*
