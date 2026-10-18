package com.dakyodream.notetrainer.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.R
import com.dakyodream.notetrainer.core.AudioPlayer
import com.dakyodream.notetrainer.core.GameEngine
import com.dakyodream.notetrainer.core.GameState
import com.dakyodream.notetrainer.ui.StaffHitResult
import com.dakyodream.notetrainer.ui.StaffNote
import com.dakyodream.notetrainer.ui.StaffView

@Composable
internal fun EarModeContent(
    engine: GameEngine,
    state: GameState,
    audio: AudioPlayer,
    ink: Color,
    onStaffTap: (StaffHitResult) -> Unit
) {
    var draftStep by remember(state.targetNote, state.round) { mutableStateOf<Int?>(null) }

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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Consigne & Bouton réécouter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
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

        // 2. La partition interactive avec validation immédiate au lâcher/drop
        StaffView(
            clef = state.clef,
            notes = displayNotes,
            inkColor = ink,
            heightDp = 260.dp,
            draftStep = if (state.isCorrect == null) draftStep else null,
            showGuideLedgerLines = true,
            onStaffTap = if (state.isCorrect == null) { hit ->
                draftStep = hit.step
                onStaffTap(hit)
            } else null
        )
    }
}
