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
        StatusRow(state = state, onExit = onExit)

        when (mode) {
            GameMode.NAME_THE_NOTE -> NameModeContent(engine, state, ink)
            GameMode.PLACE_THE_NOTE -> PlaceModeContent(engine, state, ink, selectedAccidental, { selectedAccidental = it }, ::onStaffTap)
            GameMode.EAR_TRAINING -> EarModeContent(engine, state, audio, ink, ::onStaffTap)
        }

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

@Composable
private fun NameModeContent(
    engine: GameEngine,
    state: GameState,
    ink: Color
) {
    val target = state.targetNote ?: return
    val noteColor = when (state.isCorrect) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> ink
    }
    StaffView(
        clef = state.clef,
        notes = listOf(StaffNote(target, color = noteColor)),
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
    state: GameState,
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
                enabled = state.isCorrect == null,
                colors = if (selectedAccidental == acc)
                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                else ButtonDefaults.outlinedButtonColors()
            ) { Text(sym, fontSize = 20.sp) }
        }
    }

    val displayNotes = remember(state.isCorrect, state.targetNote, state.userPlacedNote) {
        when (state.isCorrect) {
            null -> emptyList()
            true -> listOf(StaffNote(target, color = Color(0xFF2E7D32)))
            false -> {
                val list = mutableListOf<StaffNote>()
                state.userPlacedNote?.let { list.add(StaffNote(it, color = Color(0xFFC62828))) }
                list.add(StaffNote(target, color = Color(0xFF2E7D32)))
                list
            }
        }
    }

    StaffView(
        clef = state.clef,
        notes = displayNotes,
        inkColor = ink,
        onStaffTap = if (state.isCorrect == null) onStaffTap else null
    )
}

@Composable
private fun EarModeContent(
    engine: GameEngine,
    state: GameState,
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

    val displayNotes = remember(state.isCorrect, state.targetNote, state.userPlacedNote) {
        val target = state.targetNote ?: return@remember emptyList()
        when (state.isCorrect) {
            null -> emptyList()
            true -> listOf(StaffNote(target, color = Color(0xFF2E7D32)))
            false -> {
                val list = mutableListOf<StaffNote>()
                state.userPlacedNote?.let { list.add(StaffNote(it, color = Color(0xFFC62828))) }
                list.add(StaffNote(target, color = Color(0xFF2E7D32)))
                list
            }
        }
    }

    StaffView(
        clef = state.clef,
        notes = displayNotes,
        inkColor = ink,
        onStaffTap = if (state.isCorrect == null) onStaffTap else null
    )
}
