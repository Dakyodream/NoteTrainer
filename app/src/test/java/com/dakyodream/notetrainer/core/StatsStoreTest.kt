package com.dakyodream.notetrainer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests de la persistance des stats (StatsFileStore, testable sans Android).
 */
class StatsStoreTest {

    private fun newStore(): Pair<StatsFileStore, File> {
        val dir = createTempDir("stats_test")
        return StatsFileStore(File(dir, "stats.json")) to dir
    }

    private fun record(ts: Long = 1000L, score: Int = 5, rounds: Int = 10) = GameRecord(
        timestamp = ts,
        mode = GameMode.NAME_THE_NOTE,
        difficulty = Difficulty.MEDIUM,
        score = score,
        roundsPlayed = rounds,
        correctAnswers = score,
        wrongAnswers = rounds - score
    )

    @Test
    fun load_missingFile_returnsEmpty() {
        val (store, _) = newStore()
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun save_isAtomic_noTmpFileLeft() {
        val (store, dir) = newStore()
        store.save(listOf(record(ts = 1L)))
        val tmp = File(dir, "stats.json.tmp")
        assertFalse("fichier temporaire residuel", tmp.exists())
        assertTrue(File(dir, "stats.json").exists())
        assertEquals(1, store.load().size)
    }

    @Test
    fun save_thenLoad_roundTrips() {
        val (store, _) = newStore()
        val records = listOf(record(ts = 42L, score = 3), record(ts = 43L, score = 7, rounds = 12))
        store.save(records)
        val loaded = store.load()
        assertEquals(2, loaded.size)
        assertEquals(42L, loaded[0].timestamp)
        assertEquals(3, loaded[0].score)
        assertEquals(7, loaded[1].score)
        assertEquals(12, loaded[1].roundsPlayed)
        assertEquals(GameMode.NAME_THE_NOTE, loaded[0].mode)
        assertEquals(Difficulty.MEDIUM, loaded[0].difficulty)
    }

    @Test
    fun add_appendsToExisting() {
        val (store, _) = newStore()
        store.add(record(ts = 1L))
        store.add(record(ts = 2L))
        val loaded = store.load()
        assertEquals(2, loaded.size)
        assertEquals(1L, loaded[0].timestamp)
        assertEquals(2L, loaded[1].timestamp)
    }

    @Test
    fun load_corruptedFile_returnsEmpty() {
        val (store, dir) = newStore()
        File(dir, "stats.json").writeText("not json at all {{{")
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun gameRecord_derivedCounts() {
        val r = GameRecord(
            timestamp = 0L, mode = GameMode.EAR_TRAINING,
            difficulty = Difficulty.EASY, score = 4, roundsPlayed = 6
        )
        assertEquals(4, r.correctAnswers)
        assertEquals(2, r.wrongAnswers)
    }

    @Test
    fun gameRecord_wrongNeverNegative() {
        val r = GameRecord(
            timestamp = 0L, mode = GameMode.EAR_TRAINING,
            difficulty = Difficulty.EASY, score = 10, roundsPlayed = 6
        )
        assertEquals(0, r.wrongAnswers)
    }

    @Test
    fun constants_weekMonthBounds() {
        assertEquals(7L * 24 * 3600 * 1000, StatsStore.WEEK_MS)
        assertEquals(30L * 24 * 3600 * 1000, StatsStore.MONTH_MS)
    }
}
