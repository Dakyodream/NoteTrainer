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
    val roundsPlayed: Int,
    val correctAnswers: Int = score,
    val wrongAnswers: Int = (roundsPlayed - score).coerceAtLeast(0)
)

class StatsStore(context: Context) : StatsFileStore(File(context.filesDir, "stats.json"))

/**
 * Persistance des stats sur un fichier donné (testable sans Context Android).
 * Écriture atomique : temp + rename pour éviter la corruption si le processus
 * est tué au milieu d'une sauvegarde.
 */
open class StatsFileStore(private val file: File) {

    fun load(): MutableList<GameRecord> {
        if (!file.exists()) return mutableListOf()
        return try {
            val arr = JSONArray(file.readText())
            val list = mutableListOf<GameRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val score = o.optInt("score", 0)
                val rounds = o.optInt("rounds", 0)
                val correct = o.optInt("correct", score)
                val wrong = o.optInt("wrong", (rounds - correct).coerceAtLeast(0))
                list.add(
                    GameRecord(
                        timestamp = o.getLong("ts"),
                        mode = GameMode.valueOf(o.getString("mode")),
                        difficulty = Difficulty.valueOf(o.getString("diff")),
                        score = score,
                        roundsPlayed = rounds,
                        correctAnswers = correct,
                        wrongAnswers = wrong
                    )
                )
            }
            list
        } catch (_: Exception) {
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
                    put("correct", r.correctAnswers)
                    put("wrong", r.wrongAnswers)
                })
            }
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(arr.toString())
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                // rename échoué (ex. FS exotique) : fallback écriture directe
                file.writeText(arr.toString())
                tmp.delete()
            }
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
