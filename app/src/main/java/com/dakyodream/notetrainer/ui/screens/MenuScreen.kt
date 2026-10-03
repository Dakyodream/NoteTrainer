package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.R
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Difficulty
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.core.Notation
import com.dakyodream.notetrainer.ui.ThemeMode

@Composable
fun MenuScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    notation: Notation,
    onNotationChange: (Notation) -> Unit,
    onStartGame: (GameMode, Difficulty, Clef, Notation) -> Unit,
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
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🎵 NoteTrainer", fontSize = 32.sp, fontWeight = FontWeight.Bold)

        SectionTitle(stringResource(R.string.clef_title))
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

        SectionTitle(stringResource(R.string.mode_title_games))
        GameMode.entries.forEach { mode ->
            ModeCard(
                title = stringResource(mode.titleRes),
                description = stringResource(mode.descRes),
                selected = selectedMode == mode,
                onClick = { selectedMode = mode }
            )
        }

        SectionTitle(stringResource(R.string.diff_title))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Difficulty.entries.forEach { diff ->
                FilterChip(
                    selected = selectedDifficulty == diff,
                    onClick = { selectedDifficulty = diff },
                    label = {
                        Text(
                            stringResource(diff.labelRes),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            stringResource(
                when (selectedDifficulty) {
                    Difficulty.EASY -> R.string.diff_easy_desc
                    Difficulty.MEDIUM -> R.string.diff_medium_desc
                    Difficulty.HARD -> R.string.diff_hard_desc
                    Difficulty.EXPERT -> R.string.diff_expert_desc
                }
            ),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        SectionTitle(stringResource(R.string.notation_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Notation.entries.forEach { n ->
                FilterChip(
                    selected = notation == n,
                    onClick = { onNotationChange(n) },
                    label = { Text(stringResource(n.labelRes)) }
                )
            }
        }

        SectionTitle(stringResource(R.string.theme_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { t ->
                FilterChip(
                    selected = themeMode == t,
                    onClick = { onThemeChange(t) },
                    label = {
                        Text(
                            stringResource(
                                when (t) {
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                }
                            )
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onStartGame(selectedMode, selectedDifficulty, selectedClef, notation) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.play), fontSize = 20.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onShowInfo) { Text(stringResource(R.string.help)) }
            TextButton(onClick = onShowCredits) { Text(stringResource(R.string.credits)) }
            TextButton(onClick = onShowLicense) { Text(stringResource(R.string.license)) }
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
