package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var highlightedNote by remember { mutableStateOf<Note?>(null) }
    val context = LocalContext.current
    val audio = remember { AudioPlayer(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    val allNotes = remember(selectedClef) {
        (Notes.RANGE_MIN_STEP..Notes.RANGE_MAX_STEP).map { step ->
            val midi = Notes.stepToMidi(step, selectedClef)
            Notes.fromMidi(midi, preferSharps = true)
        }
    }

    val displayStaffNotes = remember(allNotes, highlightedNote) {
        allNotes.map { n ->
            val isHighlighted = highlightedNote != null && n.letter == highlightedNote!!.letter && n.octave == highlightedNote!!.octave && n.accidental == highlightedNote!!.accidental
            StaffNote(n, color = if (isHighlighted) Color(0xFF2E7D32) else Color(0xFF1B1B1B))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    onClick = { selectedClef = Clef.TREBLE; highlightedNote = null },
                    label = { Text(stringResource(R.string.clef_sol)) }
                )
                FilterChip(
                    selected = selectedClef == Clef.BASS,
                    onClick = { selectedClef = Clef.BASS; highlightedNote = null },
                    label = { Text(stringResource(R.string.clef_fa)) }
                )
            }

            Text(
                stringResource(R.string.guide_c4_note),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Partition unique continue avec toutes les notes
        StaffView(
            clef = selectedClef,
            notes = displayStaffNotes,
            heightDp = 250.dp
        )

        // Grille des noms de notes avec leur octave
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allNotes.chunked(3).forEach { rowNotes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowNotes.forEach { note ->
                        val isSel = highlightedNote != null && note.letter == highlightedNote!!.letter && note.octave == highlightedNote!!.octave && note.accidental == highlightedNote!!.accidental
                        Card(
                            onClick = {
                                highlightedNote = note
                                audio.playNote(note, 0.8)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = Notes.noteName(note, notation) + " (${note.octave})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "MIDI ${note.midi}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (rowNotes.size < 3) Spacer(Modifier.weight(3 - rowNotes.size.toFloat()))
                }
            }
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.back))
            }
        }
    }
}
