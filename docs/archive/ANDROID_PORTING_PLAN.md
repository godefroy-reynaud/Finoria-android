# Finoria — Plan de portage Android (Kotlin + Jetpack Compose)

> Document de référence pour porter l'app iOS **Finoria** (SwiftUI + SwiftData + CloudKit)
> vers **Android (Kotlin + Jetpack Compose + Material 3 + Room)**.
>
> **Principe directeur** : une app **fonctionnelle et stable** d'abord. On n'ajoute aucune
> feature absente de l'app iOS. Chaque phase ne démarre que lorsque la précédente est stable.
> L'UI suit les conventions **Material 3**, pas une copie pixel-perfect d'iOS.

---

## Sommaire

- [Phase 0 — Analyse](#phase-0--analyse)
  - [0.1 Modèles de données](#01-modèles-de-données)
  - [0.2 Écrans et navigation](#02-écrans-et-navigation)
  - [0.3 Dépendances iOS → équivalents Android](#03-dépendances-ios--équivalents-android)
  - [0.4 Fonctionnalités : core vs secondaire](#04-fonctionnalités--core-vs-secondaire)
  - [0.5 ⚠️ Fonctionnalités SANS équivalent Android direct](#05-️-fonctionnalités-sans-équivalent-android-direct)
- [Phase 1 — Fondations (aucune UI)](#phase-1--fondations-aucune-ui)
- [Phase 2 — Navigation + squelette UI](#phase-2--navigation--squelette-ui)
- [Phase 3 — Fonctionnalités core](#phase-3--fonctionnalités-core)
- [Phase 4+ — Fonctionnalités secondaires (priorité décroissante)](#phase-4--fonctionnalités-secondaires-priorité-décroissante)
- [Annexe A — Choix techniques recommandés](#annexe-a--choix-techniques-recommandés)
- [Annexe B — Table de correspondance SF Symbols → Material](#annexe-b--table-de-correspondance-sf-symbols--material)

---

# Phase 0 — Analyse

## 0.1 Modèles de données

L'app persiste **5 entités** (SwiftData `@Model`) + **4 enums** stockés par leur `rawValue`.
Source : [`Finoria-app/Models/`](Finoria-app/Models/).

### Entités persistées

| Entité iOS | Champs | Relations | Règle de suppression |
|---|---|---|---|
| **Account** ([Account.swift](Finoria-app/Models/Account.swift)) | `id: UUID`, `name: String` (≤15), `detail: String` (≤20), `style: AccountStyle` | `transactions`, `widgetShortcuts`, `recurringTransactions`, `customTransactionCategories` (1→0..*) | racine ; supprime tout en **cascade** |
| **Transaction** ([Transaction.swift](Finoria-app/Models/Transaction.swift)) | `id: UUID`, `amount: Double` (signé : +revenu/−dépense), `comment: String` (≤30), `potentiel: Bool`, `date: Date?`, `category: TransactionCategory`, `importedCategoryName: String?` | `account` (→1), `sourceRecurringTransaction` (→0..1), `customCategory` (→0..1) | enfant d'Account |
| **RecurringTransaction** ([RecurringTransaction.swift](Finoria-app/Models/RecurringTransaction.swift)) | `id`, `amount` (positif), `comment` (≤20), `type: TransactionType`, `category`, `customCategory`, `frequency: RecurrenceFrequency`, `startDate: Date`, `lastGeneratedDate: Date?`, `isPaused: Bool` | `account` (→1), `generatedTransactions` (1→0..*, **nullify**) | enfant d'Account |
| **WidgetShortcut** ([WidgetShortcut.swift](Finoria-app/Models/WidgetShortcut.swift)) | `id`, `amount`, `comment` (≤15), `type`, `category`, `customCategory` | `account` (→1) | enfant d'Account |
| **CustomTransactionCategory** ([CustomTransactionCategory.swift](Finoria-app/Models/CustomTransactionCategory.swift)) | `id`, `name`, `symbol: String` (SF Symbol), `colorHex: String` (`#RRGGBB`) | `account` (→1), inverses **nullify** vers `transactions`, `widgetShortcuts`, `recurringTransactions` | enfant d'Account |

**Règles de suppression à reproduire exactement** (voir [DATA_MODEL.md](DATA_MODEL.md)) :
- `Account → *` : **cascade** (supprimer un compte supprime tout son contenu).
- `CustomTransactionCategory → {Transaction, WidgetShortcut, RecurringTransaction}` : **nullify** (la catégorie supprimée met le lien à `null`, les objets sont conservés et retombent sur la catégorie intégrée « Autre »).
- `RecurringTransaction → Transaction` : **nullify** (supprimer une récurrence conserve l'historique déjà généré).

→ En Room, cela se traduit **directement** par des `ForeignKey` avec `onDelete = CASCADE` (Account) ou `onDelete = SET_NULL` (customCategory, sourceRecurring).

### Enums (stockés par `rawValue` — stabilité critique)

| Enum iOS | Cas | Données portées | Fichier |
|---|---|---|---|
| **AccountStyle** | 10 (`bank`, `savings`, `investment`, `business`, `travel`, `grocery`, `student`, `family`, `property`, `entertainment`) | `icon` (SF Symbol), `color`, `label` (FR), `guessFrom(name:)` | [Account.swift](Finoria-app/Models/Account.swift) |
| **TransactionType** | 2 (`income="+"`, `expense="-"`) | `label` | [Transaction.swift](Finoria-app/Models/Transaction.swift) |
| **RecurrenceFrequency** | 4 (`daily`, `weekly`, `monthly`, `yearly`) | `label`, `shortLabel` | [RecurringTransaction.swift](Finoria-app/Models/RecurringTransaction.swift) |
| **TransactionCategory** | **32** (salary, income, freelance, bonus, rent, utilities, home, subscription, phone, insurance, food, grocery, coffee, fuel, transport, car, loan, savings, investment, tax, shopping, party, sport, travel, culture, family, health, gift, education, pet, expense, other) | `icon`, `color`, `label`, `guessFrom(comment:type:)` (≈30 règles mots-clés FR) | [TransactionCategory.swift](Finoria-app/Models/TransactionCategory.swift) |

> ⚠️ **Le `rawValue` (= nom du `case`) est persisté.** Côté Android on stocke le **nom de l'enum** (String) en base, jamais l'ordinal. Ne jamais renommer/supprimer un cas après publication — uniquement ajouter (voir migration, Annexe A).

### Logique métier embarquée (à porter telle quelle, c'est le cœur)

- **Auto-catégorisation** : `TransactionCategory.guessFrom(comment:type:)` et `AccountStyle.guessFrom(name:)` — listes de `contains` sur mots-clés français.
- **Calculs** : [`CalculationService`](Finoria-app/Services/CalculationService.swift) — totaux validés/potentiels, totaux par mois/année, % de variation mensuelle.
- **Moteur de récurrence** : [`RecurrenceEngine`](Finoria-app/Services/RecurrenceEngine.swift) — génère les occurrences du mois à venir **ancrées sur `startDate`** (un loyer du 31 reste au 31, simplement clampé les mois courts), valide automatiquement les potentielles dont la date est passée, double anti-doublon (`lastGeneratedDate` + check par (récurrence, jour)).
- **CSV** : [`CSVService`](Finoria-app/Services/CSVService.swift) — export/import RFC 4180 (échappement guillemets/virgules), format FR (`dd/MM/yyyy`, EUR).

---

## 0.2 Écrans et navigation

Navigation racine : un **TabView à 5 onglets** dans [ContentView.swift](Finoria-app/Views/ContentView.swift), dont une **pseudo-tab « + »** (`role: .search`) qui n'navigue pas mais ouvre une sheet d'ajout puis revient à l'onglet précédent.

| # | Écran iOS | Rôle | Fichier |
|---|---|---|---|
| **Racine** | `ContentView` | TabView 5 onglets, onboarding, alerte CloudKit, lifecycle (refresh + récurrences au foreground) | [ContentView.swift](Finoria-app/Views/ContentView.swift) |
| **Tab 1** | `HomeTabView` → `HomeView` | Solde + % variation, cartes « Solde du mois »/« À venir », grille de raccourcis, grille de récurrences, toasts, toolbar export/import CSV | [HomeTabView.swift](Finoria-app/Views/TabView/HomeTabView.swift), [HomeView.swift](Finoria-app/Views/TabView/HomeView.swift) |
| **Tab 2** | `AnalysesTabView` → `AnalysesView` | Camembert (donut) dépenses/revenus par catégorie, navigation mensuelle, drill-down catégorie | [AnalysesTabView.swift](Finoria-app/Views/TabView/Analyses/AnalysesTabView.swift), [AnalysesView.swift](Finoria-app/Views/TabView/Analyses/AnalysesView.swift) |
| **Tab 3** | `CalendrierMainView` → `CalendrierTabView` | Navigation Jour/Mois/Année avec totaux par période | [CalendrierTabView.swift](Finoria-app/Views/TabView/Calendrier/CalendrierTabView.swift) |
| **Tab 4** | `FutureTabView` → `PotentialTransactionsView` | Liste des transactions potentielles, valider/supprimer (swipe) | [PotentialTransactionsView.swift](Finoria-app/Views/TabView/PotentialTransactionsView.swift) |
| **Pseudo-tab** | « + » | Ouvre `AddTransactionView` en sheet | logique dans `ContentView.onChange(tabSelection)` |
| Sheet | `AddTransactionView` | Formulaire transaction (montant, commentaire, type, catégorie, date, futur) | [AddTransactionView.swift](Finoria-app/Views/Transactions/AddTransactionView.swift) |
| Sheet | `AddRecurringTransactionView` | Formulaire récurrence | [AddRecurringTransactionView.swift](Finoria-app/Views/Recurring/AddRecurringTransactionView.swift) |
| Sheet | `AddWidgetShortcutView` | Formulaire raccourci | [AddWidgetShortcutView.swift](Finoria-app/Views/Widget/AddWidgetShortcutView.swift) |
| Sheet | `AccountPickerView` | Liste des comptes, sélection, ajout/édition/reset/suppression | [AccountPickerView.swift](Finoria-app/Views/Account/AccountPickerView.swift) |
| Sheet | `AddAccountSheet` | Formulaire compte (nom, détail, style + aperçu) | [AddAccountSheet.swift](Finoria-app/Views/Account/AddAccountSheet.swift) |
| Sheet | `AddCustomTransactionCategorySheet` | Création catégorie perso (nom, couleur, symbole parmi 72) | [AddCustomTransactionCategorySheet.swift](Finoria-app/Views/Transactions/AddCustomTransactionCategorySheet.swift) |
| Sheet | `WelcomeView` | Onboarding « What's New » premier lancement | [WelcomeView.swift](Finoria-app/Views/WelcomeView.swift) |
| Push | `AllTransactionsView`, `TransactionsListView`, `MonthsView`, `CategoryTransactionsView` | Listes détaillées (drill-down depuis Home/Calendrier/Analyses) | [Views/TabView/Calendrier/](Finoria-app/Views/TabView/Calendrier/), [Analyses/](Finoria-app/Views/TabView/Analyses/) |
| États | `NoAccountView`, `DatabaseErrorView` | Vides / erreur DB | [NoAccountView.swift](Finoria-app/Views/NoAccountView.swift), [DatabaseErrorView.swift](Finoria-app/Views/DatabaseErrorView.swift) |
| Composant | `TransactionCategoryPicker` | **Le plus complexe** : grille paginée catégories intégrées + perso + bouton ajouter, appui long → menu modifier/supprimer | [TransactionCategoryPicker.swift](Finoria-app/Views/Components/TransactionCategoryPicker.swift) |

**Composant transverse** : `accountPickerToolbar` (bouton « changer de compte » dans la barre de nav de chaque onglet) — [ViewModifiers.swift](Finoria-app/Extensions/ViewModifiers.swift).

---

## 0.3 Dépendances iOS → équivalents Android

L'app iOS a **zéro dépendance tierce** (SPM vide — [Package.resolved](Finoria.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved)). Tout est framework Apple. Voici la traduction :

| Brique iOS (framework Apple) | Rôle | Équivalent Android recommandé |
|---|---|---|
| **SwiftUI** | UI déclarative | **Jetpack Compose** + **Material 3** (`androidx.compose.material3`) |
| **SwiftData** (`@Model`, `@Query`, `ModelContainer`) | Persistance locale | **Room** (`androidx.room`) — entities + DAO + `Flow` réactif |
| **Versioned schema + MigrationPlan** | Évolution sans perte | **Room `Migration`** + `fallbackToDestructiveMigration` **désactivé** |
| **CloudKit** (`.automatic`, `CKContainer`) | Sync iCloud multi-appareils | ❌ **Aucun équivalent** — voir [§0.5](#05-️-fonctionnalités-sans-équivalent-android-direct) |
| **Swift Charts** (`SectorMark` donut) | Camembert analyses | **Vico** (`com.patrykandpatrick.vico`, Compose-natif) ou **Canvas** custom. *Recommandé : Canvas custom* (un donut est ~80 lignes, zéro dépendance, contrôle total des interactions tap) |
| **Observation** (`@Observable`, `@MainActor`) | État réactif | **ViewModel** + **StateFlow** + **Coroutines** |
| **@AppStorage / UserDefaults** | Préférences (compte sélectionné, flags onboarding) | **Jetpack DataStore** (Preferences) |
| **UserNotifications** (rappel hebdo) | Notification locale dimanche 20h | **WorkManager** (`PeriodicWorkRequest`) + `NotificationManager` + canal de notif |
| **registerForRemoteNotifications** (push silencieux CloudKit) | Déclenche sync | ❌ Sans objet (pas de CloudKit). Déjà **abandonné** côté iOS aussi |
| **ShareLink + Transferable** (export CSV) | Partager le fichier | **`Intent.ACTION_SEND`** + **FileProvider** |
| **UIDocumentPicker** (import CSV) | Choisir un fichier | **Storage Access Framework** : `ActivityResultContracts.OpenDocument()` |
| **os.log `Logger`** | Logs | `android.util.Log` ou **Timber** |
| **SF Symbols** (icônes catégories/styles/symboles) | Icônes système | **Material Icons** (`material-icons-extended`) via **table de mapping** (voir [Annexe B](#annexe-b--table-de-correspondance-sf-symbols--material)) ⚠️ travail manuel |
| **Color (système, `.blue`, etc.)** | Couleurs catégories | `androidx.compose.ui.graphics.Color` (valeurs fixes) |
| **Haptics** (`UIImpactFeedbackGenerator`) | Retour haptique raccourcis/appui long | `HapticFeedback` Compose / `Vibrator` |
| **NumberFormat `.currency(EUR)` / `fr_FR`** | Format montants/dates | `java.text.NumberFormat` + `java.time` avec `Locale.FRANCE` |
| **DI implicite (env injection)** | Injection de l'AccountsManager | **Hilt** (`com.google.dagger:hilt-android`) |

---

## 0.4 Fonctionnalités : core vs secondaire

### CORE — sans elles l'app n'a aucun intérêt (Phase 3)
1. **Comptes** : créer/sélectionner/éditer/supprimer ; multi-comptes.
2. **Transactions** : ajouter/éditer/supprimer (montant signé, commentaire, date, catégorie).
3. **Solde** : total validé du compte sélectionné, affiché sur l'accueil.
4. **Catégories intégrées (32)** + **auto-catégorisation** depuis le commentaire.

### SECONDAIRE — par priorité décroissante (Phases 4+)
5. **Transactions futures (potentielles)** + validation. *(quasi-core, mais isolable)*
6. **Transactions récurrentes** + moteur de génération/auto-validation.
7. **Raccourcis « une tape »** sur l'accueil (+ toasts/haptique).
8. **Catégories personnalisées** (nom, couleur, symbole) par compte.
9. **Analyses** (camembert + drill-down).
10. **Calendrier** (navigation jour/mois/année).
11. **Export / import CSV**.
12. **Onboarding** (écran de bienvenue).
13. **Rappel hebdomadaire** (notification locale).
14. **Sync iCloud** → ❌ voir §0.5 (à **ne pas** porter en v1).

---

## 0.5 ⚠️ Fonctionnalités SANS équivalent Android direct

Ces points doivent être **signalés et arbitrés explicitement** — ne pas les porter aveuglément.

### 🔴 1. Synchronisation iCloud / CloudKit — **AUCUN équivalent**
CloudKit est exclusivement Apple. **Impossible** de synchroniser avec un compte iCloud depuis Android.
C'est la fonctionnalité la plus structurante de l'app iOS (README : « synchronized across your Apple devices through your own iCloud account »).

**Décision recommandée pour la v1 : application 100 % locale (Room seul), sans aucune sync.**
C'est conforme au principe « stabilité d'abord » et supprime toute la complexité CloudKit (diagnostics, fallback, push silencieux, alertes de statut → tout cela disparaît côté Android).

**Alternatives natives Android, si une sync est souhaitée plus tard (hors v1) :**
- **Sauvegarde Auto Backup Android** (gratuit, zéro code, sauvegarde le fichier Room sur le Drive de l'utilisateur, restauré à la réinstallation) — *pas* du multi-appareils temps réel, mais une sécurité « ne pas perdre ses données ». **Recommandé comme premier pas.**
- **Google Drive App Data folder** (export/import du fichier `.db`, semi-manuel).
- **Backend custom** (REST + Postgres) ou **Firebase Firestore** — vrai multi-appareils, mais c'est un projet à part entière, **hors périmètre du portage**.

> Conséquence sur l'architecture : on **supprime** `CloudKitService`, l'alerte CloudKit de `ContentView`, le fallback `makeFallbackContainer`, l'`AppDelegate`/push. Le `dataVersion` (hack iOS pour rafraîchir l'UI car l'observation des inverses SwiftData n'est pas fiable) **n'est pas nécessaire** : les `Flow` de Room ré-émettent automatiquement après chaque écriture.

### 🟠 2. La pseudo-tab « + » (`role: .search`)
Pattern iOS spécifique (un onglet qui ouvre une sheet). **Anti-pattern sur Material.**
→ **Alternative native : un `FloatingActionButton`** (Material). On garde **4 onglets** dans la `NavigationBar` du bas (Accueil, Analyses, Calendrier, Futur) et un **FAB central/docké** pour « ajouter une transaction ».

### 🟠 3. Sheets iOS (modales glissantes)
Pas de notion de « sheet » sur Android. → **Alternative native :**
- Formulaires longs (ajout transaction/compte/récurrence) → **écran plein** (destination de navigation) ou **`ModalBottomSheet`** Material 3.
- Menus contextuels courts → **`ModalBottomSheet`** ou **`DropdownMenu`**.
*Recommandé : écrans pleins pour les formulaires (plus robuste, back système natif).*

### 🟠 4. Swipe actions (valider/supprimer)
iOS `.swipeActions`. → **Alternative native : `SwipeToDismiss` Material 3** (`SwipeToDismissBox`) avec arrière-plans colorés, ou menu d'actions sur appui long.

### 🟡 5. Camembert interactif (Swift Charts donut)
Material 3 **n'a pas** de composant graphique. → Canvas custom (recommandé) ou Vico.

### 🟡 6. SF Symbols
Les noms d'icônes iOS (`building.columns.fill`, `fuelpump.fill`…) **n'existent pas** sur Android. → Table de mapping vers Material Icons (Annexe B). Pour les **catégories personnalisées**, comme il n'y a pas de sync iOS↔Android, on stocke directement des **clés d'icônes Material** (le champ `symbol` change de contenu mais garde le même rôle).

### 🟡 7. `ColorPicker` système (catégorie perso)
iOS a un `ColorPicker` natif. Material 3 n'en a pas de standard. → Petite **palette de couleurs prédéfinies** (grille de pastilles) — plus simple et plus stable qu'un sélecteur HSB complet. Conserver le stockage `#RRGGBB`.

---

# Phase 1 — Fondations (aucune UI)

> **Objectif** : avoir une couche données Room complète, testée, qui reproduit fidèlement
> le modèle SwiftData + toute la logique métier (calculs, récurrence, CSV, auto-catégorisation),
> **sans une seule ligne d'UI**. C'est la phase la plus importante. On n'avance pas tant
> qu'elle n'est pas couverte par des tests unitaires.

### Structure du projet (packages)

```
com.godefroyinformatique.finoria/
├── FinoriaApplication.kt            # @HiltAndroidApp
├── MainActivity.kt                  # setContent { FinoriaTheme { FinoriaApp() } }
├── data/
│   ├── local/
│   │   ├── FinoriaDatabase.kt       # @Database(version=1)
│   │   ├── Converters.kt            # enums, Instant/LocalDate, UUID(String)
│   │   ├── dao/
│   │   │   ├── AccountDao.kt
│   │   │   ├── TransactionDao.kt
│   │   │   ├── RecurringTransactionDao.kt
│   │   │   ├── WidgetShortcutDao.kt
│   │   │   └── CustomCategoryDao.kt
│   │   └── entity/
│   │       ├── AccountEntity.kt
│   │       ├── TransactionEntity.kt
│   │       ├── RecurringTransactionEntity.kt
│   │       ├── WidgetShortcutEntity.kt
│   │       └── CustomCategoryEntity.kt
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt   # DataStore (selectedAccountId, flags)
│   └── repository/
│       └── FinoriaRepository.kt     # = AccountsManager : SEUL chemin d'écriture
├── domain/
│   ├── model/
│   │   ├── AccountStyle.kt          # enum + icon/color/label/guessFrom
│   │   ├── TransactionType.kt
│   │   ├── TransactionCategory.kt   # 32 cas + guessFrom
│   │   └── RecurrenceFrequency.kt
│   └── service/
│       ├── CalculationService.kt
│       ├── RecurrenceEngine.kt
│       ├── CsvService.kt
│       └── CategoryGuesser.kt       # (si on sort guessFrom des enums)
├── ui/ ...                          # Phase 2+
├── di/
│   └── DatabaseModule.kt            # Hilt : provide Database, DAOs, Repository
└── util/
    ├── CurrencyFormatter.kt
    ├── DateFormatting.kt
    ├── ColorHex.kt
    └── IconMapper.kt
```

---

### Étape 1.1 — Setup projet & dépendances

**Ce qu'on construit** : un projet Android vide qui compile, avec Compose, Material 3, Room, Hilt, DataStore, Coroutines.

**Fichiers à créer/modifier :**
- `build.gradle.kts` (module) : Compose BOM, `material3`, `material-icons-extended`, `room-runtime` + `room-ktx` + `room-compiler` (ksp), `hilt-android` + `hilt-compiler`, `datastore-preferences`, `navigation-compose`, `lifecycle-viewmodel-compose`, `work-runtime-ktx`, `kotlinx-coroutines`. Tests : `junit`, `room-testing`, `kotlinx-coroutines-test`, `turbine`.
- `FinoriaApplication.kt` : `@HiltAndroidApp class FinoriaApplication : Application()`.
- `MainActivity.kt` : `@AndroidEntryPoint`, `setContent { … }`.
- `minSdk = 26` recommandé (java.time sans desugaring ; iOS cible iOS 18 donc un public récent — acceptable).

**Équivalent iOS** : projet Xcode + `FinoriaApp.swift`.

**Critère de validation** : `./gradlew assembleDebug` réussit ; l'app lance un écran « Hello » ; Hilt s'initialise sans crash.

---

### Étape 1.2 — Enums du domaine (avec icône/couleur/label/guess)

**Ce qu'on construit** : les 4 enums, fidèles aux `rawValue`, avec leurs propriétés visuelles et la logique de devinette.

**Fichiers :**

`domain/model/TransactionType.kt`
```kotlin
enum class TransactionType(val raw: String, val label: String) {
    INCOME("+", "Revenu"),
    EXPENSE("-", "Dépense");
    // stocké en base par `name` (INCOME/EXPENSE) via TypeConverter — voir note rawValue
}
```

`domain/model/AccountStyle.kt`
```kotlin
enum class AccountStyle(val label: String, val icon: ImageVector, val color: Color) {
    BANK("Courant", Icons.Filled.AccountBalance, FinoriaBlue),
    SAVINGS("Épargne", Icons.Filled.Savings, FinoriaOrange),
    // … 10 cas, voir Annexe B pour le mapping d'icônes
    ;
    companion object {
        fun guessFrom(name: String): AccountStyle { /* port exact des contains() */ }
    }
}
```

`domain/model/TransactionCategory.kt` — **32 cas** + `guessFrom(comment, type)` (port ligne-à-ligne des ~30 règles `contains`). Conserver l'**ordre de `allCases`** d'iOS (income, expense, salary, … other) car il définit l'ordre d'affichage dans le picker.

`domain/model/RecurrenceFrequency.kt` — 4 cas + `label` + `shortLabel`.

> ⚠️ **Stockage** : on persiste le **`name` Kotlin** de l'enum (équivalent du `rawValue`). Si on veut une parité parfaite avec d'éventuelles données iOS (non requis en v1 sans sync), mapper `name`↔`rawValue` (ex. `salary`). Sinon, peu importe tant que c'est stable dans le temps.

**Équivalent iOS** : [Account.swift](Finoria-app/Models/Account.swift), [Transaction.swift](Finoria-app/Models/Transaction.swift), [TransactionCategory.swift](Finoria-app/Models/TransactionCategory.swift), [RecurringTransaction.swift](Finoria-app/Models/RecurringTransaction.swift).

**Critère de validation** : tests unitaires `guessFrom` — un jeu de phrases FR (« plein d'essence » → `FUEL`, « Netflix » → `SUBSCRIPTION`, « Livret A » → `SAVINGS`…) donne exactement les mêmes résultats que l'app iOS.

---

### Étape 1.3 — Entités Room + relations + règles de suppression

**Ce qu'on construit** : les 5 entités avec les bonnes `ForeignKey` (cascade / set null) et index.

**Fichiers :** `data/local/entity/*.kt`

```kotlin
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val detail: String = "",
    val style: AccountStyle = AccountStyle.BANK,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = CASCADE),
        ForeignKey(CustomCategoryEntity::class, ["id"], ["customCategoryId"], onDelete = SET_NULL),
        ForeignKey(RecurringTransactionEntity::class, ["id"], ["sourceRecurringId"], onDelete = SET_NULL),
    ],
    indices = [Index("accountId"), Index("customCategoryId"), Index("sourceRecurringId")]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val comment: String = "",
    val potentiel: Boolean = true,
    val date: Long? = null,                       // epoch millis, nullable (cf. Date?)
    val category: TransactionCategory = TransactionCategory.OTHER,
    val importedCategoryName: String? = null,
    val accountId: String? = null,
    val customCategoryId: String? = null,
    val sourceRecurringId: String? = null,
)
// … RecurringTransactionEntity, WidgetShortcutEntity, CustomCategoryEntity (mêmes principes)
```

`data/local/Converters.kt` : `@TypeConverter` pour `AccountStyle`, `TransactionType`, `TransactionCategory`, `RecurrenceFrequency` (↔ String `name`), et `Long?`↔`Instant?` si besoin.

> Le mapping des règles de suppression est **1:1** avec iOS — c'est l'avantage : `CASCADE` = `.cascade`, `SET_NULL` = `.nullify`. Voir [DATA_MODEL.md](DATA_MODEL.md) §Règles de suppression.

**Équivalent iOS** : les 5 `@Model` + leurs `@Relationship`.

**Critère de validation** : test Room (`Room.inMemoryDatabaseBuilder`) — insérer un compte avec transactions/raccourcis/récurrences/catégories puis supprimer le compte ⇒ tout est supprimé. Supprimer une `CustomCategory` ⇒ les transactions liées passent à `customCategoryId = null` (et sont conservées). Supprimer une récurrence ⇒ les transactions générées conservées avec `sourceRecurringId = null`.

---

### Étape 1.4 — DAOs réactifs

**Ce qu'on construit** : un DAO par entité, requêtes en `Flow` (lecture réactive) + suspend (écriture).

**Fichiers :** `data/local/dao/*.kt`

```kotlin
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    fun observeForAccount(accountId: String): Flow<List<TransactionEntity>>
    @Upsert suspend fun upsert(tx: TransactionEntity)
    @Delete suspend fun delete(tx: TransactionEntity)
    @Query("SELECT * FROM transactions") suspend fun getAll(): List<TransactionEntity>
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name") fun observeAll(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1") fun observe(id: String): Flow<AccountEntity?>
    // …
}
```

> **Remplace les `@Query` SwiftData + le `dataVersion`** : les `Flow` ré-émettent tout seuls après écriture → l'UI se rafraîchit automatiquement (architecture plus propre que iOS sur ce point).

**Équivalent iOS** : `modelContext.fetch`, `@Query`, et les helpers de lecture de `AccountsManager`.

**Critère de validation** : test — un `Flow` collecté via Turbine émet une nouvelle liste après chaque insert/update/delete.

---

### Étape 1.5 — Services purs (logique métier)

**Ce qu'on construit** : portage **fidèle** des 3 services iOS (sans dépendance Room — ils travaillent sur des listes/data classes).

**Fichiers :**

`domain/service/CalculationService.kt` — port direct de [CalculationService.swift](Finoria-app/Services/CalculationService.swift) : `totalNonPotential`, `totalPotential`, `availableYears`, `totalForYear`, `totalForMonth`, `monthlyChangePercentage`, `validatedTransactions(year, month)`. Utiliser `java.time` (`Instant`→`LocalDate` en zone système) pour extraire année/mois.

`domain/service/RecurrenceEngine.kt` — port de [RecurrenceEngine.swift](Finoria-app/Services/RecurrenceEngine.swift) :
- `occurrences(from, to)` et `occurrenceDate(at index)` **ancrés sur `startDate`** :
  ```kotlin
  // ⚠️ ancrage : index depuis startDate, JAMAIS chaîné depuis l'occurrence précédente
  fun occurrenceDate(index: Int, start: LocalDate, freq: RecurrenceFrequency): LocalDate = when (freq) {
      DAILY   -> start.plusDays(index.toLong())
      WEEKLY  -> start.plusWeeks(index.toLong())
      MONTHLY -> start.plusMonths(index.toLong())   // plusMonths clampe déjà 31→28/30
      YEARLY  -> start.plusYears(index.toLong())
  }
  ```
  > `LocalDate.plusMonths` reproduit le clamping iOS (31 janv → 28 févr → 31 mars). À **vérifier par test**.
- `pendingTransactions()` (occurrences ≤ 1 mois non encore générées, futures = potentielles, jour J = validées).
- `processAll(accounts)` (génère + auto-valide les potentielles dont la date est passée + double anti-doublon).
- `removePotentialTransactions(for recurring)`.

`domain/service/CsvService.kt` — port de [CSVService.swift](Finoria-app/Services/CSVService.swift) : export RFC 4180 (échappement guillemets/virgules), import (parser respectant les guillemets), entête `Date,Type,Montant,Commentaire,Catégorie`, format `dd/MM/yyyy` + `Locale.FRANCE`, `importedCategoryName` pour les catégories inconnues.

**Équivalent iOS** : les 3 fichiers de `Finoria-app/Services/`.

**Critère de validation** : suite de tests unitaires JVM portant **les mêmes cas** que la logique iOS :
- Récurrence mensuelle au 31 janvier → occurrences 31/01, 28/02, 31/03, 30/04, 31/05.
- Variation mensuelle = `null` si mois précédent à 0.
- Export puis import d'un CSV contenant une virgule et un guillemet dans le commentaire ⇒ round-trip identique.
- Auto-validation : une potentielle datée d'hier devient validée.

---

### Étape 1.6 — Repository (le « AccountsManager »)

**Ce qu'on construit** : `FinoriaRepository` — **seul chemin d'écriture** (règle d'or iOS conservée). Orchestration DAO + services + DataStore. Expose des `Flow` pour la lecture.

**Fichier :** `data/repository/FinoriaRepository.kt`
```kotlin
@Singleton
class FinoriaRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val txDao: TransactionDao,
    private val recurringDao: RecurringTransactionDao,
    private val shortcutDao: WidgetShortcutDao,
    private val customDao: CustomCategoryDao,
    private val prefs: UserPreferencesRepository,
) {
    val accounts: Flow<List<AccountEntity>> = accountDao.observeAll()
    val selectedAccountId: Flow<String?> = prefs.selectedAccountId
    fun transactions(accountId: String): Flow<List<TransactionEntity>> = txDao.observeForAccount(accountId)

    // Écritures (suspend) — équivalents des AccountsManager+*.swift
    suspend fun addTransaction(...) ; suspend fun updateTransaction(...) ; suspend fun deleteTransaction(...)
    suspend fun validateTransaction(...) ; suspend fun addAccount(...) ; suspend fun deleteAccount(...)
    suspend fun addRecurring(...) ; suspend fun processRecurringTransactions() ; …
    suspend fun commitImportedTransactions(...) ; fun csvExportSnapshot(...) ; …
}
```
+ `data/preferences/UserPreferencesRepository.kt` (DataStore) : `selectedAccountId`, `hasSeenWelcome` (le flag `hasSeenICloudWarning` **disparaît** avec CloudKit).
+ `di/DatabaseModule.kt` (Hilt) : fournit DB, DAOs, Repository, DataStore.

> Regroupe en un point : le découpage iOS en extensions (`+Accounts`, `+Transactions`, `+Recurring`, `+CSV`, `+Shortcuts`, `+CustomCategories`, `+Calculations`) peut être conservé via des **fichiers d'extension Kotlin** sur `FinoriaRepository` pour garder la même lisibilité.

**Équivalent iOS** : [AccountsManager.swift](Finoria-app/Models/AccountsManager.swift) + ses 7 extensions.

**Critère de validation** : tests d'intégration repository (DB in-memory) reproduisant les flux : `addTransaction` attache bien au compte sélectionné et persiste ; `deleteAccount` rebascule la sélection sur le premier compte restant (ou `null`) ; `processRecurringTransactions` génère/valide correctement.

**🔒 Fin de Phase 1** : couche données + métier **complète et testée**. Aucune UI. On ne passe à la Phase 2 que si tous les tests passent.

---

# Phase 2 — Navigation + squelette UI

> **Objectif** : la coquille de navigation Material + **un seul flux de bout en bout fonctionnel**
> (le plus central). Écrans vides ou minimaux ailleurs.

### Étape 2.1 — Thème & coquille de navigation

**Ce qu'on construit** : `FinoriaTheme` (Material 3, dark mode), `Scaffold` avec `NavigationBar` (4 onglets) + `FloatingActionButton` (« + »), `NavHost`.

**Fichiers :**
- `ui/theme/{Color,Theme,Type}.kt` — couleurs des catégories définies ici (port des `.blue/.orange/...`), support clair/sombre.
- `ui/navigation/FinoriaNavHost.kt` — graphe : `home`, `analyses`, `calendar`, `future`, + destinations `addTransaction`, `accountPicker`, etc.
- `ui/navigation/FinoriaBottomBar.kt` — 4 `NavigationBarItem` (Accueil/Analyses/Calendrier/Futur).
- `ui/FinoriaApp.kt` — `Scaffold(bottomBar, floatingActionButton = { FAB → addTransaction })`.

> **Remplace** : `ContentView` TabView 5 onglets + pseudo-tab « + ». Ici 4 onglets + **FAB** (convention Material).

**Équivalent iOS** : [ContentView.swift](Finoria-app/Views/ContentView.swift), [ViewModifiers.swift](Finoria-app/Extensions/ViewModifiers.swift) (toolbar compte → ici une icône dans la `TopAppBar`).

**Critère de validation** : l'app affiche la bottom bar + le FAB, on navigue entre 4 écrans (placeholders), le back système fonctionne.

### Étape 2.2 — Flux central de bout en bout : **compte → solde → ajout de transaction**

C'est la **colonne vertébrale** de l'app. On le rend pleinement fonctionnel, branché sur la Phase 1.

**Ce qu'on construit :**
1. **État « aucun compte »** + création d'un compte minimal (nom + style auto-deviné). → `AccountPickerScreen` (liste) + `AddAccountScreen` (form minimal).
2. **HomeScreen** : affiche le **nom du compte sélectionné** et son **solde validé** (lecture `Flow` → `HomeViewModel`).
3. **FAB → AddTransactionScreen** : montant + commentaire + type + (catégorie auto-devinée) → `repository.addTransaction` → retour Home, **le solde se met à jour tout seul** (Flow).

**Fichiers :**
- `ui/account/{AccountPickerScreen,AccountViewModel,AddAccountScreen}.kt`
- `ui/home/{HomeScreen,HomeViewModel}.kt`
- `ui/transaction/{AddTransactionScreen,AddTransactionViewModel}.kt`
- `ui/components/CurrencyTextField.kt` (port de [CurrencyTextField.swift](Finoria-app/Views/Components/CurrencyTextField.swift) : `OutlinedTextField` clavier décimal + suffixe « € »).

**Équivalent iOS** : [HomeView.swift](Finoria-app/Views/TabView/HomeView.swift) (solde), [AddTransactionView.swift](Finoria-app/Views/Transactions/AddTransactionView.swift), [AddAccountSheet.swift](Finoria-app/Views/Account/AddAccountSheet.swift), [AccountPickerView.swift](Finoria-app/Views/Account/AccountPickerView.swift).

**Critère de validation (le jalon clé du portage)** :
> Démarrage à froid → créer un compte → le solde affiche 0,00 € → appuyer sur le FAB → ajouter « Courses −45 € » → de retour sur l'accueil le solde affiche **−45,00 €**, et la transaction est **persistée** (relancer l'app la conserve). Auto-catégorisation visible (« courses » → Courses).

**🔒 Fin de Phase 2** : on a une app installable, navigable, qui crée des comptes et des transactions de façon stable et persistante. Tout le reste est incrémental.

---

# Phase 3 — Fonctionnalités core

> Une feature à la fois, chacune stable avant la suivante.

### Étape 3.1 — Transactions complètes (édition, suppression, date, catégorie intégrée)
**Ce qu'on construit** : `AddTransactionScreen` complet (édition + création) : sélecteur de **date** (`DatePicker` Material 3), toggle « Transaction future » (`potentiel`), validations (montant > 0, ≤ 999 999 999,99, commentaire non vide ≤ 30), bouton supprimer en mode édition. Picker de catégorie **intégrée** (grille paginée — version sans catégories perso pour l'instant).
**Fichiers** : compléter `AddTransactionScreen`, créer `ui/components/CategoryPickerGrid.kt`, `ui/components/CategoryTile.kt`, `util/IconMapper.kt`.
**iOS** : [AddTransactionView.swift](Finoria-app/Views/Transactions/AddTransactionView.swift), [TransactionCategoryPicker.swift](Finoria-app/Views/Components/TransactionCategoryPicker.swift) (partie intégrée uniquement).
**Validation** : créer/éditer/supprimer une transaction ; changer sa catégorie ; les contraintes de saisie sont respectées ; le solde reflète chaque opération.

### Étape 3.2 — Liste des transactions + accueil enrichi
**Ce qu'on construit** : sur l'accueil, l'en-tête solde + **% de variation mensuelle** + carte « Solde du mois ». Écran liste « toutes les transactions » groupées par jour (`AllTransactions`), accessible en tapant l'en-tête.
**Fichiers** : `ui/home/components/{BalanceHeader,QuickCard}.kt`, `ui/transaction/{TransactionListScreen,TransactionRow}.kt`, `util/DateFormatting.kt` (en-têtes « Aujourd'hui »/« Hier »/« Lundi 5 février 2026 », port de [ViewModifiers.swift](Finoria-app/Extensions/ViewModifiers.swift)).
**iOS** : [HomeView.swift](Finoria-app/Views/TabView/HomeView.swift), [HomeComponents.swift](Finoria-app/Views/TabView/Home/HomeComponents.swift), `AllTransactionsView`, `TransactionRow`, [TransactionGrouping.swift](Finoria-app/Extensions/TransactionGrouping.swift).
**Validation** : l'accueil affiche solde + % cohérents ; la liste groupée par jour est correcte et triée.

### Étape 3.3 — Gestion multi-comptes complète
**Ce qu'on construit** : `AccountPickerScreen` complet (cartes compte avec solde/futur, sélection, **édition**, **reset** (supprime seulement les transactions), **suppression** cascade), `AddAccountScreen` complet (détail, sélecteur de style `AccountStyle`, **aperçu** de la carte), accès via icône compte dans la `TopAppBar` de chaque onglet.
**Fichiers** : compléter `AccountPickerScreen`/`AddAccountScreen`, `ui/account/AccountCard.kt`, `ui/components/AccountStylePickerGrid.kt`.
**iOS** : [AccountPickerView.swift](Finoria-app/Views/Account/AccountPickerView.swift), [AddAccountSheet.swift](Finoria-app/Views/Account/AddAccountSheet.swift), [AccountCardView.swift](Finoria-app/Views/Account/AccountCardView.swift), [AccountCategoryPicker.swift](Finoria-app/Views/Components/AccountCategoryPicker.swift).
**Validation** : créer/éditer/supprimer/reset des comptes ; la sélection se rebascule correctement après suppression ; le compte sélectionné survit au redémarrage (DataStore).

**🔒 Fin de Phase 3** : l'app couvre le cœur (comptes + transactions + catégories intégrées + solde), stable.

---

# Phase 4+ — Fonctionnalités secondaires (priorité décroissante)

> Chaque phase est indépendante et ne démarre que si la précédente est stable.

### Phase 4 — Transactions futures (potentielles) + validation
**Construit** : `FutureScreen` (`PotentialTransactionsView`) — listes « récurrentes » / « futures », **valider** (swipe leading / `SwipeToDismissBox`) et **supprimer** (swipe trailing), toggle « futur » déjà géré à l'ajout.
**Fichiers** : `ui/future/{FutureScreen,FutureViewModel}.kt`.
**iOS** : [PotentialTransactionsView.swift](Finoria-app/Views/TabView/PotentialTransactionsView.swift).
**Validation** : créer une transaction future (sans date) → elle apparaît dans Futur, pas dans le solde ; la valider → elle entre dans le solde du jour.

### Phase 5 — Transactions récurrentes
**Construit** : `AddRecurringScreen` (montant, commentaire ≤20, type, fréquence, date de début, catégorie), grille des récurrences sur l'accueil (éditer/supprimer/**pause**/reprendre), branchement du `RecurrenceEngine` (Phase 1) **au lancement et au retour au premier plan** (via `Lifecycle` `ON_RESUME` de l'Activity/`ProcessLifecycleOwner`).
**Fichiers** : `ui/recurring/{AddRecurringScreen,RecurringViewModel}.kt`, `ui/home/components/RecurringGrid.kt`, hook lifecycle dans `MainActivity`/`FinoriaApp`.
**iOS** : [AddRecurringTransactionView.swift](Finoria-app/Views/Recurring/AddRecurringTransactionView.swift), [RecurringTransactionsGridView.swift](Finoria-app/Views/Recurring/RecurringTransactionsGridView.swift), [AccountsManager+Recurring.swift](Finoria-app/Models/AccountsManager+Recurring.swift), `ContentView.onChange(scenePhase)`.
**Validation** : créer un loyer mensuel au 31 → occurrences générées correctement (clamp) ; pause stoppe la génération ; reprise régénère depuis aujourd'hui ; pas de doublons après plusieurs ouvertures de l'app ; les occurrences passées sont auto-validées.

### Phase 6 — Raccourcis « une tape »
**Construit** : grille de raccourcis sur l'accueil, ajout/édition/suppression (`AddWidgetShortcutScreen`), tap → crée une transaction validée immédiate + **toast** (Snackbar/composant custom) + **retour haptique**.
**Fichiers** : `ui/shortcut/{AddShortcutScreen,ShortcutViewModel}.kt`, `ui/home/components/{ShortcutsGrid,Toast}.kt`.
**iOS** : [AddWidgetShortcutView.swift](Finoria-app/Views/Widget/AddWidgetShortcutView.swift), [ShortcutsGridView.swift](Finoria-app/Views/TabView/Home/ShortcutsGridView.swift), [Widget/Toast/](Finoria-app/Views/Widget/Toast/).
**Validation** : un tap sur un raccourci crée la bonne transaction, affiche un toast, joue un retour haptique.

### Phase 7 — Catégories personnalisées
**Construit** : extension du `CategoryPickerGrid` (catégories perso + bouton ajouter, appui long → menu modifier/supprimer via `DropdownMenu`/`ModalBottomSheet`), `AddCustomCategoryScreen` (nom ≤15, palette de couleurs, grille de symboles Material — voir §0.5/7), validation d'unicité du nom, re-link des transactions importées (déjà dans le repo).
**Fichiers** : `ui/category/{AddCustomCategoryScreen,...}.kt`, compléter `CategoryPickerGrid`.
**iOS** : [TransactionCategoryPicker.swift](Finoria-app/Views/Components/TransactionCategoryPicker.swift), [AddCustomTransactionCategorySheet.swift](Finoria-app/Views/Transactions/AddCustomTransactionCategorySheet.swift), [AccountsManager+CustomCategories.swift](Finoria-app/Models/AccountsManager+CustomCategories.swift), [ColorHex.swift](Finoria-app/Extensions/ColorHex.swift).
**Validation** : créer une catégorie perso, l'assigner à une transaction, la voir dans le picker ; la supprimer ⇒ les transactions retombent sur « Autre » (nullify) sans disparaître.

### Phase 8 — Analyses (camembert)
**Construit** : `AnalysesScreen` — segmented (Dépenses/Revenus), navigation mensuelle, **donut Canvas** custom (tap pour sélectionner une part, centre = total), liste des catégories triées, drill-down `CategoryTransactionsScreen`.
**Fichiers** : `ui/analyses/{AnalysesScreen,AnalysesViewModel,DonutChart,CategoryBreakdownRow,CategoryTransactionsScreen}.kt`, `domain/model/CategoryData.kt`.
**iOS** : [AnalysesView.swift](Finoria-app/Views/TabView/Analyses/AnalysesView.swift), [AnalysesPieChart.swift](Finoria-app/Views/TabView/Analyses/AnalysesPieChart.swift), [AnalysesModels.swift](Finoria-app/Views/TabView/Analyses/AnalysesModels.swift), [CategoryBreakdownRow.swift](Finoria-app/Views/TabView/Analyses/CategoryBreakdownRow.swift), [CategoryTransactionsView.swift](Finoria-app/Views/TabView/Analyses/CategoryTransactionsView.swift).
**Validation** : le donut reflète la répartition du mois ; le tap sélectionne/désélectionne une part ; le drill-down liste les bonnes transactions ; chaque catégorie perso a sa propre part.

### Phase 9 — Calendrier
**Construit** : `CalendarScreen` — segmented Jour/Mois/Année, listes avec totaux par période, drill-down mois→transactions.
**Fichiers** : `ui/calendar/{CalendarScreen,CalendarViewModel,MonthsList,TransactionsForPeriod}.kt`.
**iOS** : [CalendrierTabView.swift](Finoria-app/Views/TabView/Calendrier/CalendrierTabView.swift), [MonthsView.swift](Finoria-app/Views/TabView/Calendrier/MonthsView.swift), [TransactionsListView.swift](Finoria-app/Views/TabView/Calendrier/TransactionsListView.swift), [CalendrierRoute.swift](Finoria-app/Views/TabView/Calendrier/CalendrierRoute.swift).
**Validation** : totaux jour/mois/année corrects ; navigation cohérente avec les données.

### Phase 10 — Export / import CSV
**Construit** : export via `Intent.ACTION_SEND` + **FileProvider** (génération hors thread principal), import via **Storage Access Framework** (`OpenDocument`), dialogue de confirmation du nombre de lignes (l'import n'a pas de déduplication — comme iOS, à signaler à l'utilisateur).
**Fichiers** : `ui/home/CsvExportImport.kt`, `res/xml/file_paths.xml`, `AndroidManifest` (FileProvider), branchement sur `CsvService` (Phase 1).
**iOS** : [HomeTabView.swift](Finoria-app/Views/TabView/HomeTabView.swift), [DocumentPicker.swift](Finoria-app/Views/DocumentPicker.swift), [AccountsManager+CSV.swift](Finoria-app/Models/AccountsManager+CSV.swift).
**Validation** : export → fichier partageable ouvrable ; import → le bon nombre de transactions ajoutées avec catégories re-liées ; round-trip d'un commentaire contenant une virgule.

### Phase 11 — Onboarding
**Construit** : `WelcomeScreen` (liste de features, bouton Continuer), affichée au 1er lancement (flag DataStore `hasSeenWelcome`).
**iOS** : [WelcomeView.swift](Finoria-app/Views/WelcomeView.swift).
> ⚠️ Retirer la feature « Synchronisation iCloud » de la liste des features présentées (elle n'existe pas sur Android). Ajuster le texte.
**Validation** : l'écran s'affiche une seule fois.

### Phase 12 — Rappel hebdomadaire
**Construit** : notification locale « As-tu acheté quelque chose cette semaine ? » le dimanche 20h, via **WorkManager** (`PeriodicWorkRequest` calé sur le prochain dimanche 20h) + canal de notification + permission `POST_NOTIFICATIONS` (Android 13+).
**iOS** : [Notifications.swift](Finoria-app/Notifications.swift) (`NotificationManager`).
**Validation** : la notification se planifie et se déclenche ; pas de doublon après plusieurs lancements.

### Phase 13 (optionnelle, hors v1) — Sauvegarde des données
Voir [§0.5](#05-️-fonctionnalités-sans-équivalent-android-direct). **Recommandé** : activer **Android Auto Backup** (`allowBackup=true` + règles d'inclusion du fichier Room) — zéro UI, protège contre la perte de données à la réinstallation. **Ne pas** tenter de reproduire le multi-appareils CloudKit en v1.

---

# Annexe A — Choix techniques recommandés

| Sujet | Recommandation | Raison |
|---|---|---|
| Architecture | **MVVM** : Repository (= AccountsManager, seul chemin d'écriture) + ViewModels + StateFlow | Conserve la « règle d'or » iOS, idiomatique Android |
| DI | **Hilt** | Standard, simple |
| Persistance | **Room** + `Flow` | Remplace SwiftData ; réactif (supprime le hack `dataVersion`) |
| Montants | **`Double`** (comme iOS) | Parité exacte de la logique ; *(alternative `Long` centimes plus correcte, mais diverge d'iOS → éviter pour le port initial)* |
| Dates | **`java.time`** (`Instant`/`LocalDate`), stockées en `Long` millis | `plusMonths` reproduit le clamping iOS ; nécessite `minSdk 26` (ou desugaring) |
| ID | **`String`** (UUID) | Portable, lisible |
| Enums en base | par **`name`** (jamais l'ordinal) | Équivaut au `rawValue` ; **ne jamais renommer/supprimer un cas** publié — uniquement ajouter |
| Migration | **Room `Migration`**, additif seulement (`ALTER TABLE ADD COLUMN ... DEFAULT`), **jamais** `fallbackToDestructiveMigration` en prod | Reproduit la philosophie « zéro perte » de [FinoriaSchema.swift](Finoria-app/Models/FinoriaSchema.swift) ; exporter le schéma Room (`room.schemaLocation`) et tester les migrations |
| Graphiques | **Canvas Compose** custom (donut) | Pas de composant Material ; évite une dépendance |
| Locale | **`Locale.FRANCE`** figé (EUR, `dd/MM/yyyy`) | Comme iOS (FR/EUR codés en dur) |

> **Note migration (équivalent du piège iOS build 286)** : côté Room, l'erreur symétrique est d'oublier de bumper `version` après un changement de schéma, ou d'utiliser `fallbackToDestructiveMigration` (= perte de données). Toujours : bumper la version, écrire la `Migration`, tester sur une base contenant des données de l'ancienne version.

# Annexe B — Table de correspondance SF Symbols → Material

Les noms SF Symbols n'existent pas sur Android. Centraliser le mapping dans `util/IconMapper.kt`
(ou directement dans les enums). Exemples (à compléter pour les 32 catégories + 72 symboles du picker perso) :

### AccountStyle (10)
| iOS (SF Symbol) | Material Icons (extended) |
|---|---|
| `building.columns.fill` | `Icons.Filled.AccountBalance` |
| `banknote.fill` | `Icons.Filled.Savings` / `Payments` |
| `chart.line.uptrend.xyaxis` | `Icons.Filled.TrendingUp` |
| `briefcase.fill` | `Icons.Filled.Work` |
| `airplane` | `Icons.Filled.Flight` |
| `cart.fill` | `Icons.Filled.ShoppingCart` |
| `graduationcap.fill` | `Icons.Filled.School` |
| `person.2.fill` | `Icons.Filled.People` |
| `house.fill` | `Icons.Filled.Home` |
| `gamecontroller.fill` | `Icons.Filled.SportsEsports` |

### TransactionCategory — exemples (mapper les 32)
| iOS | Material |
|---|---|
| `fuelpump.fill` | `LocalGasStation` |
| `fork.knife` | `Restaurant` |
| `cup.and.saucer.fill` | `LocalCafe` |
| `bus.fill` | `DirectionsBus` |
| `car.fill` | `DirectionsCar` |
| `cross.case.fill` | `MedicalServices` |
| `gift.fill` | `CardGiftcard` |
| `pawprint.fill` | `Pets` |
| `play.rectangle.fill` | `Subscriptions` |
| `bolt.fill` | `Bolt` |
| `ellipsis.circle.fill` (`other`) | `MoreHoriz` |
| … | … |

> Pour les **catégories personnalisées** : iOS stocke le nom du SF Symbol dans `symbol`.
> Sur Android (pas de sync iOS↔Android), on stocke une **clé d'icône Material** (ex. `"restaurant"`)
> et on résout via `IconMapper`. La grille du picker perso propose des icônes Material équivalentes
> aux 72 symboles iOS de [AddCustomTransactionCategorySheet.swift](Finoria-app/Views/Transactions/AddCustomTransactionCategorySheet.swift).

---

## Récapitulatif des écarts iOS → Android (à garder en tête)

1. **Pas de sync iCloud** → app locale (+ Auto Backup optionnel). *Le plus gros écart.*
2. **« + » pseudo-tab → FAB** ; 4 onglets en bottom bar.
3. **Sheets → écrans pleins** (formulaires) / `ModalBottomSheet` (menus courts).
4. **Swipe actions → `SwipeToDismissBox`** Material 3.
5. **Swift Charts → Canvas** custom (donut).
6. **SF Symbols → Material Icons** (table de mapping).
7. **`ColorPicker` → palette prédéfinie**.
8. **`dataVersion` supprimé** : les `Flow` Room rafraîchissent l'UI automatiquement.
9. **`CloudKitService`, `AppDelegate` push, fallback container, alerte iCloud, flag `hasSeenICloudWarning`** : tout supprimé.

*Plan rédigé à partir de l'analyse exhaustive du code iOS au 30/06/2026.*
