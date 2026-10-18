package com.dakyodream.notetrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dakyodream.notetrainer.core.GameEngine
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.core.GameRecord
import com.dakyodream.notetrainer.core.Notation
import com.dakyodream.notetrainer.core.SettingsStore
import com.dakyodream.notetrainer.core.StatsStore
import com.dakyodream.notetrainer.ui.NoteTrainerTheme
import com.dakyodream.notetrainer.ui.ThemeMode
import com.dakyodream.notetrainer.ui.screens.CreditsScreen
import com.dakyodream.notetrainer.ui.screens.game.GameScreen
import com.dakyodream.notetrainer.ui.screens.InfoScreen
import com.dakyodream.notetrainer.ui.screens.LicenseScreen
import com.dakyodream.notetrainer.ui.screens.NoteGuideScreen
import com.dakyodream.notetrainer.ui.screens.PrivacyScreen
import com.dakyodream.notetrainer.ui.screens.MenuScreen
import com.dakyodream.notetrainer.ui.screens.StatsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteTrainerRoot()
        }
    }
}

@Composable
fun NoteTrainerRoot() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    var themeMode by rememberSaveable {
        mutableStateOf(settings.loadThemeMode()?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM)
    }
    NoteTrainerTheme(themeMode = themeMode) {
        NoteTrainerApp(themeMode = themeMode, onThemeChange = { mode ->
            themeMode = mode
            settings.saveThemeMode(mode.name)
        })
    }
}

@Composable
fun NoteTrainerApp(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    val nav = rememberNavController()
    val engine: GameEngine = viewModel()
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    var notation by rememberSaveable {
        mutableStateOf(settings.loadNotation()?.let { runCatching { Notation.valueOf(it) }.getOrNull() } ?: Notation.FRENCH)
    }
    val statsStore = remember { StatsStore(context) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        engine.onGameOver = { mode, difficulty, score, rounds, correct, wrong ->
            statsStore.add(
                GameRecord(
                    timestamp = System.currentTimeMillis(),
                    mode = mode,
                    difficulty = difficulty,
                    score = score,
                    roundsPlayed = rounds,
                    correctAnswers = correct,
                    wrongAnswers = wrong
                )
            )
        }
    }

    NavHost(navController = nav, startDestination = "menu") {
        composable("menu") {
            MenuScreen(
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                notation = notation,
                onNotationChange = { n ->
                    notation = n
                    settings.saveNotation(n.name)
                },
                onStartGame = { mode, difficulty, clef, n ->
                    engine.startGame(mode, difficulty, clef, n)
                    nav.navigate("game/${mode.name}")
                },
                onShowCredits = { nav.navigate("credits") },
                onShowInfo = { nav.navigate("info") },
                onShowLicense = { nav.navigate("license") },
                onShowStats = { nav.navigate("stats") },
                onShowPrivacy = { nav.navigate("privacy") },
                onShowGuide = { nav.navigate("guide") }
            )
        }
        composable("game/{mode}") { backStack ->
            val mode = GameMode.valueOf(backStack.arguments?.getString("mode") ?: GameMode.NAME_THE_NOTE.name)
            GameScreen(engine = engine, mode = mode, onExit = { nav.popBackStack() })
        }
        composable("guide") { NoteGuideScreen(notation = notation, onBack = { nav.popBackStack() }) }
        composable("credits") { CreditsScreen(onBack = { nav.popBackStack() }) }
        composable("info") { InfoScreen(onBack = { nav.popBackStack() }) }
        composable("license") { LicenseScreen(onBack = { nav.popBackStack() }) }
        composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }) }
        composable("stats") { StatsScreen(notation = notation, onBack = { nav.popBackStack() }) }
    }
    }
}
