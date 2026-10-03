package com.dakyodream.notetrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Difficulty
import com.dakyodream.notetrainer.core.GameEngine
import com.dakyodream.notetrainer.core.GameMode
import com.dakyodream.notetrainer.ui.NoteTrainerTheme
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
            NoteTrainerTheme {
                NoteTrainerApp()
            }
        }
    }
}

@Composable
fun NoteTrainerApp() {
    val nav = rememberNavController()
    val engine: GameEngine = androidx.lifecycle.viewmodel.compose.viewModel()

    NavHost(navController = nav, startDestination = "menu") {
        composable("menu") {
            MenuScreen(
                onStartGame = { mode, difficulty, clef ->
                    engine.startGame(mode, difficulty, clef)
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
