package com.dakyodream.notetrainer.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests de la logique de jeu. Le timer (coroutines viewModelScope) n'est pas
 * testé ici : on teste les transitions d'état pures via les méthodes publiques.
 * Pour les modes chrono, on vérifie l'état après applyResult sans attendre le timer.
 */
class GameEngineTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun engineWith(mode: GameMode, diff: Difficulty = Difficulty.EASY): GameEngine {
        val e = GameEngine()
        e.startGame(mode, diff, Clef.TREBLE, Notation.FRENCH)
        return e
    }

    // ---- startGame / nextQuestion ----

    @Test
    fun startGame_resetsState() {
        val e = engineWith(GameMode.NAME_THE_NOTE)
        val s = e.state.value
        assertEquals(GameMode.NAME_THE_NOTE, s.mode)
        assertEquals(3, s.lives)
        assertFalse(s.isGameOver)
        assertNotNull(s.targetNote)
        // après startGame, nextQuestion a déjà été appelé -> round = 1
        assertEquals(1, s.round)
    }

    @Test
    fun nextQuestion_incrementsRound() {
        val e = engineWith(GameMode.NAME_THE_NOTE)
        val before = e.state.value.round
        e.nextQuestion()
        assertEquals(before + 1, e.state.value.round)
    }

    // ---- checkAnswerName ----

    @Test
    fun correctAnswer_incrementsScore() {
        val e = engineWith(GameMode.NAME_THE_NOTE)
        val target = e.state.value.targetNote!!
        val before = e.state.value.score
        e.checkAnswerName(target.letter)
        val s = e.state.value
        assertEquals(before + 1, s.score)
        assertTrue(s.isCorrect!!)
        assertEquals(3, s.lives)
    }

    @Test
    fun wrongAnswer_decrementsLives_andGivesFeedback() {
        val e = engineWith(GameMode.NAME_THE_NOTE)
        val target = e.state.value.targetNote!!
        val wrongLetter = Notes.LETTERS.first { it != target.letter }
        e.checkAnswerName(wrongLetter)
        val s = e.state.value
        assertEquals(2, s.lives)
        assertFalse(s.isCorrect!!)
        assertEquals(1, s.wrongAnswers)
        // feedbackExtra contient le nom de la note cible
        assertTrue(s.feedbackExtra.isNotEmpty())
    }

    // ---- checkAnswerPlace ----

    @Test
    fun place_correctNote_isCorrect() {
        val e = engineWith(GameMode.PLACE_THE_NOTE)
        val target = e.state.value.targetNote!!
        e.checkAnswerPlace(target)
        assertTrue(e.state.value.isCorrect!!)
    }

    @Test
    fun place_wrongOctave_isWrong() {
        val e = engineWith(GameMode.PLACE_THE_NOTE)
        val target = e.state.value.targetNote!!
        e.checkAnswerPlace(target.copy(octave = target.octave + 1))
        assertFalse(e.state.value.isCorrect!!)
    }

    @Test
    fun place_easyMode_ignoresAccidental() {
        val e = engineWith(GameMode.PLACE_THE_NOTE, Difficulty.EASY)
        val target = e.state.value.targetNote!!
        // EASY : l'altération n'est pas vérifiée
        e.checkAnswerPlace(target.copy(accidental = if (target.accidental == 0) 1 else 0))
        assertTrue(e.state.value.isCorrect!!)
    }

    // ---- game over ----

    @Test
    fun threeWrongAnswers_endsGame() {
        val e = engineWith(GameMode.NAME_THE_NOTE)
        repeat(3) {
            val target = e.state.value.targetNote!!
            val wrongLetter = Notes.LETTERS.first { it != target.letter }
            e.checkAnswerName(wrongLetter)
        }
        assertTrue(e.state.value.isGameOver)
        assertEquals(0, e.state.value.lives)
    }

    @Test
    fun onGameOver_firedOnce_withStats() {
        val e = engineWith(GameMode.NAME_THE_NOTE)
        var calls = 0
        var lastStats: IntArray? = null
        e.onGameOver = { _, _, score, rounds, correct, wrong ->
            calls++
            lastStats = intArrayOf(score, rounds, correct, wrong)
        }
        // 1 bonne réponse puis 3 mauvaises -> 3 vies = 1 correct + 3 wrong
        val t1 = e.state.value.targetNote!!
        e.checkAnswerName(t1.letter)
        e.advance()
        repeat(3) {
            val target = e.state.value.targetNote!!
            val wrongLetter = Notes.LETTERS.first { it != target.letter }
            e.checkAnswerName(wrongLetter)
        }
        assertEquals(1, calls)
        assertNotNull(lastStats)
        assertArrayEquals(intArrayOf(1, 2, 1, 3), lastStats)
    }

    @Test
    fun roundsLimit_easyNameMode_endsAfter10Rounds() {
        val e = engineWith(GameMode.NAME_THE_NOTE, Difficulty.EASY)
        var over = false
        e.onGameOver = { _, _, _, _, _, _ -> over = true }
        repeat(10) {
            if (!e.state.value.isGameOver) {
                val target = e.state.value.targetNote!!
                e.checkAnswerName(target.letter) // toujours bon
                e.advance()
            }
        }
        assertTrue(e.state.value.isGameOver)
        assertTrue(over)
        // 10 bonnes réponses
        assertEquals(10, e.state.value.score)
    }

    // ---- mode chronométré ----

    @Test
    fun timedMode_activeForMediumPlus() {
        val easy = engineWith(GameMode.NAME_THE_NOTE, Difficulty.EASY).state.value
        val medium = engineWith(GameMode.NAME_THE_NOTE, Difficulty.MEDIUM).state.value
        val expert = engineWith(GameMode.NAME_THE_NOTE, Difficulty.EXPERT).state.value
        assertFalse(easy.isTimedMode)
        assertTrue(medium.isTimedMode)
        assertTrue(expert.isTimedMode)
    }

    @Test
    fun earTraining_isNeverTimed() {
        val s = engineWith(GameMode.EAR_TRAINING, Difficulty.EXPERT).state.value
        assertFalse(s.isTimedMode)
    }

    @Test
    fun correctAnswerInTimedMode_grantsBonusTime() {
        val e = engineWith(GameMode.NAME_THE_NOTE, Difficulty.MEDIUM)
        val before = e.state.value.timeLeftSec
        val target = e.state.value.targetNote!!
        e.checkAnswerName(target.letter)
        val s = e.state.value
        // MEDIUM : bonus initial 10 s, première minute -> bonus complet
        assertNotNull(s.lastBonusSec)
        assertTrue("temps attendu > avant, avant=$before apres=${s.timeLeftSec}", s.timeLeftSec > before)
    }

    @Test
    fun wrongAnswerInTimedMode_noBonus() {
        val e = engineWith(GameMode.NAME_THE_NOTE, Difficulty.MEDIUM)
        val target = e.state.value.targetNote!!
        val wrongLetter = Notes.LETTERS.first { it != target.letter }
        e.checkAnswerName(wrongLetter)
        val s = e.state.value
        assertNull(s.lastBonusSec)
    }

    // ---- advance (mode oreille) ----

    @Test
    fun earTraining_correctAnswer_advancesSequence() {
        val e = engineWith(GameMode.EAR_TRAINING, Difficulty.EASY)
        val s0 = e.state.value
        assertEquals(0, s0.sequenceIndex)
        // bonne réponse sur la première note
        e.checkAnswerName(s0.sequence.first().letter)
        assertEquals(1, e.state.value.sequenceIndex)
        e.advance()
        // advance positionne targetNote sur la 2e note
        assertEquals(e.state.value.sequence[1], e.state.value.targetNote)
    }

    @Test
    fun earTraining_sequenceCompletes_endsGame() {
        val e = engineWith(GameMode.EAR_TRAINING, Difficulty.EASY)
        e.onGameOver = { _, _, _, _, _, _ -> }
        // répondre correctement à toute la séquence (3 notes en EASY)
        while (!e.state.value.isGameOver) {
            val target = e.state.value.targetNote!!
            e.checkAnswerName(target.letter)
            e.advance()
        }
        assertTrue(e.state.value.isGameOver)
        assertEquals(3, e.state.value.correctAnswers)
    }
}
