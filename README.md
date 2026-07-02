# 💰 Finoria

> Application Android de gestion de finances personnelles — 100 % locale, sans compte ni serveur.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Persistance-Room%20v2-FF6F00)
![License](https://img.shields.io/badge/License-Private-lightgrey)

---

## Qu'est-ce que Finoria ?

Finoria permet de suivre ses comptes au quotidien : on saisit ses dépenses et
revenus, l'app calcule le solde, projette le futur (transactions « potentielles »
et récurrences) et visualise où part l'argent. **Toutes les données restent sur
le téléphone** (base SQLite locale via Room) — aucune connexion, aucun compte,
aucune collecte.

### Fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| **Multi-comptes** | Plusieurs comptes avec style (icône/couleur), solde et solde futur |
| **Transactions** | Dépenses/revenus, catégorie, date, édition complète, swipe pour modifier/supprimer |
| **Transactions potentielles** | Dépenses/revenus prévus, comptés dans le « futur » puis validables |
| **Récurrences** | Loyer, salaire, abonnements… générés automatiquement (quotidien → annuel), pause/reprise |
| **Catégories personnalisées** | Par compte : nom, icône, couleur — en plus des 32 catégories par défaut |
| **Raccourcis** | Transactions fréquentes ajoutées en un tap |
| **Analyses** | Camembert dépenses/revenus par catégorie, par mois |
| **Calendrier** | Historique par année / mois |
| **Import / Export CSV** | Avec prévisualisation à l'import et création automatique des catégories inconnues |
| **Rappel hebdomadaire** | Notification le dimanche soir (WorkManager) |
| **Sauvegarde Android** | Auto Backup + transfert d'appareil : les données suivent le téléphone |

---

## Démarrage rapide

### Prérequis

- Android Studio (Ladybug ou plus récent), avec son JBR (JDK 21 embarqué)
- SDK Android 35
- Un appareil ou émulateur sous Android 8.0+ (API 26)

### Lancer en debug

Depuis Android Studio : **Run ▶**. En ligne de commande :

```bash
./gradlew assembleDebug
```

> ⚠️ En CLI sur Windows, pointer Gradle sur le JBR d'Android Studio si le
> `JAVA_HOME` global est un autre JDK :
> `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug`

### Tests

```bash
./gradlew testDebugUnitTest         # tests unitaires (JVM)
./gradlew connectedDebugAndroidTest # tests instrumentés (device/émulateur requis) :
                                    # cascade/nullify Room + test de migration v1→v2
```

---

## Publication (Play Store)

1. **Version** : incrémenter `versionCode` (et `versionName`) dans
   [app/build.gradle.kts](app/build.gradle.kts) à chaque release.
2. **Signature** : générer l'AAB signé via Android Studio
   (*Build → Generate Signed App Bundle*) et activer **Play App Signing**.
   Garder le keystore et ses mots de passe hors du dépôt.
3. **Build release** : R8 est activé (minification + réduction des ressources).
   Toujours **tester l'APK/AAB release sur un vrai appareil** avant de publier :
   `./gradlew bundleRelease`.
4. **Base de données** : si le schéma Room a changé, relire la
   [checklist de migration](ARCHITECTURE.md#migrations--la-checklist) —
   jamais de release sans migration testée.
5. **Fiche Play** : l'app ne collecte aucune donnée (formulaire *Data safety* :
   stockage local uniquement) ; seule permission sensible : notifications.

---

## Architecture en bref

```
UI (Compose) ──observe──▶ MainViewModel ──▶ AccountsRepository ──▶ Room (SQLite)
                              │                                      ▲
                              └──▶ CalculationService (calculs purs) │
                                   RecurrenceEngine ─────────────────┘
                                   CsvService (import/export)
```

- **UI** : Jetpack Compose + Material 3, navigation par onglets (Accueil,
  Analyses, Calendrier, Futur).
- **État** : `StateFlow` alimentés par les `Flow` Room — l'UI se rafraîchit
  automatiquement après chaque écriture.
- **Persistance** : Room (schéma **versionné + migrations testées**, aucune
  perte de données à la mise à jour), DataStore pour les petites préférences.
- **DI** : Hilt.

👉 La description précise (schéma de base, conventions métier, checklist de
migration, arborescence complète) est dans **[ARCHITECTURE.md](ARCHITECTURE.md)**
— à lire avant toute modification, par un humain comme par une IA.

---

## Stack technique

| Composant | Technologie |
|-----------|-------------|
| Plateforme | Android 8.0+ (minSdk 26, targetSdk 35) |
| Langage | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 (BOM 2024.12.01) |
| Navigation | Navigation Compose |
| État | StateFlow + `collectAsStateWithLifecycle` |
| DI | Hilt (KSP) |
| Persistance | Room 2.7 (schéma versionné) + DataStore Preferences |
| Sérialisation | kotlinx.serialization (migration legacy JSON) |
| Notifications | WorkManager |
| Build | AGP 9.0.1, KSP 2.0.21 |

---

## Documentation

| Document | Contenu |
|----------|---------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Structure détaillée, schéma de données, conventions, checklists |
| [docs/archive/](docs/archive/) | Anciens plans de portage iOS → Android (historique) |

## Licence

Projet personnel — tous droits réservés.
