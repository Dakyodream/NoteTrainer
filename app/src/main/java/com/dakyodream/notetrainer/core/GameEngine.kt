package com.dakyodream.notetrainer.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/** Question courante et état de la partie. */
data class GameState(
    val mode: GameMode = GameMode.NAME_THE_NOTE,
    val difficulty: Difficulty = Difficulty.EASY,
    val clef: Clef = Clef.TREBLE,
    val round: Int = 0,
    val score: Int = 0,
    val lives: Int = 3,
    val targetNote: Note? = null,
    val targetName: String = "",
    val isCorrect: Boolean? = null,
    val sequence: List<Note> = emptyList(),
    val sequenceIndex: Int = 0,
    val sequenceLength: Int = 3,
    val hasPlayedTarget: Boolean = false,
    val isGameOver: Boolean = false,
    val highScore: Int = 0,
    val isSimonPhase: Boolean = false,
    val feedback: String = ""
)

class GameEngine : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private val rng = Random.Default

    fun startGame(mode: GameMode, difficulty: Difficulty, clef: Clef) {
        val seqLen = when (difficulty) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 5
            Difficulty.EXPERT -> 6
        }
        _state.value = GameState(
            mode = mode,
            difficulty = difficulty,
            clef = clef,
            sequenceLength = seqLen,
            highScore = _state.value.highScore
        )
        nextQuestion()
    }

    fun nextQuestion() {
        val s = _state.value
        if (s.isGameOver) return
        val mode = s.mode
        val withAcc = s.difficulty.withAccidentals
        val seq = if (mode == GameMode.EAR_TRAINING || s.round == 0) {
            List(s.sequenceLength) { Notes.randomInClef(s.clef, s.difficulty, withAcc, rng) }
        } else {
            s.sequence
        }
        val note = if (mode == GameMode.EAR_TRAINING) {
            seq.getOrNull(s.sequenceIndex.coerceIn(0, seq.size - 1)) ?: seq.first()
        } else {
            Notes.randomInClef(s.clef, s.difficulty, withAcc, rng)
        }
        _state.value = s.copy(
            round = s.round + 1,
            sequence = seq,
            sequenceIndex = if (mode == GameMode.EAR_TRAINING) s.sequenceIndex else 0,
            targetNote = note,
            targetName = note.displayName,
            isCorrect = null,
            hasPlayedTarget = false,
            isSimonPhase = mode == GameMode.EAR_TRAINING,
            feedback = ""
        )
    }

    fun checkAnswerName(selectedLetter: Char) {
        val s = _state.value
        val target = s.targetNote ?: return
        val correct = selectedLetter == target.letter
        applyResult(correct)
    }

    fun checkAnswerPlace(selectedNote: Note) {
        val s = _state.value
        val target = s.targetNote ?: return
        val correct = selectedNote.letter == target.letter &&
                selectedNote.octave == target.octave &&
                (s.difficulty == Difficulty.EASY || selectedNote.accidental == target.accidental)
        applyResult(correct)
    }

    private fun applyResult(correct: Boolean) {
        val s = _state.value
        val newLives = if (correct) s.lives else s.lives - 1
        val newScore = if (correct) s.score + 1 else s.score
        val newIdx = if (s.mode == GameMode.EAR_TRAINING && correct) s.sequenceIndex + 1 else s.sequenceIndex
        val seqDone = s.mode == GameMode.EAR_TRAINING && newIdx >= s.sequence.size
        val roundsDone = s.mode != GameMode.EAR_TRAINING && s.round >= s.difficulty.rounds
        val gameOver = newLives <= 0 || seqDone
        val highScore = if (newScore > s.highScore) newScore else s.highScore
        _state.value = s.copy(
            score = newScore,
            lives = newLives,
            isCorrect = correct,
            sequenceIndex = newIdx,
            isGameOver = gameOver,
            highScore = highScore,
            feedback = if (correct) "✅ Bravo !" else "❌ Dommage, c'était ${s.targetNote?.id}"
        )
        if (roundsDone && !gameOver) {
            _state.value = _state.value.copy(isGameOver = true)
        }
    }

    fun advance() {
        val s = _state.value
        if (s.isGameOver) return
        if (s.mode == GameMode.EAR_TRAINING && s.sequenceIndex < s.sequence.size) {
            val note = s.sequence[s.sequenceIndex]
            _state.value = s.copy(
                targetNote = note,
                targetName = note.displayName,
                isCorrect = null,
                hasPlayedTarget = false,
                round = s.round + 1,
                feedback = ""
            )
        } else {
            nextQuestion()
        }
    }

    fun markPlayed() {
        _state.value = _state.value.copy(hasPlayedTarget = true)
    }

    fun endGame() {
        val s = _state.value
        _state.value = s.copy(isGameOver = true)
    }
}
