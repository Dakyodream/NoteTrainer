package com.dakyodream.notetrainer.core

import android.content.Context

/**
 * Persistance des préférences utilisateur (thème, notation).
 * 100 % local (SharedPreferences privées de l'app), aucune donnée collectée.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun loadThemeMode(): String? = prefs.getString(KEY_THEME, null)
    fun saveThemeMode(mode: String) = prefs.edit().putString(KEY_THEME, mode).apply()

    fun loadNotation(): String? = prefs.getString(KEY_NOTATION, null)
    fun saveNotation(notation: String) = prefs.edit().putString(KEY_NOTATION, notation).apply()

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_NOTATION = "notation"
    }
}
