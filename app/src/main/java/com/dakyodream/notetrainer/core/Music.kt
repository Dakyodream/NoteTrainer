package com.dakyodream.notetrainer.core

import androidx.annotation.StringRes
import com.dakyodream.notetrainer.R
import kotlin.math.pow
import kotlin.random.Random

/** Représente une note de musique sur la partition, avec son octave. */
data class Note(
    /** Lettre de la note (notation anglo-saxonne interne) : C, D, E, F, G, A, B */
    val letter: Char,
    /** Altération : 0 = bécarre, 1 = dièse, -1 = bémol */
    val accidental: Int,
    /** Numéro d'octave scientifique (C4 = do3, 261,63 Hz) */
    val octave: Int
) {
    val id: String get() = "${letter}${if (accidental == 1) "♯" else if (accidental == -1) "♭" else ""}$octave"
    val midi: Int get() = Notes.midiNumber(this)
}

/** Notation d'affichage des notes. */
enum class Notation(@StringRes val labelRes: Int) {
    FRENCH(R.string.notation_fr),
    ENGLISH(R.string.notation_en)
}

object Notes {
    val LETTERS = listOf('C', 'D', 'E', 'F', 'G', 'A', 'B')
    private val PC = intArrayOf(0, 2, 4, 5, 7, 9, 11)

    private const val A4_FREQ = 440.0
    private const val A4_MIDI = 69

    fun noteName(letter: Char, notation: Notation): String = when (letter) {
        'C' -> if (notation == Notation.FRENCH) "Do" else "C"
        'D' -> if (notation == Notation.FRENCH) "Ré" else "D"
        'E' -> if (notation == Notation.FRENCH) "Mi" else "E"
        'F' -> if (notation == Notation.FRENCH) "Fa" else "F"
        'G' -> if (notation == Notation.FRENCH) "Sol" else "G"
        'A' -> if (notation == Notation.FRENCH) "La" else "A"
        'B' -> if (notation == Notation.FRENCH) "Si" else "B"
        else -> "?"
    }

    fun accidentalSymbol(accidental: Int): String =
        when (accidental) { 1 -> "♯"; -1 -> "♭"; else -> "" }

    fun noteName(note: Note, notation: Notation): String =
        noteName(note.letter, notation) + accidentalSymbol(note.accidental)

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
        val pc = ((midi % 12) + 12) % 12
        val octave = midi / 12 - 1
        val sharpLetters = listOf('C', 'C', 'D', 'D', 'E', 'F', 'F', 'G', 'G', 'A', 'A', 'B')
        val sharpAcc = listOf(0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0)
        val flatLetters = listOf('C', 'D', 'D', 'E', 'E', 'F', 'G', 'A', 'A', 'B', 'B', 'C')
        val flatAcc = listOf(0, -1, 0, -1, 0, 0, -1, 0, -1, 0, -1, 0)
        return if (preferSharps) {
            val letter = sharpLetters[pc]
            val acc = sharpAcc[pc]
            // B♯ : l'octave imprimée est celle du B, pas du C naturel
            val oct = if (letter == 'B' && acc == 1) (midi - 1) / 12 - 1 else octave
            Note(letter, acc, oct)
        } else {
            val letter = flatLetters[pc]
            val acc = flatAcc[pc]
            // C♭ : l'octave imprimée est celle du C, pas du B naturel
            val oct = if (letter == 'C' && acc == -1) (midi + 1) / 12 - 1 else octave
            Note(letter, acc, oct)
        }
    }

    // ---- Géométrie de la portée ----
    // 1 step = demi-interligne. Ligne du haut : F5 (clé de Sol), A3 (clé de Fa).
    // step positif = vers le bas de la portée.

    /** Index diatonique de la ligne du haut (lettre + 7 × octave). */
    fun topLineDiatonic(clef: Clef): Int =
        if (clef == Clef.TREBLE) 3 + 5 * 7 else 5 + 3 * 7

    fun stepToMidi(step: Int, clef: Clef): Int {
        val d = topLineDiatonic(clef) - step
        val letterIdx = Math.floorMod(d, 7)
        val octave = Math.floorDiv(d, 7)
        return (octave + 1) * 12 + PC[letterIdx]
    }

    /** Étendue jouable : 4 notes au-delà de chaque ligne extrême (lignes supplémentaires). */
    const val RANGE_MIN_STEP = -4
    const val RANGE_MAX_STEP = 12

    fun randomNote(clef: Clef, withAccidentals: Boolean, rng: Random): Note {
        val step = (RANGE_MIN_STEP..RANGE_MAX_STEP).random(rng)
        val accidental = when {
            !withAccidentals -> 0
            rng.nextInt(3) == 0 -> 1
            rng.nextInt(3) == 0 -> -1
            else -> 0
        }
        val midi = stepToMidi(step, clef) + accidental
        return fromMidi(midi, preferSharps = accidental >= 0)
    }
}

enum class Difficulty(@StringRes val labelRes: Int) {
    EASY(R.string.diff_easy),
    MEDIUM(R.string.diff_medium),
    HARD(R.string.diff_hard),
    EXPERT(R.string.diff_expert);

    val rounds: Int get() = if (this == EASY || this == MEDIUM) 10 else 12
    val withAccidentals: Boolean get() = this != EASY
    val sequenceLength: Int get() = when (this) {
        EASY -> 3; MEDIUM -> 4; HARD -> 5; EXPERT -> 6
    }

    companion object {
        fun fromName(name: String?): Difficulty =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: EASY
    }
}

enum class Clef(@StringRes val labelRes: Int) {
    TREBLE(R.string.clef_sol),
    BASS(R.string.clef_fa);
}

enum class GameMode(@StringRes val titleRes: Int, @StringRes val descRes: Int) {
    NAME_THE_NOTE(R.string.mode_name, R.string.mode_name_desc),
    PLACE_THE_NOTE(R.string.mode_place, R.string.mode_place_desc),
    EAR_TRAINING(R.string.mode_ear, R.string.mode_ear_desc);
}
