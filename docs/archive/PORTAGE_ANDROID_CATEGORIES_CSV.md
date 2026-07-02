# Portage Android — CSV & Catégories

Ce document décrit **la structure des données et la logique** des fonctionnalités
CSV (import/export) et des catégories (par défaut, de compte, personnalisées) de
l'app iOS Finoria, afin de les reproduire sur l'app Android.

Il est volontairement écrit en **structure + logique** (aucun détail propre à
Swift/SwiftUI). Les composants d'UI iOS sont décrits par leur comportement pour
que tu choisisses l'équivalent Android (Compose ou Views).

---

## 1. Structure du fichier CSV d'export

### En-tête (toujours la 1re ligne)

```
Date,Type,Montant,Commentaire,Catégorie
```

5 colonnes, séparateur **virgule**, encodage **UTF-8**, fin de ligne `\n`.

### Format de chaque colonne

| # | Colonne | Contenu | Format / valeurs |
|---|---------|---------|------------------|
| 1 | `Date` | Date de la transaction | `jj/MM/aaaa` (locale FR). Si pas de date → littéral `N/A` |
| 2 | `Type` | Sens de la transaction | `Revenu` (montant ≥ 0) ou `Dépense` (montant < 0) |
| 3 | `Montant` | Montant **en valeur absolue** | 2 décimales, séparateur décimal = **point** (ex. `1250.00`). Le signe n'est **pas** stocké ici, il est porté par la colonne `Type` |
| 4 | `Commentaire` | Texte libre saisi par l'utilisateur | Chaîne quelconque |
| 5 | `Catégorie` | Libellé **affiché** de la catégorie | Soit le libellé d'une catégorie par défaut (ex. `Courses`), soit le **nom d'une catégorie personnalisée** (ex. `Abonnement Sport`) |

### Ce qui est exporté

- On exporte les transactions du **compte sélectionné** uniquement.
- On **exclut** les transactions « potentielles » (planifiées/non validées) et
  celles générées par une récurrence (`sourceRecurringTransaction != nil`).
- Tri par **date décroissante** (les plus récentes en haut). Les transactions
  sans date sont placées en dernier.
- Le nom du fichier généré : `<NomDuCompte>_transactions_<timestamp>.csv`.

### Échappement CSV (RFC 4180) — IMPORTANT à reproduire

Chaque champ est échappé indépendamment :

- Si un champ contient une **virgule**, un **guillemet** `"` ou un **saut de
  ligne**, il est entouré de guillemets doubles `"…"`.
- Les guillemets présents dans le champ sont **doublés** (`"` → `""`).
- Sinon, le champ est écrit tel quel.

> Pourquoi c'est critique : le commentaire et surtout le **nom d'une catégorie
> personnalisée** peuvent contenir une virgule. Sans cet échappement, une virgule
> crée une colonne fantôme et corrompt toute la ligne à la relecture.

Exemple de ligne exportée :

```
15/03/2026,Dépense,42.90,"Resto, midi entre collègues",Restaurant
```

---

## 2. Logique d'import CSV (avec gestion des catégories personnalisées)

L'import se fait en **2 étapes séparées** : on lit d'abord sans rien enregistrer,
puis on enregistre seulement après confirmation de l'utilisateur. C'est ce qui
permet d'afficher « X transactions vont être ajoutées » avant d'écrire.

### Étape 1 — Lecture / parsing (aucune écriture en base)

Pour chaque ligne **après l'en-tête** :

1. Ignorer les lignes vides.
2. **Découper la ligne en respectant les guillemets RFC 4180** (ne pas faire un
   simple `split(",")` : il couperait au milieu des champs échappés). L'algo :
   - parcourir caractère par caractère ;
   - un `"` bascule le mode « à l'intérieur de guillemets » ;
   - à l'intérieur, `""` = un guillemet littéral ;
   - une virgule **hors** guillemets termine un champ.
3. Si moins de 4 colonnes → ligne invalide, on l'ignore.
4. **Date** : si `N/A` → pas de date (on mettra la date du jour à la création) ;
   sinon parser `jj/MM/aaaa`.
5. **Type** : `Dépense` → dépense, sinon revenu.
6. **Montant** : parser en nombre. Puis **réappliquer le signe** selon le type :
   - `Dépense` et montant positif → on le rend négatif ;
   - revenu et montant négatif → on prend la valeur absolue.
7. **Commentaire** : colonne 4 (les virgules internes sont préservées grâce au
   parsing RFC 4180).
8. **Catégorie** (colonne 5, si présente) — c'est le point clé :
   - On compare le libellé à ceux des **catégories par défaut**. Si ça
     correspond → on assigne cette catégorie par défaut.
   - Sinon, si le libellé n'est **pas vide** → catégorie inconnue dans ce
     compte. On met la catégorie par défaut sur `Autre` **et on mémorise le
     libellé brut** dans un champ temporaire `importedCategoryName` (le nom de
     la future catégorie personnalisée).

À ce stade, rien n'est en base : on a juste une liste d'objets « transaction »
en mémoire, certaines portant un `importedCategoryName`.

### Étape 2 — Enregistrement / commit (après confirmation)

C'est ici que les **catégories personnalisées sont créées automatiquement** pour
que l'import rattache directement les transactions à leurs catégories, **sans
action manuelle** :

1. Construire un **index des catégories personnalisées existantes** du compte,
   indexé par **nom normalisé** (voir §6 pour la normalisation). Cet index sert
   de cache pour éviter les doublons.
2. Pour chaque transaction importée :
   - Si elle porte un `importedCategoryName` :
     - **résoudre ou créer** la catégorie personnalisée correspondante :
       - si une catégorie (existante **ou déjà créée pendant cet import**) a le
         même nom normalisé → la réutiliser ;
       - sinon → **créer une nouvelle catégorie personnalisée** avec ce nom, un
         **symbole et une couleur par défaut** (`tag.fill` / `#8E8E93`), et
         l'ajouter au cache.
     - rattacher la transaction à cette catégorie personnalisée, remettre sa
       catégorie par défaut sur `Autre`, et **effacer** `importedCategoryName`.
   - Rattacher la transaction au compte et l'insérer.
3. Persister une seule fois à la fin.

> **Résultat** : si le CSV contient 10 lignes avec la catégorie `Sport Club` qui
> n'existe pas encore, l'import crée **une seule** catégorie personnalisée
> `Sport Club` (couleur/symbole par défaut) et y rattache les 10 transactions.
> L'utilisateur peut ensuite lui changer couleur/symbole ; elles resteront
> groupées.

### Règles importantes de l'import

- **Pas de dé-duplication** des transactions : réimporter le même fichier
  **crée des doublons**. À signaler à l'utilisateur avant confirmation.
- Les transactions existantes ne sont **jamais** remplacées.
- Le rattachement « libellé CSV → catégorie personnalisée » se fait par **nom
  normalisé** (insensible à la casse et aux accents), pas par correspondance
  exacte de chaîne.

### Rattachement différé (cas connexe à reproduire)

Il existe une seconde voie de rattachement : quand l'utilisateur **crée ou
modifie manuellement** une catégorie personnalisée, on re-parcourt les
transactions du compte et on rattache automatiquement toutes celles dont le
`importedCategoryName` (encore présent) correspond au nom normalisé de la
catégorie. Utile si une transaction a été importée avec un libellé inconnu, puis
que l'utilisateur crée plus tard une catégorie du même nom.

---

## 3. Catégories par défaut des transactions

**32 catégories** au total. Elles sont **fixes / non modifiables / non
supprimables**. L'ordre ci-dessous est l'**ordre d'affichage** dans le sélecteur.

Chaque catégorie a : un **identifiant technique** (clé stable, à conserver
identique côté Android pour l'interop CSV/données), un **libellé** (affiché et
écrit dans le CSV), une **couleur** sémantique, une **icône** (nom SF Symbol iOS
— à remapper vers un jeu d'icônes Android).

| # | Clé (id) | Libellé (CSV) | Couleur | Icône iOS (à remapper) |
|---|----------|---------------|---------|------------------------|
| 1 | `income` | Revenu | vert | arrow.down.circle.fill |
| 2 | `expense` | Dépense | rouge | arrow.up.circle.fill |
| 3 | `salary` | Salaire | vert | briefcase.fill |
| 4 | `freelance` | Freelance | teal | laptopcomputer |
| 5 | `bonus` | Prime | jaune | star.fill |
| 6 | `rent` | Loyer | orange | house.fill |
| 7 | `utilities` | Charges | jaune | bolt.fill |
| 8 | `home` | Maison | brun | hammer.fill |
| 9 | `subscription` | Abonnement | violet | play.rectangle.fill |
| 10 | `phone` | Téléphone | indigo | iphone |
| 11 | `insurance` | Assurance | bleu | shield.fill |
| 12 | `food` | Restaurant | orange | fork.knife |
| 13 | `grocery` | Courses | vert | cart.fill |
| 14 | `coffee` | Café | brun | cup.and.saucer.fill |
| 15 | `fuel` | Carburant | orange | fuelpump.fill |
| 16 | `transport` | Transport | cyan | bus.fill |
| 17 | `car` | Voiture | bleu | car.fill |
| 18 | `loan` | Crédit | rouge | percent |
| 19 | `savings` | Épargne | mint | banknote.fill |
| 20 | `investment` | Investissement | violet | chart.line.uptrend.xyaxis |
| 21 | `tax` | Impôts | rouge | doc.text.fill |
| 22 | `shopping` | Shopping | rose | bag.fill |
| 23 | `party` | Soirée | rose | heart.fill |
| 24 | `sport` | Sport | orange | figure.run |
| 25 | `travel` | Voyage | cyan | airplane |
| 26 | `culture` | Culture | indigo | theatermasks.fill |
| 27 | `family` | Famille | violet | person.2.fill |
| 28 | `health` | Santé | mint | cross.case.fill |
| 29 | `gift` | Cadeau | indigo | gift.fill |
| 30 | `education` | Éducation | bleu | graduationcap.fill |
| 31 | `pet` | Animaux | brun | pawprint.fill |
| 32 | `other` | Autre | gris | ellipsis.circle.fill |

> Note : `other` (Autre) est la **catégorie de repli**. C'est aussi la valeur
> forcée sur `category` quand une transaction porte une catégorie personnalisée
> (le lien réel se fait alors via la catégorie personnalisée, pas via `category`).

### Auto-détection de catégorie (bonus utile à porter)

À la création d'une transaction, la catégorie par défaut peut être **devinée à
partir du commentaire** (recherche de mots-clés, en minuscules). Exemples de
règles (ordre = priorité) :

- « loyer », « appartement » → **Loyer**
- « salaire », « paie », « travail » → **Salaire**
- « netflix », « spotify », « abonnement », « abo » → **Abonnement**
- « edf », « eau », « gaz », « électricité », « charge » → **Charges**
- « course », « supermarché », « leclerc », « carrefour », « lidl » → **Courses**
- « resto », « restaurant », « repas » → **Restaurant**
- « uber », « train », « taxi », « bus », « métro » → **Transport**
- … (voir la liste complète des mots-clés dans le code source pour le portage exact)
- **Défaut** si aucun mot-clé : `Revenu` si type = revenu, sinon `Dépense`.

---

## 4. Catégories de compte

**10 catégories** de compte (appelées « styles » de compte). Fixes, non
modifiables. Servent à donner une icône + couleur à un compte. Il n'existe **pas**
de catégorie de compte personnalisée (contrairement aux transactions).

| # | Clé (id) | Libellé | Couleur | Icône iOS (à remapper) |
|---|----------|---------|---------|------------------------|
| 1 | `bank` | Courant | bleu | building.columns.fill |
| 2 | `savings` | Épargne | orange | banknote.fill |
| 3 | `investment` | Investissement | violet | chart.line.uptrend.xyaxis |
| 4 | `business` | Professionnel | indigo | briefcase.fill |
| 5 | `travel` | Voyage | teal | airplane |
| 6 | `grocery` | Courses | vert | cart.fill |
| 7 | `student` | Étudiant | cyan | graduationcap.fill |
| 8 | `family` | Famille | rose | person.2.fill |
| 9 | `property` | Immobilier | brun | house.fill |
| 10 | `entertainment` | Loisirs | rouge | gamecontroller.fill |

### Auto-détection du style de compte (bonus)

À la création d'un compte, le style par défaut est **deviné depuis le nom** :
« courant / principal / bnp… » → **Courant** ; « livret / épargne / pel » →
**Épargne** ; « pea / crypto / bourse » → **Investissement** ; etc. Défaut =
**Courant** (`bank`).

---

## 5. Affichage des sélecteurs de catégorie

Deux sélecteurs différents, avec deux comportements distincts.

### 5.1 Sélecteur de catégorie de **transaction** (paginé + swipe)

Utilisé dans l'écran « Ajouter une transaction ». Comportement :

- **Grille de 5 colonnes × 2 lignes = 10 tuiles par page.**
- **Pagination horizontale par swipe** (comme des pages), avec un **indicateur
  de pages** (points/dots) sous la grille, cliquables. L'indicateur n'apparaît
  que s'il y a plus d'une page.
- Hauteur de grille fixe (≈ 168 pt) pour que la zone ne « saute » pas d'une
  page à l'autre.
- **Contenu et ordre des tuiles** (concaténés à la suite, sur plusieurs pages) :
  1. les **32 catégories par défaut** (dans l'ordre du §3) ;
  2. puis les **catégories personnalisées** du compte (triées par nom, ordre
     alphabétique insensible à la casse) ;
  3. puis, tout à la fin, une **tuile spéciale « + Ajouter »** (bouton de
     création d'une nouvelle catégorie personnalisée).
- **Aspect d'une tuile** : un cercle coloré (fond = couleur à faible opacité)
  contenant l'icône, surmonté du libellé sur une ligne. Quand la tuile est
  sélectionnée : opacité du fond plus forte + **anneau** (bordure) de la couleur
  autour du cercle + libellé coloré.
- **Sélection** : un **tap** sélectionne la catégorie. Une seule à la fois.
- **Appui long** (long press, ~0.45 s) sur une tuile :
  - sur une **catégorie personnalisée** → ouvre un petit menu contextuel
    (popover ancré sur la tuile) avec **Modifier** / **Supprimer** ;
  - sur une **catégorie par défaut** → affiche « Non modifiable » (cadenas) ;
  - sur la tuile « + Ajouter » → rien (le tap suffit).
- **Retour haptique** au déclenchement de l'appui long.
- **Navigation auto** : à l'ouverture, le sélecteur se positionne sur la page
  qui contient la catégorie actuellement sélectionnée ; si la sélection change,
  il change de page avec animation.

**Modèle de sélection (important pour Android)** : la sélection est portée par
**deux valeurs combinées** :
- `selectedCategory` : la clé d'une catégorie par défaut ;
- `selectedCustomCategoryId` : l'identifiant (nullable) d'une catégorie
  personnalisée.

Règle : une **catégorie personnalisée est sélectionnée** quand
`selectedCustomCategoryId != null`. Dans ce cas, `selectedCategory` est forcé à
`Autre` (`other`). Sinon (id null), c'est la catégorie par défaut qui est
sélectionnée. À la sélection :
- tap sur une par défaut → `selectedCategory = <clé>`, `selectedCustomCategoryId = null` ;
- tap sur une personnalisée → `selectedCategory = other`, `selectedCustomCategoryId = <id>`.

### 5.2 Sélecteur de catégorie de **compte** (grille dépliable, pas de swipe)

Utilisé dans l'écran « Ajouter un compte ». Comportement :

- **Grille de 5 colonnes** (configurable), **sans pagination ni swipe**.
- Peut fonctionner en **mode replié** : n'afficher que les N premières lignes,
  avec un bouton **« Voir tout » / « Voir moins »** pour déplier/replier
  (animé). Dans l'écran d'ajout de compte, il est appelé **sans repli** → les 10
  styles sont tous affichés d'un coup (2 lignes de 5).
- Si le mode replié masque l'élément sélectionné, celui-ci est quand même rendu
  visible (remplace le dernier de la zone visible) pour qu'on voie toujours son
  choix.
- **Aspect des tuiles** identique au sélecteur de transaction (cercle coloré +
  icône + libellé, anneau si sélectionné).
- **Sélection** : simple tap. Pas d'appui long, pas de menu, pas de création
  (les styles de compte ne sont pas personnalisables).

### Récap des deux sélecteurs

| | Transaction | Compte |
|---|---|---|
| Colonnes | 5 | 5 |
| Lignes | 2 par page | toutes (ou N si replié) |
| Navigation | **swipe horizontal paginé** + dots | statique, bouton « Voir tout » |
| Nb d'items | 32 + perso + bouton « + » | 10 |
| Catégories perso | oui (+ bouton d'ajout) | non |
| Appui long / menu | oui (modifier/supprimer) | non |

---

## 6. Catégories personnalisées — logique complète

Fonctionnalité **propre aux transactions** (pas aux comptes). Une catégorie
personnalisée **appartient à un compte** : chaque compte a sa propre liste. Elles
sont créées/modifiées/supprimées localement par l'utilisateur, ou créées
automatiquement à l'import CSV (§2).

### 6.1 Modèle de données

Une catégorie personnalisée =

| Champ | Type | Défaut | Rôle |
|-------|------|--------|------|
| `id` | identifiant unique (UUID) | généré | identité stable |
| `name` | texte | — | nom affiché, **max 15 caractères** |
| `symbol` | nom d'icône | `tag.fill` | icône (SF Symbol iOS → remapper) |
| `colorHex` | couleur hex `#RRGGBB` | `#8E8E93` (gris système) | couleur |
| `account` | référence compte | — | compte propriétaire |

Relations : une catégorie personnalisée est référencée par des **transactions**,
des **raccourcis widget** et des **récurrences**. Règle de suppression =
**nullify** : supprimer une catégorie personnalisée **ne supprime pas** les
transactions/raccourcis/récurrences qui l'utilisaient ; elle met simplement leur
référence à « nulle » (ils retombent alors sur la catégorie par défaut `Autre`).

### 6.2 Comment une transaction « porte » sa catégorie

Une transaction a deux champs liés à la catégorie :
- `category` : une **catégorie par défaut** (clé) ;
- `customCategory` : une **référence** optionnelle vers une catégorie
  personnalisée.

**Règle d'affichage** (à reproduire) : si `customCategory != null`, on affiche le
nom/icône/couleur de la catégorie personnalisée ; sinon ceux de la catégorie par
défaut. Concrètement :
- libellé affiché = `customCategory?.name ?? category.label`
- icône affichée = `customCategory?.symbol ?? category.icon`
- couleur affichée = `customCategory?.color ?? category.color`

Quand une catégorie personnalisée est assignée, `category` est mis à `Autre`
(`other`) par convention.

Il existe aussi un champ temporaire `importedCategoryName` (texte, nullable) qui
sert **uniquement pendant l'import CSV** à mémoriser un libellé de catégorie
inconnu avant de le résoudre en vraie catégorie personnalisée (voir §2). Il est
remis à null une fois le rattachement fait.

### 6.3 Normalisation des noms (dé-duplication)

Toute comparaison de nom de catégorie (validation, résolution à l'import,
rattachement) passe par une **normalisation** :

```
normaliser(nom) = trim(espaces) puis pliage insensible à la casse ET aux accents
```

Ainsi `« Épargne »`, `« epargne »` et `« ÉPARGNE  »` sont considérés comme
**identiques**. C'est ce qui évite les doublons de catégories personnalisées.

### 6.4 Création / édition — la feuille (sheet)

Ouverture :
- tap sur la tuile **« + Ajouter »** du sélecteur de transaction → sheet en mode
  **création** ;
- appui long sur une catégorie personnalisée → menu → **Modifier** → sheet en
  mode **édition** (pré-remplie).

Contenu de la sheet (formulaire), dans l'ordre :

1. **Section « Nom »** : un champ texte.
   - Auto-capitalisation par mot, correction auto désactivée.
   - **Limité à 15 caractères** (troncature au-delà).
   - Un compteur `N/15` affiché sous le champ.
2. **Section « Couleur »** : un sélecteur de couleur (color picker), **sans
   gestion d'opacité**. La couleur choisie est convertie en `#RRGGBB`.
3. **Section « Symbole »** :
   - un aperçu (cercle de la couleur choisie + icône sélectionnée + nom du
     symbole) ;
   - une **grille de 6 colonnes** proposant **~72 symboles** prédéfinis ; tap
     pour choisir ; l'icône choisie a un anneau de la couleur.
4. **Barre de navigation** : bouton **Annuler** (ferme sans rien faire) et bouton
   **Valider**.

**Validation au « Valider »** (à reproduire) :
- nom obligatoire (non vide après trim) ;
- nom **≠** libellé d'une catégorie par défaut (comparaison normalisée) →
  sinon « Nom déjà utilisé. » ;
- nom **≠** nom d'une autre catégorie personnalisée du même compte (normalisé,
  en excluant la catégorie en cours d'édition) → sinon « Nom déjà utilisé. » ;
- si erreur → alerte, la sheet reste ouverte.

À la sauvegarde réussie :
- **création** : nouvelle catégorie ajoutée au compte, puis re-lien des
  transactions importées correspondantes (§2 « rattachement différé »), puis la
  nouvelle catégorie devient la sélection courante ;
- **édition** : mise à jour nom/symbole/couleur, re-lien, la catégorie éditée
  reste sélectionnée.

Liste des ~72 symboles proposés (noms SF Symbols iOS — à remapper vers un jeu
d'icônes Android équivalent) :

```
tag.fill, questionmark.circle.fill, cart.fill, basket.fill, fork.knife,
cup.and.saucer.fill, takeoutbag.and.cup.and.straw.fill, birthday.cake.fill,
wineglass.fill, house.fill, building.2.fill, bed.double.fill, bolt.fill,
drop.fill, flame.fill, wifi, phone.fill, car.fill, bus.fill, bicycle, tram.fill,
fuelpump.fill, airplane, banknote.fill, creditcard.fill, eurosign.circle.fill,
dollarsign.circle.fill, sterlingsign.circle.fill, yensign.circle.fill,
chart.line.uptrend.xyaxis, chart.bar.fill, chart.pie.fill, briefcase.fill,
doc.text.fill, folder.fill, archivebox.fill, hammer.fill,
wrench.and.screwdriver.fill, cross.case.fill, pills.fill, stethoscope,
heart.fill, bandage.fill, gift.fill, graduationcap.fill, book.fill,
books.vertical.fill, figure.run, figure.walk, gamecontroller.fill, film.fill,
music.note, tv.fill, camera.fill, pawprint.fill, dog.fill, cat.fill, sparkles,
leaf.fill, tree.fill, carrot.fill, fish.fill, clock.fill, calendar, alarm.fill,
timer, shield.fill, lock.fill, mappin.and.ellipse, location.fill, suitcase.fill,
bag.fill
```

### 6.5 Suppression

- Appui long sur la tuile personnalisée → menu → **Supprimer** → **alerte de
  confirmation** (« Supprimer la catégorie ? / Suppression définitive. »).
- À la suppression : la catégorie est retirée ; toutes les transactions (et
  raccourcis/récurrences) qui la référençaient voient leur référence remise à
  null (règle *nullify*) → elles retombent sur `Autre`.
- Si la catégorie supprimée était la sélection courante dans le sélecteur, la
  sélection retombe sur `Autre`.

### 6.6 Où on voit les catégories personnalisées dans l'UI

- Dans le **sélecteur de transaction** (§5.1) : ajoutées **après** les 32
  catégories par défaut, avant le bouton « + Ajouter », donc sur les pages
  suivantes du swipe si le total dépasse 10 tuiles. Elles se comportent comme les
  autres tuiles (sélection au tap), avec en plus le menu au long-press.
- Sur une transaction déjà catégorisée (listes, lignes, analyses) : elle
  s'affiche avec le **nom / icône / couleur** de la catégorie personnalisée
  (règle du §6.2).

---

## 7. Checklist de portage Android

- [ ] Modèle `CustomCategory` (id, name, symbol, colorHex, accountId) + relation
      *nullify* vers transactions/raccourcis/récurrences.
- [ ] Transaction avec `category` (enum défaut) **et** `customCategoryId`
      (nullable) + champ temporaire `importedCategoryName`.
- [ ] Enum des **32 catégories par défaut** (mêmes **clés**, libellés, couleurs)
      + remap des icônes iOS → icônes Android.
- [ ] Enum des **10 styles de compte** (mêmes clés/libellés) + remap icônes.
- [ ] Export CSV : même **en-tête**, mêmes formats de colonnes, **échappement
      RFC 4180**, tri par date décroissante, exclusion potentielles/récurrences.
- [ ] Import CSV : parsing **RFC 4180**, réapplication du signe via `Type`,
      matching des catégories par défaut par libellé, mémorisation des libellés
      inconnus, puis **création automatique dé-dupliquée** des catégories
      personnalisées au commit.
- [ ] Normalisation des noms (trim + insensible casse/accents) partout.
- [ ] Sélecteur transaction : grille **5×2 paginée par swipe** + dots + tuile
      « + » + long-press (modifier/supprimer) + double binding de sélection.
- [ ] Sélecteur compte : grille **5 colonnes** statique (option « Voir tout »),
      sans personnalisation.
- [ ] Sheet de création/édition : nom (max 15 + compteur), color picker, grille
      6 colonnes de ~72 symboles, validation des doublons.
- [ ] Import en **2 étapes** (aperçu du nombre + confirmation) et avertissement
      « pas de dé-duplication des transactions ».
