package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.R
import com.dakyodream.notetrainer.core.AudioPlayer
import com.dakyodream.notetrainer.core.GameEngine
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.core.Note
import com.dakyodream.notetrainer.core.Notes
import com.dakyodream.notetrainer.core.Notation
import com.dakyodream.notetrainer.ui.StaffNote
import com.dakyodream.notetrainer.ui.StaffHitResult
import com.dakyodream.notetrainer.ui.StaffView
import com.dakyodream.notetrainer.ui.staffInkColor
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    engine: GameEngine,
    mode: GameMode,
    onExit: () -> Unit
) {
    val state by engine.state.collectAsState()
    val context = LocalContext.current
    val audio = remember { AudioPlayer(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    var selectedAccidental by remember { mutableStateOf(0) }
    var showGameOver by remember { mutableStateOf(false) }

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

    val ink = staffInkColor()

    fun onStaffTap(hit: StaffHitResult) {
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
        StatusRow(
            lives = state.lives,
            round = state.round,
            totalRounds = if (mode == GameMode.EAR_TRAINING) state.sequence.size else state.difficulty.rounds,
            score = state.score,
            onExit = onExit
        )

        when (mode) {
            GameMode.NAME_THE_NOTE -> NameModeContent(engine, state, ink)
            GameMode.PLACE_THE_NOTE -> PlaceModeContent(engine, state, ink, selectedAccidental, { selectedAccidental = it }, ::onStaffTap)
            GameMode.EAR_TRAINING -> EarModeContent(engine, state, audio, ink, ::onStaffTap)
        }

        state.feedbackRes?.let { res ->
            Text(
                text = if (state.feedbackExtra.isNotEmpty())
                    stringResource(res, state.feedbackExtra)
                else stringResource(res),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (state.isCorrect == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = { engine.advance() },
            enabled = state.isCorrect != null && !state.isGameOver,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.next_note))
        }
    }

    if (showGameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.game_over)) },
            text = {
                Column {
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
private fun StatusRow(lives: Int, round: Int, totalRounds: Int, score: Int, onExit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.lives, lives), fontSize = 18.sp)
        Text(stringResource(R.string.round, round, totalRounds), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.score, score), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onExit) { Text(stringResource(R.string.quit)) }
    }
}

@Composable
private fun NameModeContent(
    engine: GameEngine,
    state: com.dakyodream.notetrainer.core.GameState,
    ink: Color
) {
    val target = state.targetNote ?: return
    StaffView(
        clef = state.clef,
        notes = listOf(StaffNote(target, color = ink)),
        inkColor = ink
    )
    Text(
        stringResource(R.string.mode_name_prompt),
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    // Les 7 noms de notes, disposés en grille
    Notes.LETTERS.chunked(4).forEach { rowLetters ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowLetters.forEach { letter ->
                Button(
                    onClick = { engine.checkAnswerName(letter) },
                    enabled = state.isCorrect == null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        Notes.noteName(letter, state.notation) +
                                if (target.accidental != 0) Notes.accidentalSymbol(target.accidental) else "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (rowLetters.size < 4) Spacer(Modifier.weight(4 - rowLetters.size.toFloat()))
        }
    }
}

@Composable
private fun PlaceModeContent(
    engine: GameEngine,
    state: com.dakyodream.notetrainer.core.GameState,
    ink: Color,
    selectedAccidental: Int,
    onSelectAccidental: (Int) -> Unit,
    onStaffTap: (StaffHitResult) -> Unit
) {
    val target = state.targetNote ?: return
    Text(
        stringResource(
            R.string.mode_place_prompt,
            Notes.noteName(target, state.notation) + " (${target.octave})"
        ),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        listOf(-1 to "♭", 0 to "♮", 1 to "♯").forEach { (acc, sym) ->
            OutlinedButton(
                onClick = { onSelectAccidental(acc) },
                colors = if (selectedAccidental == acc)
                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                else ButtonDefaults.outlinedButtonColors()
            ) { Text(sym, fontSize = 20.sp) }
        }
    }
    StaffView(
        clef = state.clef,
        notes = emptyList(),
        inkColor = ink,
        onStaffTap = onStaffTap
    )
}

@Composable
private fun EarModeContent(
    engine: GameEngine,
    state: com.dakyodream.notetrainer.core.GameState,
    audio: AudioPlayer,
    ink: Color,
    onStaffTap: (StaffHitResult) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.mode_ear_prompt), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Button(
                onClick = { state.targetNote?.let { audio.playNote(it, 1.0) } },
                enabled = state.targetNote != null
            ) {
                Text(stringResource(R.string.mode_ear_listen), fontSize = 18.sp)
            }
            Text(
                stringResource(R.string.sequence_progress, state.sequenceIndex + 1, state.sequence.size),
                fontSize = 14.sp, color = MaterialTheme.colorScheme.primary
            )
        }
    }
    StaffView(
        clef = state.clef,
        notes = emptyList(),
        inkColor = ink,
        onStaffTap = onStaffTap
    )
}
