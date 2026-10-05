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
import com.dakyodream.notetrainer.ui.StaffNote
import com.dakyodream.notetrainer.ui.StaffView

@Composable
internal fun NameModeContent(
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
