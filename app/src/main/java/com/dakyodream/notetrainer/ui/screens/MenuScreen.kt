package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Difficulty
import com.dakyodream.notetrainer.core.GameMode

@Composable
fun MenuScreen(
    onStartGame: (GameMode, Difficulty, Clef) -> Unit,
    onShowCredits: () -> Unit,
    onShowInfo: () -> Unit,
    onShowLicense: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(GameMode.NAME_THE_NOTE) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }
    var selectedClef by remember { mutableStateOf(Clef.TREBLE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🎵 Note Trainer", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Apprenez à lire les notes en jouant", fontSize = 14.sp)

        SectionTitle("Mode de jeu")
        GameMode.entries.forEach { mode ->
            ModeCard(
                title = mode.label,
                description = mode.description,
                selected = selectedMode == mode,
                onClick = { selectedMode = mode }
            )
        }

        SectionTitle("Difficulté")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Difficulty.entries.forEach { diff ->
                FilterChip(
                    selected = selectedDifficulty == diff,
                    onClick = { selectedDifficulty = diff },
                    label = { Text(diff.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            when (selectedDifficulty) {
                Difficulty.EASY -> "7 réponses possibles, pas de dièses/bémols, 10 manches"
                Difficulty.MEDIUM -> "3 propositions, dièses/bémols possibles, 10 manches"
                Difficulty.HARD -> "3 propositions, dièses/bémols, 12 manches"
                Difficulty.EXPERT -> "3 propositions, dièses/bémols, 12 manches rapides"
            },
            fontSize = 12.sp
        )

        SectionTitle("Clé")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedClef == Clef.TREBLE,
                onClick = { selectedClef = Clef.TREBLE },
                label = { Text("Clé de Sol") }
            )
            FilterChip(
                selected = selectedClef == Clef.BASS,
                onClick = { selectedClef = Clef.BASS },
                label = { Text("Clé de Fa") }
            )
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onStartGame(selectedMode, selectedDifficulty, selectedClef) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("▶ Jouer", fontSize = 20.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onShowInfo) { Text("ℹ️ Aide") }
            TextButton(onClick = onShowCredits) { Text("👤 Crédits") }
            TextButton(onClick = onShowLicense) { Text("⚖️ Licence") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ModeCard(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(description, fontSize = 12.sp)
        }
    }
}
