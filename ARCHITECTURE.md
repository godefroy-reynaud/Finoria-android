# 🏗️ ARCHITECTURE.md — Carte technique de Finoria

> Document de référence pour **humains et IA**. À lire avant toute modification.
> Il décrit la structure exacte du code, le schéma de données, les conventions
> métier non évidentes et les checklists (migration, release).
>
> Dernière mise à jour : 2026-07-02 — Room v2.

---

## 1. Vue d'ensemble

Finoria est une app de finances personnelles **entièrement locale** (pas de
réseau, pas de backend). Un seul module Gradle (`:app`), un seul écran Activity
(`MainActivity`), toute l'UI en Jetpack Compose.

```
┌─────────────┐  collectAsStateWithLifecycle  ┌───────────────┐
│ Composables │ ◀──────────────────────────── │ MainViewModel │  @HiltViewModel
│  (ui/…)     │ ────────── appels ──────────▶ │ (orchestrateur│
└─────────────┘                               │    léger)     │
                                              └──────┬────────┘
                                                     ▼
                                       ┌──────────────────────────┐
                                       │   AccountsRepository     │  @Singleton
                                       │  SEUL chemin d'écriture  │
                                       └──────┬───────────┬───────┘
                                              ▼           ▼
                                     ┌──────────────┐ ┌──────────────────┐
                                     │ Room (SQLite)│ │ StorageService   │
                                     │ finoria.db   │ │ (DataStore prefs)│
                                     │ source de    │ └──────────────────┘
                                     │ vérité       │
                                     └──────────────┘
        Services purs (object, sans état) : CalculationService, RecurrenceEngine, CsvService
```

**Réactivité** : les `Flow` Room ré-émettent après chaque écriture →
`AccountsRepository` les assemble en `StateFlow` → l'UI se met à jour seule.
Il n'y a **aucun rafraîchissement manuel** nulle part.

**Règle d'or** : toute mutation de données passe par
`Composable → MainViewModel → AccountsRepository → DAO`. Jamais de DAO dans
l'UI, jamais d'écriture hors repository.

---

## 2. Persistance & versioning des données

### 2.1 Room — source de vérité

- Base : `finoria.db` ([FinoriaDatabase.kt](app/src/main/java/com/finoria/app/data/local/FinoriaDatabase.kt)), **version 2**, `exportSchema = true`.
- Schémas exportés dans [app/schemas/](app/schemas/) (versionnés dans git —
  **ne jamais supprimer**, ils servent aux tests de migration).
- Construite dans [AppModule.kt](app/src/main/java/com/finoria/app/di/AppModule.kt) avec `.addMigrations(...)` et
  **sans** `fallbackToDestructiveMigration` (interdit : perte de données).

### 2.2 Schéma (v2)

```
accounts                    id TEXT PK, name, detail, style (enum name)
custom_categories           id TEXT PK, accountId FK→accounts CASCADE, name, symbol, colorHex
recurring_transactions      id TEXT PK, accountId FK→accounts CASCADE, amount, comment,
                            type, category, frequency, startDate (epochDay),
                            lastGeneratedDate (epochDay, null), isPaused,
                            customCategoryId FK→custom_categories SET_NULL
widget_shortcuts            id TEXT PK, accountId FK→accounts CASCADE, amount, comment,
                            type, category, customCategoryId FK SET_NULL
transactions                id TEXT PK, accountId FK→accounts CASCADE, amount (signé),
                            comment, potentiel (bool), date (epochDay, null),
                            category, sourceRecurringId FK→recurring SET_NULL,
                            customCategoryId FK SET_NULL, importedCategoryName (null)
```

Règles de suppression (appliquées par les FK, `PRAGMA foreign_keys=ON`) :

| Suppression de… | Effet |
|---|---|
| un compte | **CASCADE** : tout son contenu est supprimé |
| une récurrence | **SET_NULL** sur `transactions.sourceRecurringId` (l'historique validé est conservé) ; ses occurrences *potentielles* sont supprimées explicitement avant |
| une catégorie perso | **SET_NULL** partout (les éléments retombent sur la catégorie « Autre ») |

Encodage des types ([Converters.kt](app/src/main/java/com/finoria/app/data/local/Converters.kt)) :
- Enums stockés par leur **`name` Kotlin** (jamais l'ordinal). ⚠️ Ne jamais
  renommer/supprimer un cas d'enum publié (`TransactionCategory`,
  `AccountStyle`, `TransactionType`, `RecurrenceFrequency`) — ajouter
  seulement. Les lecteurs sont défensifs (`valueOf` → fallback `OTHER`/`BANK`).
- `LocalDate` stocké en **epoch day** (Long). `UUID` stockés en `String`.

### 2.3 Migrations — la checklist

**À chaque évolution du schéma** (nouvelle colonne/table/index) :

1. Modifier l'entité dans `data/local/entity/`.
2. **Bumper `version`** dans `@Database` (FinoriaDatabase).
3. Écrire une `Migration(n, n+1)` dans le companion de `FinoriaDatabase`
   (SQL brut ; pour ajouter une FK, recréer la table : create → copy → drop →
   rename, comme `MIGRATION_1_2`).
4. L'ajouter à `.addMigrations(...)` dans `AppModule`.
5. Builder une fois → le nouveau `app/schemas/…/<n+1>.json` est généré ;
   **le commiter**.
6. Ajouter un test `migrateNtoN+1` dans
   [MigrationTest.kt](app/src/androidTest/java/com/finoria/app/data/local/MigrationTest.kt)
   (insère des données en v_n, migre, vérifie qu'elles survivent) et le lancer
   sur un émulateur : `./gradlew connectedDebugAndroidTest`.

Un utilisateur peut sauter des versions (v1 → v3) : Room chaîne les migrations
fournies, il faut donc **toutes** les garder dans `addMigrations`.

### 2.4 DataStore (`finoria_prefs`)

[StorageService.kt](app/src/main/java/com/finoria/app/data/local/StorageService.kt) — trois clés seulement :

| Clé | Rôle |
|---|---|
| `lastSelectedAccountId` | compte affiché au lancement |
| `migrated_to_room_v1` | flag one-shot : l'import JSON→Room a déjà eu lieu |
| `accounts_data_v2` | **legacy** : ancien blob JSON (pré-Room), lu une seule fois pour la migration |

### 2.5 Sauvegarde Android

`android:allowBackup="true"` + règles explicites
([backup_rules.xml](app/src/main/res/xml/backup_rules.xml),
[data_extraction_rules.xml](app/src/main/res/xml/data_extraction_rules.xml)) :
la base Room **et** le DataStore sont inclus ensemble (cloud + device-transfer)
pour rester cohérents à la restauration.

---

## 3. Conventions métier (invariants à ne pas casser)

Ces règles sont appliquées partout ; les violer crée des bugs silencieux :

1. **Montants signés** : une dépense est stockée **négative**, un revenu
   positif. La conversion se fait à la saisie via `TransactionType.signed(amount)`
   — l'UI saisit toujours une valeur absolue.
2. **Transaction potentielle** : `potentiel = true` = prévue, non comptée dans
   le solde, comptée dans le « futur ». La validation (`Transaction.validated()`)
   pose `potentiel = false` + date.
3. **Double sélection de catégorie** : si `customCategoryId != null`, alors
   `category` vaut **obligatoirement** `TransactionCategory.OTHER` (convention
   héritée du portage iOS). L'affichage résout la catégorie perso via
   `LocalCustomCategories` (CompositionLocal fourni dans `MainScreen`).
4. **Récurrences ancrées sur `startDate`** : la n-ième occurrence est
   `startDate + n × période` (`RecurringTransaction.occurrenceDate(index)`),
   jamais chaînée depuis l'occurrence précédente — un loyer du 31 reste au 31
   (clampé les mois courts, sans dérive). Testé par `RecurrenceAnchoringTest`.
5. **Génération des récurrences** : `RecurrenceEngine.processAll` génère les
   occurrences jusqu'à aujourd'hui + 1 mois (futures = potentielles), avec
   garde-fou anti-doublon (`recurringTransactionId` + date). Déclenchée à
   l'init, au retour au premier plan (`ON_RESUME`) et après tout CRUD de
   récurrence.
6. **Import CSV** : un libellé de catégorie inconnu est mémorisé dans
   `transactions.importedCategoryName`, puis résolu au commit de l'import
   (catégorie perso trouvée par **nom normalisé** — sans casse ni accents,
   `CustomCategory.normalizeName` — ou créée automatiquement). Créer/renommer
   une catégorie perso « rattache » aussi les transactions en attente
   (`relinkImportedTransactions`).
7. **Format CSV** : `Date,Montant,Commentaire,Catégorie` — date `jj/MM/aaaa`,
   montant signé à virgule décimale entre guillemets, échappement RFC 4180.
   L'export exclut les potentielles et les transactions générées par récurrence.
8. **Langue** : UI française, textes en dur dans les composables (choix assumé,
   pas de multi-langue prévu) ; le **code** (identifiants, commits) est
   anglais camelCase.

---

## 4. Arborescence commentée

```
app/src/main/java/com/finoria/app/
├── FinoriaApp.kt                  # @HiltAndroidApp ; canal de notification + planif. rappel hebdo
├── MainActivity.kt                # @AndroidEntryPoint ; edge-to-edge ; demande POST_NOTIFICATIONS
│
├── data/
│   ├── local/
│   │   ├── FinoriaDatabase.kt     # @Database v2 + MIGRATION_1_2 (⚠️ checklist §2.3)
│   │   ├── Converters.kt          # enums par name, LocalDate en epochDay
│   │   ├── StorageService.kt      # DataStore : compte sélectionné, flag migration, legacy JSON
│   │   ├── dao/                   # 5 DAO (Flow observeAll + suspend CRUD)
│   │   └── entity/                # 5 entités Room + Mappers.kt (entité ↔ modèle domaine)
│   ├── model/                     # Modèles de domaine (exposés à l'UI, @Serializable pour legacy)
│   │   ├── Transaction.kt         # + validated()
│   │   ├── RecurringTransaction.kt# + occurrenceDate()/occurrences() (ancrage startDate)
│   │   ├── TransactionType.kt     # INCOME/EXPENSE + signed(amount)
│   │   ├── TransactionCategory.kt # 32 catégories par défaut + guessFrom(comment)
│   │   ├── CustomCategory.kt      # normalizeName(), parseHexColor() ; implémente StylableEnum
│   │   ├── CustomCategoryIcons.kt # ~72 icônes Material nommées pour les catégories perso
│   │   ├── Account.kt / AccountStyle.kt / WidgetShortcut.kt / RecurrenceFrequency.kt
│   │   ├── TransactionManager.kt  # vue regroupée par compte (reconstruite depuis Room)
│   │   ├── AnalysesModels.kt      # AnalysisType, CategoryData (parts du camembert)
│   │   └── serializers/           # UUID & LocalDate pour kotlinx.serialization (legacy)
│   └── repository/
│       └── AccountsRepository.kt  # SEUL chemin d'écriture ; StateFlow assemblés depuis Room ;
│                                  # migration JSON→Room one-shot ; import CSV ; récurrences
│
├── di/AppModule.kt                # Hilt : database (+ migrations), DAOs, StorageService
│
├── domain/service/                # Logique pure, sans état ni Android (sauf CsvService : URIs)
│   ├── CalculationService.kt      # totaux, filtres, ventilation par catégorie
│   ├── RecurrenceEngine.kt        # génération des occurrences (voir §3.5)
│   └── CsvService.kt              # build/parse CSV, FileProvider, SAF
│
├── navigation/
│   ├── Screen.kt                  # routes + BottomNavItem (4 onglets)
│   └── FinoriaNavHost.kt          # NavHost ; routes paramétrées mois/année/catégorie
│
├── notifications/WeeklyReminderWorker.kt  # CoroutineWorker : notification du dimanche 20 h
│
├── ui/
│   ├── MainScreen.kt              # Scaffold racine : BottomNav + FAB + sheets globales ;
│   │                              # fournit LocalSnackbarHostState et LocalCustomCategories
│   ├── components/                # ── RÉUTILISABLES : chercher ici avant de créer ──
│   │   ├── TransactionFormComponents.kt # briques des 3 formulaires : TransactionTypeSelector,
│   │   │                          # CommentTextField, CategorySelectionSection, FormDeleteButton
│   │   ├── TransactionCategoryPicker.kt # picker paginé 5×2 (défaut + perso + « Ajouter »)
│   │   ├── CustomCategorySheet.kt # création/édition d'une catégorie perso
│   │   ├── CurrencyTextField.kt   # saisie montant (filtre décimal, suffixe €)
│   │   ├── TransactionRow.kt / SwipeableTransactionRow.kt
│   │   ├── StylableEnum.kt / StyleIconView.kt / StylePickerGrid.kt  # affichage icône+couleur générique
│   │   ├── ConfirmationDialog.kt / NoAccountView.kt
│   ├── home/                      # onglet Accueil : solde, raccourcis, récurrences, CSV
│   ├── analyses/                  # onglet Analyses : camembert Canvas + détail catégorie
│   ├── calendar/                  # onglet Calendrier : années → mois → transactions
│   ├── future/                    # onglet Futur : transactions potentielles
│   ├── transaction/ recurring/ shortcut/ account/   # formulaires & sheets par domaine
│   └── theme/                     # Color / Theme (Material You + clair/sombre) / Type
│
├── util/
│   ├── FormatUtils.kt             # formattedCurrency, compactAmount, toAmountInput
│   └── DateFormatting.kt          # formats FR + LocalDate ↔ epoch millis (DatePicker)
│
└── viewmodel/MainViewModel.kt     # expose les StateFlow, délègue tout ; aucune logique métier
```

Tests :

```
app/src/test/…/domain/             # JVM : ancrage récurrences, ventilation catégories, CSV
app/src/androidTest/…/data/local/  # Room in-memory : cascade/nullify ; MigrationTest (schémas exportés)
```

---

## 5. Patterns UI à respecter

- **Fichier = composable principal** (PascalCase). Suffixes : `…Screen` (plein
  écran), `…Sheet` (modal bottom sheet), `…TabScreen` (wrapper d'onglet avec
  TopAppBar), `…Row`/`…Card`/`…Grid` (éléments de liste).
- **État** : `collectAsStateWithLifecycle()` uniquement (jamais `collectAsState`).
  État local UI : `remember { mutableStateOf(...) }` hoisté au niveau du parent
  qui en a besoin.
- **Réutilisation d'abord** : les briques communes vivent dans `ui/components/`
  — notamment `TransactionFormComponents.kt` pour tout formulaire de type
  montant/commentaire/catégorie. Ne pas re-copier ces blocs.
- **Snackbar** : via `LocalSnackbarHostState.current` (fourni par MainScreen).
- **Catégories perso à l'affichage** : via `LocalCustomCategories.current`
  (map id → CustomCategory du compte sélectionné), pas de paramètre à faire
  transiter.
- Le travail bloquant (fichiers, parsing) se fait sur `Dispatchers.IO`
  (voir HomeTabScreen pour le modèle) ; les écritures base passent par les
  `suspend fun` du repository (déjà hors main thread).

---

## 6. Build & release

- **Toolchain** : AGP 9.0.1 (Kotlin intégré), KSP2, JDK 21 (JBR d'Android
  Studio). En CLI Windows : `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"`.
  ⚠️ Room doit rester ≥ 2.7 (KSP2) ; ne pas downgrader.
- **Release** : R8 activé (`isMinifyEnabled` + `isShrinkResources`).
  [proguard-rules.pro](app/proguard-rules.pro) garde les serializers
  kotlinx.serialization (migration legacy JSON) et les numéros de ligne.
  **Tester le build release sur device avant toute publication.**
- **Identifiants** : `applicationId = "com.finoria"` (définitif une fois publié),
  `namespace = "com.finoria.app"` (interne, sans contrainte).
- **Cible** : compileSdk 36 / targetSdk 36 (Android 16) — conforme à l'exigence
  Play du 31 août 2026. minSdk 26 (Android 8.0). L'app est edge-to-edge
  (`enableEdgeToEdge()` + insets gérés par `Scaffold`).

### Checklist avant chaque release

- [ ] `versionCode` incrémenté
- [ ] Schéma Room inchangé **ou** migration écrite + testée (§2.3)
- [ ] `./gradlew testDebugUnitTest` vert
- [ ] `./gradlew connectedDebugAndroidTest` vert sur un émulateur
- [ ] `./gradlew bundleRelease` construit et testé sur device (R8)
- [ ] Mise à jour d'un appareil portant l'ancienne version → données intactes
