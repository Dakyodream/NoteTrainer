package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakyodream.notetrainer.R
import com.dakyodream.notetrainer.core.Difficulty
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.core.GameRecord
import com.dakyodream.notetrainer.core.Notation
import com.dakyodream.notetrainer.core.Notes
import com.dakyodream.notetrainer.core.StatsStore

private enum class StatsPeriod(@JvmField val days: Int) {
    WEEK(7), MONTH(30), ALL(Int.MAX_VALUE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(notation: Notation, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { StatsStore(context) }
    var records by remember { mutableStateOf(store.load()) }
    var period by remember { mutableStateOf(StatsPeriod.WEEK) }

    val cutoff = remember(period) {
        if (period == StatsPeriod.ALL) 0L
        else System.currentTimeMillis() - period.days * StatsStore.DAY_MS
    }
    val filtered = remember(records, cutoff) {
        records.filter { it.timestamp >= cutoff }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.stats_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Sélecteur de période
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsPeriod.entries.forEach { p ->
                FilterChip(
                    selected = period == p,
                    onClick = { period = p },
                    label = {
                        Text(
                            stringResource(
                                when (p) {
                                    StatsPeriod.WEEK -> R.string.stats_week
                                    StatsPeriod.MONTH -> R.string.stats_month
                                    StatsPeriod.ALL -> R.string.stats_all
                                }
                            )
                        )
                    }
                )
            }
        }

        if (filtered.isEmpty()) {
            Text(stringResource(R.string.stats_none), fontSize = 14.sp, lineHeight = 20.sp)
        } else {
            SummaryCard(filtered)

            HorizontalDivider()
            Text(stringResource(R.string.stats_per_mode), fontWeight = FontWeight.SemiBold)
            GameMode.entries.forEach { mode ->
                val modeRecords = filtered.filter { it.mode == mode }
                if (modeRecords.isNotEmpty()) {
                    ModeCard(
                        title = stringResource(mode.titleRes, Notes.noteName('A', notation)),
                        records = modeRecords
                    )
                }
            }
        }

        Text(
            stringResource(R.string.stats_offline),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
private fun SummaryCard(records: List<GameRecord>) {
    val totalCorrect = records.sumOf { it.correctAnswers }
    val totalWrong = records.sumOf { it.wrongAnswers }
    val totalAns = totalCorrect + totalWrong
    val accuracy = if (totalAns > 0) (totalCorrect.toDouble() / totalAns * 100) else 0.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.stats_games, records.size), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.stats_avg, records.map { it.score }.average().toFloat()), fontSize = 14.sp)
            Text(stringResource(R.string.stats_best, records.maxOf { it.score }), fontSize = 14.sp)
            if (totalAns > 0) {
                Text(stringResource(R.string.stats_accuracy, accuracy, totalCorrect, totalAns), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                stringResource(R.string.stats_rounds, records.sumOf { it.roundsPlayed }),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ModeCard(title: String, records: List<GameRecord>) {
    val totalCorrect = records.sumOf { it.correctAnswers }
    val totalWrong = records.sumOf { it.wrongAnswers }
    val totalAns = totalCorrect + totalWrong
    val accuracy = if (totalAns > 0) (totalCorrect.toDouble() / totalAns * 100) else 0.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.stats_games, records.size), fontSize = 12.sp)
            }

            if (totalAns > 0) {
                Text(
                    stringResource(R.string.stats_accuracy, accuracy, totalCorrect, totalAns),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(stringResource(R.string.stats_diff_breakdown), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Difficulty.entries.forEach { diff ->
                val diffRecords = records.filter { it.difficulty == diff }
                if (diffRecords.isNotEmpty()) {
                    val maxScore = diffRecords.maxOf { it.score }
                    val diffName = stringResource(diff.labelRes)
                    Text(
                        stringResource(R.string.stats_diff_score, diffName, maxScore),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
