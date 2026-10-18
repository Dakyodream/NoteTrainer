# NoteTrainer 🎵

[![Android CI](https://github.com/Dakyodream/NoteTrainer/actions/workflows/android.yml/badge.svg)](https://github.com/Dakyodream/NoteTrainer/actions/workflows/android.yml)
[![Licence: MIT](https://img.shields.io/badge/Licence-MIT-blue.svg)](LICENSE)

**NoteTrainer** is a **100% offline, free and open-source** Android application (MIT licence) designed for practising sight-reading musical notes on a stave. It is tailored for beginner to advanced piano students and music teachers who wish to track student progress over time.

## 🎮 Game Modes

| Mode | Description |
|------|-------------|
| **♩ ➜ Name** | A note is displayed on the stave (treble or bass clef); select its correct name from the choice buttons (C D E... or Do Ré Mi...). |
| **Name ➜ ♩** | A note name is displayed alongside its octave (e.g. C4); pick an accidental (♭ ♮ ♯) and tap the correct position directly on the stave. |
| **🔊 ➜ Name** | The app plays an audio sequence of notes (Simon-style memory game); listen, replay as needed, and place each note on the stave in the correct order. |

## 🎚️ Difficulties & Clefs

- **Easy** — Natural notes only, 10 rounds.
- **Medium** — Accidentals (sharps and flats) included, 10 rounds.
- **Hard** — 12 rounds with longer note sequences.
- **Expert** — 12 fast rounds covering extended staves and all accidentals.
- **Supported Clefs** — Treble clef (G clef) and Bass clef (F clef).
- **Notations** — English notation (A, B, C...) and French/Solfège notation (Do, Ré, Mi...).

## 📊 Statistics & Privacy (GDPR Compliance)

- **100% Offline** — Zero Android permissions required (no internet access, no external storage, no microphone).
- **Local Progress Tracking** — Tracks games played, average scores, high scores, and accuracy breakdown per game mode and difficulty.
- **GDPR Rights Built-in** — One-tap JSON export of all personal statistics and instant data deletion capability.
- **Backup Exclusion** — Local stats data (`stats.json`) is explicitly excluded from Android cloud backups and device-to-device transfers to ensure data never leaves your device.

## 🔊 Local Audio Synthesis

All note sounds are **synthesised locally in real-time PCM** (`AudioTrack` / `SoundPool`). No large bundled audio files, network requests, or third-party paid APIs are used.

## 🛠️ Technical Stack

- **Language & Framework**: Kotlin 2.2 + Jetpack Compose (Material 3)
- **Stave Rendering**: 100% custom native vector graphics drawn via `Compose Canvas`, using the official Steinberg Bravura SMuFL music font
- **SDK Compatibility**: `minSdk = 26` (Android 8.0+), `targetSdk = 35` (Android 15)
- **Build System**: Gradle 9.x with Android Gradle Plugin (AGP) 9.x

## 🧱 Building & Installation

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Ladybug / 2024.2+ recommended)
- JDK 17+

### Command Line Build
```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :app:testDebugUnitTest
```

The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## ⚖️ Licence

Distributed under the [MIT Licence](LICENSE). Free, ad-free, and respects user privacy completely.
