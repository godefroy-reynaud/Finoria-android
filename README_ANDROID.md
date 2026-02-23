# 💰 Finoria Android

> Application Android de gestion de finances personnelles — Kotlin, Jetpack Compose, MAD (Modern Android Development)

![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-13+-3DDC84?logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Dependencies](https://img.shields.io/badge/Dependencies-Jetpack%20only-brightgreen)
![License](https://img.shields.io/badge/License-Private-lightgrey)

---

## 🎯 Vision

**Finoria Android** est la version Android de l'application de gestion budgétaire Finoria, conçue pour être :

- **📱 100% Native** — Kotlin + Jetpack Compose, librairies Jetpack officielles uniquement
- **⚡ Réactive** — État centralisé via `StateFlow`, rafraîchissement instantané
- **🔒 Privée** — Données stockées uniquement en local (DataStore)
- **🧩 Maintenable** — Architecture MAD, testable, DRY

### Fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| Multi-comptes | Gérez plusieurs comptes avec styles personnalisés |
| Transactions récurrentes | Automatisez loyer, salaire, abonnements… |
| Transactions potentielles | Planifiez vos dépenses/revenus futurs |
| Calendrier financier | Historique par année / mois avec navigation |
| Analyses | Répartition par catégorie (camembert Canvas) |
| Raccourcis rapides | Ajoutez une transaction récurrente en un tap |
| Export / Import CSV | Sauvegardez et restaurez vos données |
| Notifications | Rappels hebdomadaires via WorkManager |

---

## 🏗️ Architecture

### Composition de Services (MAD)

```
┌──────────────┐     observe      ┌──────────────────┐
│   Composables│ ◀─────────────── │   AppViewModel   │
│  (Compose)   │                  │  (Orchestrateur) │
└──────────────┘ ───────────────▶ └──────────────────┘
                   appelle méthodes       │
                        ┌─────────────────┼─────────────────┐
                        ▼                 ▼                 ▼
               ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
               │  AppDataStore  │ │RecurrenceEngine│ │CalculationSvc  │
               │  (Persistance) │ │  (Récurrences) │ │  (Calculs)     │
               └────────────────┘ └────────────────┘ └────────────────┘
                                                      ┌────────────────┐
                                                      │   CsvService   │
                                                      │ (Import/Export)│
                                                      └────────────────┘
```

**Principe** : `AppViewModel` est un orchestrateur qui :
1. **Délègue** la persistance à `AppDataStore`
2. **Délègue** la génération récurrente à `RecurrenceEngine`
3. **Délègue** les calculs à `CalculationService` / `CsvService`
4. **Expose** l'état via `StateFlow<AppUiState>`
5. **Persiste** automatiquement après chaque mutation

### Structure des Dossiers

```
app/src/main/java/com/finoria/
├── MainActivity.kt              # Point d'entrée
├── model/                       # Modèles de données
│   ├── Account.kt
│   ├── Transaction.kt
│   ├── RecurringTransaction.kt
│   ├── TransactionCategory.kt
│   ├── WidgetShortcut.kt
│   ├── AppState.kt
│   └── Serializers.kt
├── data/                        # Couche Data
│   ├── AppDataStore.kt
│   └── CsvService.kt
├── domain/                      # Couche Domaine
│   ├── CalculationService.kt
│   └── RecurrenceEngine.kt
├── viewmodel/
│   ├── AppViewModel.kt
│   └── AppViewModelFactory.kt
├── ui/
│   ├── theme/
│   ├── navigation/
│   ├── components/
│   ├── screens/
│   └── utils/
└── notifications/
    └── NotificationScheduler.kt
```

📚 Documentation technique complète → [STRUCTURE_APP_ANDROID.md](app/src/main/STRUCTURE_APP_ANDROID.md)

---

## 📐 Principes de Développement

### 1. Nommage (Anglais, camelCase)

```kotlin
// ✅ Correct
fun addTransaction(transaction: Transaction)
var selectedAccountId: String?

// ❌ À éviter
fun ajouterTransaction(t: Transaction)
var selected_account_id: String?
```

### 2. Responsabilité Unique (SRP)

| Couche | Responsabilité UNIQUE |
|--------|----------------------|
| `AppViewModel` | Orchestration, état global, délégation |
| `AppDataStore` | Persistance DataStore + JSON |
| `RecurrenceEngine` | Génération & validation des récurrences |
| `CalculationService` | Calculs financiers purs |
| `CsvService` | Import / Export fichiers |
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
amount.formattedCurrency()   // "1 234,56 €"
amount.compactAmount()       // "2,85k"
```

---

## 🔧 Guide de Maintenance

### Ajouter un Nouveau Service

1. Créer `domain/NewService.kt` avec **fonctions pures** (object)
2. Appeler depuis `AppViewModel`, jamais depuis les Composables
3. Documenter dans `STRUCTURE_APP_ANDROID.md`

### Ajouter un Nouveau Screen

1. Créer dans le sous-dossier `ui/screens/` approprié
2. Injecter le `AppViewModel` via paramètre
3. Utiliser les composants partagés (`StyleIconView`, `CurrencyTextField`, etc.)
4. Aucune logique métier dans le Composable
5. Si utilisation de `TopAppBar` ou autres APIs Material 3 expérimentales : ajouter `@OptIn(ExperimentalMaterial3Api::class)`
6. Si utilisation de `combinedClickable` : ajouter `@OptIn(ExperimentalFoundationApi::class)`

### Ajouter un Style (Compte / Raccourci)

Ajouter un `entry` dans l'enum `StylableEnum` concerné (`AccountStyle`, `ShortcutStyle`) avec `icon`, `color`, `label`. Le `StylePickerGrid` l'affichera automatiquement.

---

## 📱 Stack Technique

| Composant | Technologie |
|-----------|-------------|
| **Plateforme** | Android 13+ (API 33) |
| **Langage** | Kotlin 1.9+ |
| **UI** | Jetpack Compose (Material 3) |
| **Graphiques** | Canvas API (camembert custom) |
| **État** | `StateFlow`, `collectAsStateWithLifecycle` |
| **Navigation** | Navigation Compose |
| **Persistance** | DataStore Preferences + kotlinx.serialization |
| **Notifications** | WorkManager + NotificationCompat |
| **Dépendances** | Jetpack officiel uniquement |

> **Note** : Certains écrans utilisent `@OptIn` pour les APIs expérimentales (Material3, Foundation). Voir STRUCTURE_APP_ANDROID.md pour la liste complète.

---

## 🚀 Développement Local

### Prérequis

- Android Studio Ladybug (2024.2.1) ou plus récent
- JDK 17
- SDK Android 35
- Émulateur ou appareil Android 13+

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
- [ ] Toute mutation passe par `AppViewModel`
- [ ] Pas de code dupliqué — extraire en service ou extension
- [ ] Les Composables n'ont **aucune logique métier**
- [ ] Schema versioning cohérent (`AppState.schemaVersion`)

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [STRUCTURE_APP_ANDROID.md](app/src/main/STRUCTURE_APP_ANDROID.md) | Architecture technique détaillée |
| [ANDROID_MIGRATION_PLAN.md](app/src/main/ANDROID_MIGRATION_PLAN.md) | Plan de migration iOS → Android |

---

## 📜 Licence

Projet personnel — Tous droits réservés.

---

*Finoria Android — Développé avec Kotlin et Jetpack Compose*
