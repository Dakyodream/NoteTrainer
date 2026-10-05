# NoteTrainer — Contexte pour assistants IA / LLM

Fichier de contexte destiné à tout assistant IA (Claude, Cursor, Copilot, etc.) travaillant sur ce dépôt.
Il récapitule l'architecture, les décisions techniques prises et les pièges rencontrés.

## Objectif du projet

Application Android 100 % hors-ligne d'entraînement à la lecture de notes de musique,
destinée à compléter une éducation musicale (utilisable par un professeur pour suivre
l'entraînement d'un étudiant via la page statistiques). Pas de pub, pas de données collectées,
pas de connexion requise. Notation française par défaut (Do Ré Mi…), bascule possible en
notation anglaise (A B C…).

## Stack

- Kotlin + Jetpack Compose (Material 3), navigation via `navigation-compose`
- Gradle 9.x / AGP 9.x, Kotlin 2.2 (plugin `org.jetbrains.kotlin.plugin.compose`)
- `minSdk = 26` (exigé par alphaTab), `compileSdk/targetSdk = 35`, Java/JVM 17
- Rendu de partition : **alphaTab** (`net.alphatab:alphaTab:1.6.1`, licence MPL-2.0)
- Sons : synthèse locale (voir `core/AudioPlayer.kt`)
- i18n : `res/values/strings.xml` (fr, défaut) + `res/values-en/strings.xml` (en)

## Architecture

```
app/src/main/java/com/dakyodream/notetrainer/
├── MainActivity.kt          # thème (clair/sombre/auto), navigation entre écrans, état notation
├── core/
│   ├── Music.kt             # Note(letter, accidental, octave), Notes.*, Clef, Difficulty, GameMode
│   ├── GameEngine.kt        # logique de jeu (StateFlow), vérification des réponses
│   ├── AudioPlayer.kt       # synthèse des notes (AudioTrack)
│   └── Stats.kt             # statistiques hebdo/mensuelles, persistance locale
└── ui/
    ├── Theme.kt             # ThemeMode (SYSTEM/LIGHT/DARK), NoteTrainerTheme, staffInkColor()
    ├── StaffView.kt         # portée alphaTab wrappée en Compose (voir pièges ci-dessous)
    └── screens/
        ├── MenuScreen.kt    # header burger + icônes thème, sélection mode/difficulté/clé
        ├── GameScreen.kt    # 3 modes de jeu (voir titres ci-dessous)
        ├── StatsScreen.kt   # stats par semaine/mois
        └── InfoScreens.kt   # aide, crédits, licence
```

## Modèle de données musical (core/Music.kt)

- `Note` : `letter` (C..B, interne toujours anglo-saxon), `accidental` (0/±1), `octave`
  (octave scientifique, C4 = do3 = midi 60). `midi = (octave + 1) * 12 + PC[letter] + accidental`.
- Géométrie portée : **1 step = demi-interligne** ; step 0 = ligne du haut
  (F5 en clé de Sol, A3 en clé de Fa) ; step positif = vers le bas.
  `Notes.topLineDiatonic(clef)` et `Notes.stepToMidi(step, clef)` font la conversion.
- Étendue de jeu : `RANGE_MIN_STEP = -4` à `RANGE_MAX_STEP = 12`
  (4 notes de chaque côté au-delà des lignes extrêmes de la portée).

## Titres des modes (ne pas renommer en anglais !)

L'utilisateur veut des titres avec symboles + notation dynamique :
- ♩ ➜ La (NAME_THE_NOTE) — boutons : tous les noms de notes dans la notation choisie
- La ➜ ♩ (PLACE_THE_NOTE) — placement sur la portée via tap, altérations ♭ ♮ ♯
- 🔊 ➜ La (EAR_TRAINING) — séquence jouée à l'oreille, type Simon

Le nom de la note s'affiche selon `Notes.noteName(letter, notation)` avec
`Notation.FRENCH` par défaut. **Le "A" dans les titres boutons doit suivre la notation**
(ex "La" en FR, "A" en EN) — bug déjà corrigé une fois, ne pas régresser.

## UI : conventions

- Header en haut du MenuScreen : burger à gauche (notation FR/EN, aide, crédits, licence),
  icônes thème à droite (clair ☀ / sombre 🌙 / auto 🖥), **pas de boutons texte** pour le thème.
- Couleur d'encre de la portée : `staffInkColor()` dans Theme.kt — suit le thème
  (fond clair → encre sombre, et inversement).
- Texte des boutons de difficulté : centré.

## Rendu de partition : alphaTab — pièges importants

`ui/StaffView.kt` utilise alphaTab en **mode headless** : `ScoreRenderer` produit
directement des Bitmaps Android affichées dans un `Image()` Compose (pas de
`AlphaTabView`/ScrollViews intermédiaires dans l'arbre UI).

1. **Moteur de rendu** : `settings.core.engine = "android"` (Canvas Android pur).
   NE PAS utiliser `"skia"` : la lib native `libalphaskiajni.so` n'est PAS alignée
   16 KB → refus de chargement sur Android 15+ en mode pages 16 KB → partition vide.
   (alphaSkia n'a pas de correctif 16 KB à ce jour.)
   ATTENTION : NE PAS exclure libalphaskiajni.so du packaging —
   AndroidEnvironment.initializeAndroid fait System.loadLibrary("alphaskiajni")
   → UnsatisfiedLinkError → crash au lancement d'un jeu (déjà rencontré).
   ensurePlatform() tolère néanmoins un échec de chargement alphaSkia (le moteur
   "android" n'en dépend pas : Bravura et la densité sont initialisés avant).
2. **Initialisation plateforme obligatoire** : la police Bravura
   (`AndroidCanvas.MusicFont`) et `Environment.highDpiFactor` ne sont initialisées
   QUE par `AlphaTabView.init` (via `AndroidEnvironment.initializeAndroid`, internal).
   → `ensurePlatform()` instancie une `AlphaTabView(context, null)` hors-écran une
   seule fois avant tout rendu. SANS CELA : bitmap de taille 0 → rectangle vide.
3. **Lazy loading** : `core.enableLazyLoading = false` obligatoire en headless —
   sinon les tranches sont enregistrées "lazy" et `renderLazyPartial` n'est jamais
   appelé (rôle d'`AlphaTabView`) → rien ne s'affiche.
4. **Unités** : `renderer.width` = pixels / densité (unités logiques, comme
   `AndroidViewContainer.width`). `registerPartial` multiplie déjà x/y/totaux par
   `display.scale` ; `AndroidCanvas.beginRender` multiplie par `highDpiFactor`
   (= densité). Pour l'assemblage des tranches et le mapping du tap : multiplier
   par `density` uniquement.
5. **Rendu initial** : différer jusqu'au premier layout (`BoxWithConstraints`),
   jamais rendre avec largeur 0 (sinon cadre vide).
6. **Contenu AlphaTex** : `\instrument 0` obligatoire (instrument par défaut = 25
   guitare → alphaTab afficherait une tablature en plus de la portée).
   **Ordre 1.6.1** : `\instrument` = staff meta → AVANT le `.` ; `\clef` = bar meta
   → APRÈS le `.`. Format : `\instrument 0 . \clef treble c4` (clé :
   `treble`/`bass` ; silence : `r` ; altérations : `#`/`b` collés à la lettre ;
   octave = octave scientifique : `c4` = do3). Inverser l'ordre déclenche
   "Error on block metaDataTags" (crash UnsupportedFormatError).
   En 1.6.x le `/` de durée n'existe pas (c'est un commentaire), la durée se met
   après `:` (inutile ici, noire par défaut).
7. **Tap sur la portée** : géométrie via `renderer.boundsLookup`
   (`staffSystems[0].bars[0].lineAlignedBounds` : `.y` = ligne du haut, `.h` = 8 steps ;
   `finish(scale)` est appelé avant `renderFinished`). La bitmap est affichée en
   `ContentScale.FillWidth` → facteur `bmpWidth / displayedWidth` pour convertir le tap.
8. **API alphaTab 1.6.1 en Kotlin** : membres statiques/enum en PascalCase
   (`PlayerMode.Disabled`), propriétés d'instance en camelCase
   (`settings.display.scale`), `Environment.HighDpiFactor` est `internal`
   (utiliser la densité Compose à la place). Tout le fichier alphaTab requiert
   `@OptIn(ExperimentalContracts::class, ExperimentalUnsignedTypes::class)`
   (opt-in au niveau fichier dans StaffView.kt).
9. `net.alphatab:alphaTab` embarque la police Bravura (`Bravura.otf`) dans ses assets —
   rien à embarquer côté app. Le filtre assets de l'AAR exclut `.ttf` mais PAS `.otf`.
10. ProGuard (build release, R8 activé) : règles keep pour `alphaTab.**` et
   `net.alphaskia.**` dans `app/proguard-rules.pro` (code transpilé, sensible au rename).

## Thème

`ThemeMode` (SYSTEM/LIGHT/DARK) porté par une composable racine (MainActivity),
fourni via `LocalThemeMode`. Ne pas tenter de réassigner un `val` délégué
`by rememberSaveable` depuis un enfant (erreur "val cannot be reassigned" déjà
rencontrée) : l'état vit dans MainActivity et descend via paramètres/callback.

## Internationalisation

Toutes les chaînes UI dans `strings.xml` (fr) et `values-en/strings.xml`.
Penser à mettre à jour les DEUX fichiers. Les titres de modes contiennent
l'esperluette ➜ et le symbole ♩/🔊 voulus par l'utilisateur.

## Confidentialité / RGPD

- Promesse : 100 % hors-ligne, zéro permission, zéro collecte (voir `PRIVACY.md`).
- `stats.json` est EXCLU des backups et transferts : `res/xml/backup_rules.xml`
  (fullBackupContent) et `res/xml/data_extraction_rules.xml` (Android 12+).
  Si un nouveau fichier de données est ajouté, l'exclure aussi.
- `StatsFileStore` (core/Stats.kt) : testable sans Context, écriture atomique
  (tmp + rename), `clear()` pour l'effacement, `exportJson()` pour l'export
  (SAF `ActivityResultContracts.CreateDocument` — aucune permission requise).
- Pages Confidentialité/Statistiques exposent exporter (JSON) et effacer
  (dialog de confirmation) — chaînes fr + en obligatoires.

## Tests / CI

- Tests unitaires dans `app/src/test` (Music, GameEngine, StatsStore).
- Coroutines dans les tests : `Dispatchers.setMain(UnconfinedTestDispatcher())`
  (`kotlinx-coroutines-test`).

## Licence / crédits

- NoteTrainer : MIT (voir LICENSE)
- alphaTab : MPL-2.0 — mentionné dans `credits_body` (fr + en)
- Ne pas supprimer ces mentions.

## Environnement de build

- `gradle.properties` : `android.usesSdkInManifest.disallowed=false` (warning AGP
  non bloquant, disparaîtra en AGP 10)
- Le build est fait par l'utilisateur — la CI GitHub Actions (`.github/workflows/android.yml`, JDK 17) valide chaque commit (tests + assembleDebug) et publie l'APK en artifact — toujours finir par
  `./gradlew :app:assembleDebug` ou demander à l'utilisateur de lancer le build
  et de renvoyer le log en cas d'erreur.
