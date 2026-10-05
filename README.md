# NoteTrainer 🎵

[![Android CI](https://github.com/Dakyodream/NoteTrainer/actions/workflows/android.yml/badge.svg)](https://github.com/Dakyodream/NoteTrainer/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**NoteTrainer** est un jeu Android **open source et gratuit** (licence MIT) pour apprendre à lire les notes sur une partition — dans l'esprit du jeu Simon. Pensé pour les apprenants pianistes qui veulent mémoriser le nom des notes et leur position en clé de Sol et de Fa.

## 🎮 Modes de jeu

| Mode | Principe |
|------|----------|
| **Note ➜ Nom** | Une note s'affiche sur la partition (clé de Sol ou de Fa) ; trouvez son nom parmi les boutons proposés. |
| **Nom ➜ Note** | Le nom de la note s'affiche (avec son octave) ; placez-la au bon endroit sur la portée. Choisissez l'altération (♭ ♮ ♯) puis touchez la partition. |
| **Oreille ➜ Note** | Le jeu joue une séquence de notes (1 s par note, façon Simon) ; réécoutez puis placez chaque note sur la portée, dans l'ordre. |

## 🎚️ Difficultés

- **Facile** — notes naturelles uniquement, 3 propositions, séquence de 3 notes.
- **Moyen** — dièses/bémols possibles, séquence de 4 notes.
- **Difficile** — 12 manches, séquence de 5 notes.
- **Expert** — tout est permis, séquence de 6 notes.

Chaque partie se joue avec **3 vies** ; un score et le meilleur score sont affichés.

## 📱 Captures d'esprit du jeu

- Menu principal : choix du mode, de la difficulté et de la clé (Sol / Fa).
- Écrans **Aide**, **Crédits** et **Licence** accessibles depuis le menu.
- Partition dessinée intégralement en **Jetpack Compose Canvas** (clé de Sol, clé de Fa, lignes supplémentaires, altérations).

## 🔊 Audio

Les sons sont **synthétisés localement** (onde sinusoïdale + harmoniques + enveloppe, générée en WAV dans le cache de l'application) : aucun fichier audio embarqué, aucune dépendance réseau, aucune API payante. Le jeu fonctionne 100 % hors-ligne.

## 🛠️ Compilation

1. Installez [Android Studio](https://developer.android.com/studio) (Koala ou plus récent).
2. Ouvrez le dossier du projet : `File > Open` puis sélectionnez ce dépôt.
3. Laissez Gradle synchroniser (le wrapper téléchargera Gradle 8.7 automatiquement).
4. Branchez un appareil ou lancez un émulateur, puis `Run ▶`.

En ligne de commande :

```bash
./gradlew assembleDebug   # APK de debug dans app/build/outputs/apk/debug/
```

## 🧱 Stack technique

- **Kotlin** 1.9, **Jetpack Compose** (BOM 2024.06) + **Material 3**
- Architecture simple : `core` (logique de jeu, théorie musicale, synthèse audio) + `ui` (Compose)
- minSdk 24 (Android 7.0+), targetSdk 34

## 🤝 Contribuer

Les contributions sont les bienvenues (nouveaux modes, dessin des clés, statistiques d'apprentissage…). Ouvrez une issue ou une pull request.

## ⚖️ Licence

Distribué sous licence [MIT](LICENSE). Projet gratuit, sans publicité et sans collecte de données.

## 🙏 Remerciements

- Communauté des apprenants pianistes 🎹
- Documentation Android et exemples Compose
