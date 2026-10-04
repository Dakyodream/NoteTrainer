package com.dakyodream.notetrainer.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakyodream.notetrainer.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val userPlacedNote: Note? = null,
    val isCorrect: Boolean? = null,
    val sequence: List<Note> = emptyList(),
    val sequenceIndex: Int = 0,
    val hasPlayedTarget: Boolean = false,
    val isGameOver: Boolean = false,
    val gameOverReason: Int = R.string.game_over,
    val highScore: Int = 0,
    val feedbackRes: Int? = null,
    val feedbackExtra: String = "",
    val isTimedMode: Boolean = false,
    val timeLeftSec: Double = 60.0,
    val totalTimePlayedSec: Double = 0.0,
    val lastBonusSec: Double? = null,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0
)

class GameEngine : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private val rng = Random.Default
    private var timerJob: Job? = null

    /** Callback appelé une fois par partie terminée (mode, diff, score, rounds, correct, wrong). */
    var onGameOver: ((GameMode, Difficulty, Int, Int, Int, Int) -> Unit)? = null

    fun startGame(mode: GameMode, difficulty: Difficulty, clef: Clef, notation: Notation) {
        val timed = mode != GameMode.EAR_TRAINING && difficulty != Difficulty.EASY
        _state.value = GameState(
            mode = mode,
            difficulty = difficulty,
            clef = clef,
            notation = notation,
            highScore = _state.value.highScore,
            isTimedMode = timed,
            timeLeftSec = 60.0,
            totalTimePlayedSec = 0.0
        )
        startTimerIfNeeded()
        nextQuestion()
    }

    private fun startTimerIfNeeded() {
        timerJob?.cancel()
        val s = _state.value
        if (!s.isTimedMode) return

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(100)
                val curr = _state.value
                if (curr.isGameOver || curr.isCorrect != null) {
                    // Pause le chrono pendant l'affichage du feedback ou si la partie est terminée
                    continue
                }
                val newTime = (curr.timeLeftSec - 0.1).coerceAtLeast(0.0)
                val newTotal = curr.totalTimePlayedSec + 0.1
                if (newTime <= 0.0) {
                    val gameOverState = curr.copy(
                        timeLeftSec = 0.0,
                        totalTimePlayedSec = newTotal,
                        isGameOver = true,
                        gameOverReason = R.string.game_over_time
                    )
                    _state.value = gameOverState
                    onGameOver?.invoke(
                        gameOverState.mode,
                        gameOverState.difficulty,
                        gameOverState.score,
                        gameOverState.round,
                        gameOverState.correctAnswers,
                        gameOverState.wrongAnswers
                    )
                    break
                } else {
                    _state.value = curr.copy(
                        timeLeftSec = newTime,
                        totalTimePlayedSec = newTotal
                    )
                }
            }
        }
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
            userPlacedNote = null,
            isCorrect = null,
            hasPlayedTarget = false,
            feedbackRes = null,
            feedbackExtra = "",
            lastBonusSec = null
        )
    }

    fun checkAnswerName(selectedLetter: Char) {
        val s = _state.value
        val target = s.targetNote ?: return
        applyResult(selectedLetter == target.letter, null)
    }

    fun checkAnswerPlace(selectedNote: Note) {
        val s = _state.value
        val target = s.targetNote ?: return
        val correct = selectedNote.letter == target.letter &&
                selectedNote.octave == target.octave &&
                (s.difficulty == Difficulty.EASY || selectedNote.accidental == target.accidental)
        applyResult(correct, selectedNote)
    }

    private fun calculateBonusTime(diff: Difficulty, totalTimeSec: Double): Double {
        val tInit = when (diff) {
            Difficulty.MEDIUM -> 10.0
            Difficulty.HARD -> 5.0
            Difficulty.EXPERT -> 1.0
            Difficulty.EASY -> 0.0
        }
        if (tInit <= 0.0) return 0.0
        val minuteIndex = (totalTimeSec / 60.0).toInt()
        val step = tInit / 10.0
        return (tInit - minuteIndex * step).coerceAtLeast(step)
    }

    private fun applyResult(correct: Boolean, userPlaced: Note?) {
        val s = _state.value
        val newLives = if (correct) s.lives else s.lives - 1
        val newScore = if (correct) s.score + 1 else s.score
        val newCorrect = if (correct) s.correctAnswers + 1 else s.correctAnswers
        val newWrong = if (!correct) s.wrongAnswers + 1 else s.wrongAnswers

        val bonusSec = if (correct && s.isTimedMode) calculateBonusTime(s.difficulty, s.totalTimePlayedSec) else null
        val newTimeLeft = if (bonusSec != null) s.timeLeftSec + bonusSec else s.timeLeftSec

        val newIdx = if (s.mode == GameMode.EAR_TRAINING && correct) s.sequenceIndex + 1 else s.sequenceIndex
        val seqDone = s.mode == GameMode.EAR_TRAINING && newIdx >= s.sequence.size
        // En mode chrono (Medium/Hard/Expert), la partie se termine quand le chrono ou les vies tombent à 0
        val roundsDone = !s.isTimedMode && s.mode != GameMode.EAR_TRAINING && s.round >= s.difficulty.rounds
        val gameOver = newLives <= 0 || seqDone || roundsDone
        val highScore = maxOf(newScore, s.highScore)
        val roundsPlayed = if (s.mode == GameMode.EAR_TRAINING) newIdx else s.round

        if (gameOver && !s.isGameOver) {
            onGameOver?.invoke(s.mode, s.difficulty, newScore, roundsPlayed, newCorrect, newWrong)
        }

        _state.value = s.copy(
            score = newScore,
            lives = newLives,
            userPlacedNote = userPlaced,
            isCorrect = correct,
            sequenceIndex = newIdx,
            isGameOver = gameOver,
            highScore = highScore,
            timeLeftSec = newTimeLeft,
            lastBonusSec = bonusSec,
            correctAnswers = newCorrect,
            wrongAnswers = newWrong,
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
                userPlacedNote = null,
                isCorrect = null,
                hasPlayedTarget = false,
                round = s.round + 1,
                feedbackRes = null,
                feedbackExtra = "",
                lastBonusSec = null
            )
        } else {
            nextQuestion()
        }
    }

    fun markPlayed() {
        _state.value = _state.value.copy(hasPlayedTarget = true)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
