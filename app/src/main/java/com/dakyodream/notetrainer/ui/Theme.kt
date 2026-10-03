package com.dakyodream.notetrainer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6650A4),
    secondary = Color(0xFF03DAC5),
    surface = Color(0xFFFBF8FF),
    background = Color(0xFFFBF8FF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFF03DAC5),
    surface = Color(0xFF141218),
    background = Color(0xFF141218)
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

@Composable
fun NoteTrainerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            content = content
        )
    }
}

/** Couleurs de la portée selon le thème (fond clair = encre sombre, et inversement). */
@Composable
fun staffInkColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFF1B1B1B) else Color(0xFFE8E0F0)

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
