package com.dakyodream.notetrainer.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.R
import com.dakyodream.notetrainer.core.AudioPlayer
import com.dakyodream.notetrainer.core.GameEngine
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.core.GameState
import com.dakyodream.notetrainer.core.Note
import com.dakyodream.notetrainer.core.Notes
import com.dakyodream.notetrainer.ui.StaffHitResult
import com.dakyodream.notetrainer.ui.StaffNote
import com.dakyodream.notetrainer.ui.StaffView
import com.dakyodream.notetrainer.ui.staffInkColor
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun GameScreen(
    engine: GameEngine,
    mode: GameMode,
    onExit: () -> Unit
) {
    val state by engine.state.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val audio = remember { AudioPlayer(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    var selectedAccidental by remember { mutableStateOf(0) }
    var showGameOver by remember { mutableStateOf(false) }
    var confirmQuit by remember { mutableStateOf(false) }

    LaunchedEffect(state.targetNote, state.round) {
        if (mode == GameMode.EAR_TRAINING && state.targetNote != null && !state.hasPlayedTarget) {
            delay(300)
            audio.playNote(state.targetNote!!, 1.0)
            engine.markPlayed()
        }
    }

    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver) showGameOver = true
    }

    LaunchedEffect(state.isCorrect) {
        // Retour haptique + sonore à chaque réponse (droit / faux)
        if (state.isCorrect != null && !state.isGameOver) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            audio.playResultSound(state.isCorrect == true)
        }
    }

    val ink = staffInkColor()

    fun onStaffTap(hit: StaffHitResult) {
        if (state.isCorrect != null) return
        val midi = Notes.stepToMidi(hit.step, state.clef) + selectedAccidental
        val note = Notes.fromMidi(midi, preferSharps = selectedAccidental >= 0)
        engine.checkAnswerPlace(note)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusRow(state = state, onExit = { if (!state.isGameOver) confirmQuit = true else onExit() })

        when (mode) {
            GameMode.NAME_THE_NOTE -> NameModeContent(engine, state, ink)
            GameMode.PLACE_THE_NOTE -> PlaceModeContent(engine, state, ink, selectedAccidental, { selectedAccidental = it }, ::onStaffTap)
            GameMode.EAR_TRAINING -> EarModeContent(engine, state, audio, ink, ::onStaffTap)
        }

        AnimatedVisibility(
            visible = state.feedbackRes != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 2 }
        ) {
        state.feedbackRes?.let { res ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.feedbackExtra.isNotEmpty())
                        stringResource(res, state.feedbackExtra)
                    else stringResource(res),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isCorrect == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                    textAlign = TextAlign.Center
                )
                val bonus = state.lastBonusSec
                if (bonus != null && bonus > 0) {
                    Text(
                        text = stringResource(R.string.time_bonus, bonus),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
        }

        Button(
            onClick = { engine.advance() },
            enabled = state.isCorrect != null && !state.isGameOver,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.next_note))
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text(stringResource(R.string.quit_confirm_title)) },
            text = { Text(stringResource(R.string.quit_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirmQuit = false; onExit() }) {
                    Text(stringResource(R.string.quit_yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false }) {
                    Text(stringResource(R.string.quit_no))
                }
            }
        )
    }

    if (showGameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(state.gameOverReason)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.final_score, state.score))
                    Text(stringResource(R.string.best_score, state.highScore))
                    if (state.mode == GameMode.EAR_TRAINING) {
                        Text(stringResource(R.string.sequence_progress, state.sequenceIndex, state.sequence.size))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showGameOver = false
                    engine.startGame(state.mode, state.difficulty, state.clef, state.notation)
                }) { Text(stringResource(R.string.replay)) }
            },
            dismissButton = {
                TextButton(onClick = { showGameOver = false; onExit() }) { Text(stringResource(R.string.menu)) }
            }
        )
    }
}

@Composable
private fun StatusRow(state: GameState, onExit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.lives, state.lives), fontSize = 16.sp)
        if (state.isTimedMode) {
            Text(
                text = String.format(Locale.US, "⏱️ %.1fs", state.timeLeftSec),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.timeLeftSec <= 10.0) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
            )
        } else {
            val totalRounds = if (state.mode == GameMode.EAR_TRAINING) state.sequence.size else state.difficulty.rounds
            Text(
                text = stringResource(R.string.round, state.round, totalRounds),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(stringResource(R.string.score, state.score), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onExit) { Text(stringResource(R.string.quit)) }
    }
}
