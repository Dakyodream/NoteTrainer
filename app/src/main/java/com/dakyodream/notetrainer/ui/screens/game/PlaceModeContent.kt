package com.dakyodream.notetrainer.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.R
import com.dakyodream.notetrainer.core.GameEngine
import com.dakyodream.notetrainer.core.GameState
import com.dakyodream.notetrainer.core.Notes
import com.dakyodream.notetrainer.ui.StaffHitResult
import com.dakyodream.notetrainer.ui.StaffNote
import com.dakyodream.notetrainer.ui.StaffView

@Composable
internal fun PlaceModeContent(
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
        heightDp = 460.dp,
        onStaffTap = if (state.isCorrect == null) onStaffTap else null
    )
}
