package com.dakyodream.notetrainer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MusicTest {

    // ---- Notes.midiNumber / frequency ----

    @Test
    fun midi_A4_is_69() {
        assertEquals(69, Notes.midiNumber(Note('A', 0, 4)))
    }

    @Test
    fun midi_C4_is_60() {
        assertEquals(60, Notes.midiNumber(Note('C', 0, 4)))
    }

    @Test
    fun midi_Csharp4_is_61() {
        assertEquals(61, Notes.midiNumber(Note('C', 1, 4)))
    }

    @Test
    fun frequency_A4_is_440() {
        assertEquals(440.0, Notes.frequency(Note('A', 0, 4)), 0.001)
    }

    @Test
    fun frequency_A3_is_220() {
        assertEquals(220.0, Notes.frequency(Note('A', 0, 3)), 0.001)
    }

    // ---- Notes.noteName ----

    @Test
    fun frenchNames_areDoReMi() {
        assertEquals("Do", Notes.noteName('C', Notation.FRENCH))
        assertEquals("Ré", Notes.noteName('D', Notation.FRENCH))
        assertEquals("Mi", Notes.noteName('E', Notation.FRENCH))
        assertEquals("Fa", Notes.noteName('F', Notation.FRENCH))
        assertEquals("Sol", Notes.noteName('G', Notation.FRENCH))
        assertEquals("La", Notes.noteName('A', Notation.FRENCH))
        assertEquals("Si", Notes.noteName('B', Notation.FRENCH))
    }

    @Test
    fun englishNames_areCDE() {
        assertEquals("C", Notes.noteName('C', Notation.ENGLISH))
        assertEquals("G", Notes.noteName('G', Notation.ENGLISH))
        assertEquals("A", Notes.noteName('A', Notation.ENGLISH))
    }

    @Test
    fun accidentalSymbols() {
        assertEquals("♯", Notes.accidentalSymbol(1))
        assertEquals("♭", Notes.accidentalSymbol(-1))
        assertEquals("", Notes.accidentalSymbol(0))
    }

    @Test
    fun noteName_withAccidental() {
        assertEquals("Do♯", Notes.noteName(Note('C', 1, 4), Notation.FRENCH))
        assertEquals("B♭", Notes.noteName(Note('B', -1, 3), Notation.ENGLISH))
    }

    // ---- Notes.fromMidi (aller-retour) ----

    @Test
    fun fromMidi_sharp_roundTrip() {
        for (midi in 30..100) {
            val note = Notes.fromMidi(midi, preferSharps = true)
            assertEquals(midi, Notes.midiNumber(note))
        }
    }

    @Test
    fun fromMidi_flat_roundTrip() {
        for (midi in 30..100) {
            val note = Notes.fromMidi(midi, preferSharps = false)
            assertEquals(midi, Notes.midiNumber(note))
        }
    }

    // ---- Géométrie de la portée ----

    @Test
    fun topLine_treble_is_F5() {
        // F5 : midi 77
        assertEquals(77, Notes.stepToMidi(0, Clef.TREBLE))
    }

    @Test
    fun topLine_bass_is_A3() {
        // A3 : midi 57
        assertEquals(57, Notes.stepToMidi(0, Clef.BASS))
    }

    @Test
    fun step_to_midi_matches_diatonic_descent() {
        // step croissant = vers le bas : chaque +2 steps = 1 lettre en descendant
        val f5 = Notes.stepToMidi(0, Clef.TREBLE)
        val e5 = Notes.stepToMidi(1, Clef.TREBLE)
        val d5 = Notes.stepToMidi(2, Clef.TREBLE)
        assertEquals(Notes.midiNumber(Note('E', 0, 5)), e5)
        assertEquals(Notes.midiNumber(Note('D', 0, 5)), d5)
        assertTrue(f5 > e5)
    }

    // ---- Notes.randomNote ----

    @Test
    fun randomNote_staysWithinStaffRange() {
        val rng = Random(42)
        repeat(500) {
            val note = Notes.randomNote(Clef.TREBLE, withAccidentals = false, rng)
            // sans altération, la note doit être diatonique dans la portée
            assertTrue("note hors bornes: $note", note.accidental == 0)
        }
    }

    @Test
    fun randomNote_withAccidentals_canProduceAccidentals() {
        val rng = Random(7)
        val withAcc = (1..500).map { Notes.randomNote(Clef.TREBLE, true, rng) }
            .any { it.accidental != 0 }
        assertTrue(withAcc)
    }

    // ---- Difficulty ----

    @Test
    fun difficulty_rounds() {
        assertEquals(10, Difficulty.EASY.rounds)
        assertEquals(10, Difficulty.MEDIUM.rounds)
        assertEquals(12, Difficulty.HARD.rounds)
        assertEquals(12, Difficulty.EXPERT.rounds)
    }

    @Test
    fun difficulty_accidentals_onlyForEasyOff() {
        assertTrue(Difficulty.EASY.withAccidentals.not())
        assertTrue(Difficulty.MEDIUM.withAccidentals)
        assertTrue(Difficulty.HARD.withAccidentals)
        assertTrue(Difficulty.EXPERT.withAccidentals)
    }

    @Test
    fun difficulty_fromName_caseInsensitive() {
        assertEquals(Difficulty.HARD, Difficulty.fromName("hard"))
        assertEquals(Difficulty.EXPERT, Difficulty.fromName("EXPERT"))
    }

    @Test
    fun difficulty_fromName_invalid_fallsBackToEasy() {
        assertEquals(Difficulty.EASY, Difficulty.fromName("nonsense"))
        assertEquals(Difficulty.EASY, Difficulty.fromName(null))
    }
}
