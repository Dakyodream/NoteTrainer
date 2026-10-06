package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.R
import com.dakyodream.notetrainer.core.AudioPlayer
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Note
import com.dakyodream.notetrainer.core.Notation
import com.dakyodream.notetrainer.core.Notes
import com.dakyodream.notetrainer.ui.StaffNote
import com.dakyodream.notetrainer.ui.StaffView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteGuideScreen(
    notation: Notation,
    onBack: () -> Unit
) {
    var selectedClef by remember { mutableStateOf(Clef.TREBLE) }
    val context = LocalContext.current
    val audio = remember { AudioPlayer(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    val allNotes = remember(selectedClef) {
        (Notes.RANGE_MIN_STEP..Notes.RANGE_MAX_STEP).map { step ->
            val midi = Notes.stepToMidi(step, selectedClef)
            Notes.fromMidi(midi, preferSharps = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.guide_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            stringResource(R.string.guide_subtitle),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedClef == Clef.TREBLE,
                onClick = { selectedClef = Clef.TREBLE },
                label = { Text(stringResource(R.string.clef_sol)) }
            )
            FilterChip(
                selected = selectedClef == Clef.BASS,
                onClick = { selectedClef = Clef.BASS },
                label = { Text(stringResource(R.string.clef_fa)) }
            )
        }

        Text(
            stringResource(R.string.guide_c4_note),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allNotes) { note ->
                NoteCard(
                    clef = selectedClef,
                    note = note,
                    notation = notation,
                    onPlay = { audio.playNote(note, 0.8) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
private fun NoteCard(
    clef: Clef,
    note: Note,
    notation: Notation,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onPlay() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StaffView(
                clef = clef,
                notes = listOf(StaffNote(note)),
                heightDp = 180.dp
            )
            Text(
                text = Notes.noteName(note, notation) + " (${note.octave})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "MIDI ${note.midi}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
