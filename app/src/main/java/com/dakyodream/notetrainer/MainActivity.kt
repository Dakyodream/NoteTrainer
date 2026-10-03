package com.dakyodream.notetrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Difficulty
import com.dakyodream.notetrainer.core.GameEngine
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.core.Notation
import com.dakyodream.notetrainer.ui.NoteTrainerTheme
import com.dakyodream.notetrainer.ui.ThemeMode
import com.dakyodream.notetrainer.ui.screens.CreditsScreen
import com.dakyodream.notetrainer.ui.screens.GameScreen
import com.dakyodream.notetrainer.ui.screens.InfoScreen
import com.dakyodream.notetrainer.ui.screens.LicenseScreen
import com.dakyodream.notetrainer.ui.screens.MenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            NoteTrainerTheme(themeMode = themeMode) {
                NoteTrainerApp(themeMode = themeMode, onThemeChange = { themeMode = it })
            }
        }
    }
}

@Composable
fun NoteTrainerApp(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    val nav = rememberNavController()
    val engine: GameEngine = viewModel()
    var notation by rememberSaveable { mutableStateOf(Notation.FRENCH) }

    NavHost(navController = nav, startDestination = "menu") {
        composable("menu") {
            MenuScreen(
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                notation = notation,
                onNotationChange = { notation = it },
                onStartGame = { mode, difficulty, clef, n ->
                    engine.startGame(mode, difficulty, clef, n)
                    nav.navigate("game/${mode.name}")
                },
                onShowCredits = { nav.navigate("credits") },
                onShowInfo = { nav.navigate("info") },
                onShowLicense = { nav.navigate("license") }
            )
        }
        composable("game/{mode}") { backStack ->
            val mode = GameMode.valueOf(backStack.arguments?.getString("mode") ?: GameMode.NAME_THE_NOTE.name)
            GameScreen(engine = engine, mode = mode, onExit = { nav.popBackStack() })
        }
        composable("credits") { CreditsScreen(onBack = { nav.popBackStack() }) }
        composable("info") { InfoScreen(onBack = { nav.popBackStack() }) }
        composable("license") { LicenseScreen(onBack = { nav.popBackStack() }) }
    }
}
