package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.core.AudioPlayer
import com.dakyodream.notetrainer.core.GameEngine
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.core.Note
import com.dakyodream.notetrainer.core.Notes
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.ui.StaffNote
import com.dakyodream.notetrainer.ui.StaffView
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

    fun noteFromTap(step: Float): Note? {
        val midi = stepToMidi(step, state.clef)
        return Notes.fromMidiSimple(midi)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusRow(state.score, state.lives, state.round, state.difficulty.rounds, onExit)

        when (mode) {
            GameMode.NAME_THE_NOTE -> NameModeContent(
                engine = engine,
                state = state
            )
            GameMode.PLACE_THE_NOTE -> PlaceModeContent(
                engine = engine,
                state = state,
                selectedAccidental = selectedAccidental,
                onSelectAccidental = { selectedAccidental = it },
                onStaffTap = { step ->
                    val n = noteFromTap(step)
                    if (n != null) engine.checkAnswerPlace(n)
                }
            )
            GameMode.EAR_TRAINING -> EarModeContent(
                engine = engine,
                state = state,
                audio = audio,
                onStaffTap = { step ->
                    val n = noteFromTap(step)
                    if (n != null) engine.checkAnswerPlace(n)
                }
            )
        }

        if (state.feedback.isNotEmpty()) {
            Text(
                text = state.feedback,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (state.isCorrect == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Button(
            onClick = { engine.advance() },
            enabled = state.isCorrect != null && !state.isGameOver,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Note suivante")
        }
    }

    if (showGameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Partie terminée !") },
            text = {
                Text("Score : ${state.score}\nMeilleur score : ${state.highScore}\n" +
                        if (state.mode == GameMode.EAR_TRAINING) "Séquence retenue : ${state.sequenceIndex}/${state.sequence.size}" else "")
            },
            confirmButton = {
                TextButton(onClick = {
                    showGameOver = false
                    engine.startGame(state.mode, state.difficulty, state.clef)
                }) { Text("Rejouer") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGameOver = false
                    onExit()
                }) { Text("Menu") }
            }
        )
    }
}

@Composable
private fun StatusRow(score: Int, lives: Int, round: Int, totalRounds: Int, onExit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("❤️ $lives", fontSize = 18.sp)
        Text("Manche $round/$totalRounds", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        Text("⭐ $score", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onExit) { Text("Quitter") }
    }
}

@Composable
private fun NameModeContent(engine: GameEngine, state: com.dakyodream.notetrainer.core.GameState) {
    val target = state.targetNote ?: return
    StaffView(
        clef = state.clef,
        notes = listOf(StaffNote(target, showLabel = false)),
        modifier = Modifier.fillMaxWidth()
    )
    val options = buildList {
        add(target.letter)
        Notes.LETTERS.filter { it != target.letter }.shuffled().take(2).forEach { add(it) }
    }.shuffled()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Quel est le nom de cette note ?", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        options.forEach { letter ->
            Button(
                onClick = { engine.checkAnswerName(letter) },
                enabled = state.isCorrect == null,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("$letter${if (target.accidental == 1) "♯" else if (target.accidental == -1) "♭" else ""}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlaceModeContent(
    engine: GameEngine,
    state: com.dakyodream.notetrainer.core.GameState,
    selectedAccidental: Int,
    onSelectAccidental: (Int) -> Unit,
    onStaffTap: (Float) -> Unit
) {
    val target = state.targetNote ?: return
    Text(
        "Placez la note : ${target.letter}${if (target.accidental == 1) "♯" else if (target.accidental == -1) "♭" else ""} ${target.octave}",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(-1 to "♭", 0 to "♮", 1 to "♯").forEach { (acc, sym) ->
            OutlinedButton(
                onClick = { onSelectAccidental(acc) },
                colors = if (selectedAccidental == acc)
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
            ) { Text(sym, fontSize = 20.sp) }
        }
    }
    StaffView(
        clef = state.clef,
        notes = emptyList(),
        onStepTap = { step -> onStaffTap(step + selectedAccidental * 0.5f) }
    )
}

@Composable
private fun EarModeContent(
    engine: GameEngine,
    state: com.dakyodream.notetrainer.core.GameState,
    audio: AudioPlayer,
    onStaffTap: (Float) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Écoutez puis placez la note", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Button(
                onClick = { state.targetNote?.let { audio.playNote(it, 1.0) } },
                enabled = state.targetNote != null
            ) {
                Text("🔊 Réécouter", fontSize = 18.sp)
            }
            Text(
                "Note ${state.sequenceIndex + 1}/${state.sequence.size} de la séquence",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.primary
            )
        }
    }
    StaffView(
        clef = state.clef,
        notes = emptyList(),
        onStepTap = onStaffTap
    )
}
