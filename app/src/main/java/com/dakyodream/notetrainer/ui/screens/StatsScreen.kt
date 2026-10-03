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
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.core.GameRecord
import com.dakyodream.notetrainer.core.Notation
import com.dakyodream.notetrainer.core.Notes
import com.dakyodream.notetrainer.core.StatsStore
import java.util.Calendar

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
                    ModeRow(
                        title = stringResource(mode.titleRes, Notes.noteName('A', notation)),
                        count = modeRecords.size,
                        avg = modeRecords.map { it.score }.average().toFloat(),
                        best = modeRecords.maxOf { it.score }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.stats_games, records.size), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.stats_avg, records.map { it.score }.average().toFloat()), fontSize = 14.sp)
            Text(stringResource(R.string.stats_best, records.maxOf { it.score }), fontSize = 14.sp)
            Text(
                stringResource(R.string.stats_rounds, records.sumOf { it.roundsPlayed }),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ModeRow(title: String, count: Int, avg: Float, best: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(stringResource(R.string.stats_games, count), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.stats_avg, avg), fontSize = 12.sp)
                Text(stringResource(R.string.stats_best, best), fontSize = 12.sp)
            }
        }
    }
}
