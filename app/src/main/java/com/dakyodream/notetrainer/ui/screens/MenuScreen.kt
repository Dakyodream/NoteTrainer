package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.dakyodream.notetrainer.core.Notes
import com.dakyodream.notetrainer.ui.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    notation: Notation,
    onNotationChange: (Notation) -> Unit,
    onStartGame: (GameMode, Difficulty, Clef, Notation) -> Unit,
    onShowCredits: () -> Unit,
    onShowInfo: () -> Unit,
    onShowLicense: () -> Unit,
    onShowStats: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(GameMode.NAME_THE_NOTE) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }
    var selectedClef by remember { mutableStateOf(Clef.TREBLE) }
    var drawerOpen by remember { mutableStateOf(false) }

    val aName = Notes.noteName('A', notation)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ----- Header : burger à gauche, icônes thème à droite -----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { drawerOpen = true }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
            // Bouton unique : cycle clair -> sombre -> système -> clair
            val nextMode = when (themeMode) {
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
            }
            val currentIcon: ImageVector = when (themeMode) {
                ThemeMode.LIGHT -> Icons.Filled.LightMode
                ThemeMode.DARK -> Icons.Filled.DarkMode
                ThemeMode.SYSTEM -> Icons.Filled.Settings
            }
            val currentLabel = stringResource(
                when (themeMode) {
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                    ThemeMode.SYSTEM -> R.string.theme_system
                }
            )
            val nextLabel = stringResource(
                when (nextMode) {
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                    ThemeMode.SYSTEM -> R.string.theme_system
                }
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onThemeChange(nextMode) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    currentIcon,
                    contentDescription = "$currentLabel → $nextLabel",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // ----- Titre -----
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
                title = stringResource(mode.titleRes, aName),
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

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onStartGame(selectedMode, selectedDifficulty, selectedClef, notation) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.play), fontSize = 20.sp)
        }
    }

    // ----- Panneau burger (bottom sheet) -----
    if (drawerOpen) {
        ModalBottomSheet(
            onDismissRequest = { drawerOpen = false }
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.notation_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Notation.entries.forEach { n ->
                    FilterChip(
                        selected = notation == n,
                        onClick = { onNotationChange(n); drawerOpen = false },
                        label = { Text(stringResource(n.labelRes)) }
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DrawerItem(Icons.Filled.Info, stringResource(R.string.help)) { onShowInfo(); drawerOpen = false }
                DrawerItem(Icons.Filled.Person, stringResource(R.string.credits)) { onShowCredits(); drawerOpen = false }
                DrawerItem(Icons.Filled.Gavel, stringResource(R.string.license)) { onShowLicense(); drawerOpen = false }
                DrawerItem(Icons.Filled.BarChart, stringResource(R.string.stats_title)) { onShowStats(); drawerOpen = false }
            }
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Text(label)
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
