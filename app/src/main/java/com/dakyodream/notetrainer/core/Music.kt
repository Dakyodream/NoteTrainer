package com.dakyodream.notetrainer.core

import kotlin.math.pow
import kotlin.random.Random

/** Représente une note de musique sur la partition, avec son octave. */
data class Note(
    /** Lettre de la note : C, D, E, F, G, A, B */
    val letter: Char,
    /** Altération : 0 = bécarre, 1 = dièse, -1 = bémol */
    val accidental: Int,
    /** Numéro d'octave (do scientifique : C4 = do3, 261,63 Hz) */
    val octave: Int
) {
    val id: String get() = "${letter}${if (accidental == 1) "♯" else if (accidental == -1) "♭" else ""}$octave"
    val displayName: String get() = "${letter}${if (accidental == 1) "#♯" else if (accidental == -1) "b♭" else ""} ($octave)"
}

object Notes {
    val LETTERS = listOf('C', 'D', 'E', 'F', 'G', 'A', 'B')

    /** Fréquence de base A4 = 440 Hz */
    const val A4_FREQ = 440.0
    const val A4_MIDI = 69

    fun midiNumber(note: Note): Int {
        val base = when (note.letter) {
            'C' -> 0; 'D' -> 2; 'E' -> 4; 'F' -> 5; 'G' -> 7; 'A' -> 9; 'B' -> 11
            else -> 0
        }
        return (note.octave + 1) * 12 + base + note.accidental
    }

    fun frequency(note: Note): Double =
        A4_FREQ * 2.0.pow((midiNumber(note) - A4_MIDI) / 12.0)

    fun fromMidi(midi: Int, preferSharps: Boolean): Note {
        val octave = midi / 12 - 1
        val pc = midi % 12
        val sharpLetters = listOf('C', 'C', 'D', 'D', 'E', 'F', 'F', 'G', 'G', 'A', 'A', 'B')
        val flatLetters = listOf('C', 'D', 'D', 'E', 'E', 'F', 'G', 'A', 'A', 'B', 'B', 'C')
        val sharpAcc = listOf(0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0)
        val flatAcc = listOf(0, -1, 0, -1, 0, 0, -1, 0, -1, 0, -1, 0)
        val letter = if (preferSharps) sharpLetters[pc] else flatLetters[pc]
        val acc = if (preferSharps) sharpAcc[pc] else flatAcc[pc]
        return Note(letter, acc, octave)
    }

    fun frequencyRangeOfClef(clef: Clef): IntRange = when (clef) {
        Clef.TREBLE -> 60..81   // C4..A5
        Clef.BASS -> 40..60     // E2..C4
    }

    fun randomInClef(clef: Clef, difficulty: Difficulty, withAccidentals: Boolean, rng: Random): Note {
        val range = frequencyRangeOfClef(clef)
        val midi = range.first + rng.nextInt(range.last - range.first + 1)
        val n = fromMidi(midi, preferSharps = true)
        return if (withAccidentals) {
            val r = rng.nextInt(3)
            when {
                r == 0 -> Note(n.letter, 1, n.octave)
                r == 1 -> Note(n.letter, -1, n.octave)
                else -> Note(n.letter, 0, n.octave)
            }
        } else Note(n.letter, 0, n.octave)
    }

    fun fromMidiSimple(midi: Int): Note {
        val sharpLetters = listOf('C', 'C', 'D', 'D', 'E', 'F', 'F', 'G', 'G', 'A', 'A', 'B')
        val sharpAcc = listOf(0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0)
        val pc = midi % 12
        val octave = midi / 12 - 1
        return Note(sharpLetters[pc], sharpAcc[pc], octave)
    }
}

enum class Difficulty(val label: String, val rounds: Int, val secondsPerRound: Int) {
    EASY("Facile", 10, 15),
    MEDIUM("Moyen", 10, 12),
    HARD("Difficile", 12, 10),
    EXPERT("Expert", 12, 8);

    val withAccidentals: Boolean get() = this != EASY

    companion object {
        fun fromName(name: String?): Difficulty =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: EASY
    }
}

enum class Clef(val label: String) {
    TREBLE("Clé de Sol"),
    BASS("Clé de Fa");
}

enum class GameMode(val label: String, val description: String) {
    NAME_THE_NOTE("Note ➜ Nom", "Une note s'affiche sur la partition, trouvez son nom."),
    PLACE_THE_NOTE("Nom ➜ Note", "Un nom de note s'affiche, placez-la sur la partition."),
    EAR_TRAINING("Oreille ➜ Note", "Écoutez le son et placez la note sur la partition.");
}
