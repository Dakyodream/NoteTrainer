# NoteTrainer — AI Agent / LLM Context File

Context file intended for AI coding assistants (Claude, Cursor, Copilot, Gemini, etc.) working on this repository.
It summarizes the architecture, technical decisions, conventions, and constraints.

## Project Overview

NoteTrainer is a 100% offline Android application for learning and practising musical sight-reading on a stave (Simon-style game). Designed for piano learners and music teachers. No ads, no data collection, no network permissions required. Supports French (Do Ré Mi...) and English (A B C...) notation systems dynamically.

## Tech Stack

- **Language & UI**: Kotlin 2.2 + Jetpack Compose (Material 3), Navigation via `navigation-compose`
- **Build System**: Gradle 9.x / AGP 9.x, Java/JVM 17 (`minSdk = 26`, `compileSdk/targetSdk = 35`)
- **Score Renderer**: 100% custom native Kotlin Compose Canvas vector renderer (0 native C++ dependencies)
- **Audio**: Real-time local PCM audio synthesis (`AudioTrack` / `SoundPool` in `core/AudioPlayer.kt`)
- **i18n**: `res/values/strings.xml` (French, default) + `res/values-en/strings.xml` (English)

## Architecture

```
app/src/main/java/com/dakyodream/notetrainer/
├── MainActivity.kt          # Root Activity: Theme state (Light/Dark/Auto), Navigation host, Notation state
├── core/
│   ├── Music.kt             # Note(letter, accidental, octave), Notes, Clef, Difficulty, GameMode
│   ├── GameEngine.kt        # Game state engine (StateFlow), answer validation, life tracking
│   ├── AudioPlayer.kt       # Local PCM audio synthesis (AudioTrack / SoundPool)
│   └── Stats.kt             # Weekly/Monthly/All-time stats, atomic file persistence (StatsFileStore/StatsStore)
└── ui/
    ├── Theme.kt             # ThemeMode (SYSTEM/LIGHT/DARK), NoteTrainerTheme, staffInkColor()
    ├── StaffView.kt         # Custom Compose Canvas stave renderer (Tap & Drag gesture handling)
    └── screens/
        ├── MenuScreen.kt    # Top bar (Burger menu + Theme toggle icons), Mode/Difficulty/Clef selectors
        ├── GameScreen.kt    # Game UI for all 3 game modes
        ├── StatsScreen.kt   # Stats filtering, GDPR Export (JSON) & Erase
        └── InfoScreens.kt   # Help, Credits, and Licence screens
```

## Musical Data Model (`core/Music.kt`)

- `Note`: `letter` (`'C'`..'B', internally always English letter), `accidental` (0/±1), `octave` (scientific octave notation, `C4` = middle C = MIDI 60). `midi = (octave + 1) * 12 + PC[letter] + accidental`.
- **Stave Geometry**: `1 step = half a space` (demi-interligne). Step 0 = top line of the stave (F5 in Treble clef, A3 in Bass clef). Positive step = downwards on the stave. `Notes.topLineDiatonic(clef)` and `Notes.stepToMidi(step, clef)` handle the conversions.
- **Playable Range**: `RANGE_MIN_STEP = -4` to `RANGE_MAX_STEP = 12` (4 notes beyond the top/bottom lines of the stave).

## Game Modes & Dynamic Notation

- `♩ ➜ Name` (`NAME_THE_NOTE`): Note displayed on stave → user selects note name button.
- `Name ➜ ♩` (`PLACE_THE_NOTE`): Note name displayed → user selects accidental (♭ ♮ ♯) and taps the stave.
- `🔊 ➜ Name` (`EAR_TRAINING`): Audio sequence played → user listens and places notes on the stave in sequence.
- Note name formatting uses `Notes.noteName(letter, notation)` (`Notation.FRENCH` by default). In mode title strings, the `'A'` placeholder adapts dynamically to the chosen notation ("La" in FR, "A" in EN).

## Stave Vector Renderer (`ui/StaffView.kt`)

`ui/StaffView.kt` uses a 100% custom native Compose `Canvas` renderer:
1. **Full Width**: 5 stave lines drawn from $x = 0$ to $x = \text{width}$.
2. **Step Calculation**: `spaceHeight = 18.dp`, `stepSize = spaceHeight / 2f`. Middle line (line 3) corresponds to **step 4** ($Y = \text{centerY}$). For any step $s \in [-4, 12]$: $Y(s) = \text{centerY} + (s - 4) \times \text{stepSize}$.
3. **SMuFL Clefs**: Drawn using official Steinberg Bravura font (`\uE050` Treble clef, `\uE062` Bass clef) at $x = 20\text{dp}$.
4. **Ledger Lines**: Automatically generated for out-of-stave notes ($s > 8$ below or $s < 0$ above).
5. **Stem Direction**: Step $\ge 4$ (middle line or lower) $\rightarrow$ stem pointing up on right. Step $< 4$ (above middle line) $\rightarrow$ stem pointing down on left.
6. **Touch & Drag**: `detectTapGestures` and `detectDragGestures` convert Y position to step: $\text{step} = \text{round}((\text{y} - \text{centerY}) / \text{stepSize} + 4)$.

## UI / UX Conventions

- **Top Header**: Burger icon on the left (Notation FR/EN, Help, Credits, Licence), Theme icons on the right (Light ☀ / Dark 🌙 / Auto 🖥). No text buttons for theme toggling.
- **Ink Color**: `staffInkColor()` in `Theme.kt` adapts to current theme (dark ink on light background, light ink on dark background).
- **State Management**: `ThemeMode` lives in `MainActivity` and passes down via callbacks.
- **Preferences**: Persisted via `SettingsStore` (`SharedPreferences`).

## Privacy & GDPR Compliance

- **100% Offline**: Zero permissions, zero tracking, zero network calls (`PRIVACY.md`).
- **Data Exclusion**: `stats.json` is strictly excluded from backups in `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`.
- **User Rights**: Stats Screen offers JSON Export (via Storage Access Framework `ActivityResultContracts.CreateDocument`) and Clear Data (with confirmation dialog).

## Build & Test Commands

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :app:testDebugUnitTest
```
