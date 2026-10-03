package com.dakyodream.notetrainer.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Statistiques de jeu, 100 % locales (fichier JSON dans le stockage privé de l'app).
 * Aucune connexion, aucune donnée collectée, hors-ligne total.
 */
data class GameRecord(
    val timestamp: Long,
    val mode: GameMode,
    val difficulty: Difficulty,
    val score: Int,
    val roundsPlayed: Int
)

class StatsStore(context: Context) {

    private val file: File = File(context.filesDir, "stats.json")

    fun load(): MutableList<GameRecord> {
        if (!file.exists()) return mutableListOf()
        return try {
            val arr = JSONArray(file.readText())
            val list = mutableListOf<GameRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    GameRecord(
                        timestamp = o.getLong("ts"),
                        mode = GameMode.valueOf(o.getString("mode")),
                        difficulty = Difficulty.valueOf(o.getString("diff")),
                        score = o.getInt("score"),
                        roundsPlayed = o.getInt("rounds")
                    )
                )
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun save(records: List<GameRecord>) {
        try {
            val arr = JSONArray()
            records.forEach { r ->
                arr.put(JSONObject().apply {
                    put("ts", r.timestamp)
                    put("mode", r.mode.name)
                    put("diff", r.difficulty.name)
                    put("score", r.score)
                    put("rounds", r.roundsPlayed)
                })
            }
            file.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    fun add(record: GameRecord) {
        val list = load()
        list.add(record)
        save(list)
    }

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val WEEK_MS = 7L * DAY_MS
        const val MONTH_MS = 30L * DAY_MS
    }
}
