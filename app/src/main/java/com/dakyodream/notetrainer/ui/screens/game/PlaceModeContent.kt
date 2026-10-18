package com.dakyodream.notetrainer.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    var draftStep by remember(state.targetNote, state.round) { mutableStateOf<Int?>(null) }

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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Consigne & Altérations
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            // Boutons d'altérations OutlinedButton originaux avec fort contraste
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                listOf(-1 to "♭", 0 to "♮", 1 to "♯").forEach { (acc, sym) ->
                    val isSel = selectedAccidental == acc
                    OutlinedButton(
                        onClick = { onSelectAccidental(acc) },
                        enabled = state.isCorrect == null,
                        colors = if (isSel)
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        else
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                    ) {
                        Text(
                            text = sym,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 2. La partition interactive avec validation immédiate au lâcher/drop
        StaffView(
            clef = state.clef,
            notes = displayNotes,
            inkColor = ink,
            heightDp = 260.dp,
            draftStep = if (state.isCorrect == null) draftStep else null,
            draftAccidental = selectedAccidental,
            showGuideLedgerLines = true,
            onStaffTap = if (state.isCorrect == null) { hit ->
                draftStep = hit.step
                onStaffTap(hit)
            } else null
        )
    }
}
