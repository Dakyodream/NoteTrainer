package com.dakyodream.notetrainer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple80 = Color(0xFFD0BCFF)
private val Purple40 = Color(0xFF6650a4)
private val Teal200 = Color(0xFF03DAC5)

private val LightColors = lightColorScheme(
    primary = Purple40,
    secondary = Teal200,
    surface = Color(0xFFF7F5FF)
)

private val DarkColors = darkColorScheme(
    primary = Purple80,
    secondary = Teal200
)

@Composable
fun NoteTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
