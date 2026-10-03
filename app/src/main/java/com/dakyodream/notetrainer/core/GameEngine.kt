package com.dakyodream.notetrainer.core

import androidx.lifecycle.ViewModel
import com.dakyodream.notetrainer.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

data class GameState(
    val mode: GameMode = GameMode.NAME_THE_NOTE,
    val difficulty: Difficulty = Difficulty.EASY,
    val clef: Clef = Clef.TREBLE,
    val notation: Notation = Notation.FRENCH,
    val round: Int = 0,
    val score: Int = 0,
    val lives: Int = 3,
    val targetNote: Note? = null,
    val isCorrect: Boolean? = null,
    val sequence: List<Note> = emptyList(),
    val sequenceIndex: Int = 0,
    val hasPlayedTarget: Boolean = false,
    val isGameOver: Boolean = false,
    val highScore: Int = 0,
    val feedbackRes: Int? = null,
    val feedbackExtra: String = ""
)

class GameEngine : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private val rng = Random.Default

    fun startGame(mode: GameMode, difficulty: Difficulty, clef: Clef, notation: Notation) {
        _state.value = GameState(
            mode = mode,
            difficulty = difficulty,
            clef = clef,
            notation = notation,
            highScore = _state.value.highScore
        )
        nextQuestion()
    }

    fun nextQuestion() {
        val s = _state.value
        if (s.isGameOver) return
        val withAcc = s.difficulty.withAccidentals
        val seq = List(s.difficulty.sequenceLength) { Notes.randomNote(s.clef, withAcc, rng) }
        val note = if (s.mode == GameMode.EAR_TRAINING) seq.first() else Notes.randomNote(s.clef, withAcc, rng)
        _state.value = s.copy(
            round = s.round + 1,
            sequence = seq,
            sequenceIndex = 0,
            targetNote = note,
            isCorrect = null,
            hasPlayedTarget = false,
            feedbackRes = null,
            feedbackExtra = ""
        )
    }

    fun checkAnswerName(selectedLetter: Char) {
        val s = _state.value
        val target = s.targetNote ?: return
        applyResult(selectedLetter == target.letter)
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
        val gameOver = newLives <= 0 || seqDone || roundsDone
        val highScore = maxOf(newScore, s.highScore)
        _state.value = s.copy(
            score = newScore,
            lives = newLives,
            isCorrect = correct,
            sequenceIndex = newIdx,
            isGameOver = gameOver,
            highScore = highScore,
            feedbackRes = if (correct) R.string.feedback_correct else R.string.feedback_wrong,
            feedbackExtra = if (!correct) s.targetNote?.let { Notes.noteName(it, s.notation) } ?: "" else ""
        )
    }

    fun advance() {
        val s = _state.value
        if (s.isGameOver) return
        if (s.mode == GameMode.EAR_TRAINING && s.sequenceIndex < s.sequence.size) {
            val note = s.sequence[s.sequenceIndex]
            _state.value = s.copy(
                targetNote = note,
                isCorrect = null,
                hasPlayedTarget = false,
                round = s.round + 1,
                feedbackRes = null,
                feedbackExtra = ""
            )
        } else {
            nextQuestion()
        }
    }

    fun markPlayed() {
        _state.value = _state.value.copy(hasPlayedTarget = true)
    }
}
