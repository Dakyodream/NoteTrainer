package com.dakyodream.notetrainer.core

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin

/**
 * Synthétise et joue les notes en local : aucun fichier audio embarqué, aucune dépendance.
 * Chaque note est générée en WAV (1 s, sinusoïde + harmoniques + enveloppe) puis jouée via SoundPool.
 */
class AudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val soundIds = ConcurrentHashMap<String, Int>()
    private val loading = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(Dispatchers.IO)

    var isReady by mutableStateOf(false)
        private set

    fun playNote(note: Note, durationSec: Double = 1.0) {
        val key = note.id
        val existing = soundIds[key]
        if (existing != null) {
            soundPool.play(existing, 1f, 1f, 1, 0, 1f)
            return
        }
        if (loading.add(key)) {
            scope.launch {
                val id = synthesizeAndLoad(note, durationSec)
                loading.remove(key)
                if (id != null) {
                    soundIds[key] = id
                    isReady = true
                    soundPool.play(id, 1f, 1f, 1, 0, 1f)
                }
            }
        }
    }

    fun release() {
        soundPool.release()
    }

    private suspend fun synthesizeAndLoad(note: Note, durationSec: Double): Int? =
        withContext(Dispatchers.IO) {
            try {
                val freq = Notes.frequency(note)
                val samples = synthesizeSine(freq, durationSec)
                val wav = pcmToWav(samples, SAMPLE_RATE)
                val f = File.createTempFile("note_", ".wav", appContext.cacheDir)
                FileOutputStream(f).use { it.write(wav) }
                soundPool.load(f.absolutePath, 1)
            } catch (e: Exception) {
                null
            }
        }

    companion object {
        const val SAMPLE_RATE = 44100

        fun synthesizeSine(freq: Double, durationSec: Double = 1.0): ShortArray {
            val n = (durationSec * SAMPLE_RATE).toInt()
            val out = ShortArray(n)
            val attack = (0.02 * SAMPLE_RATE).toInt().coerceAtMost(n)
            val decay = (0.10 * SAMPLE_RATE).toInt().coerceAtMost(n - attack)
            val release = (0.15 * SAMPLE_RATE).toInt().coerceAtMost(n - attack - decay)
            val sustainLevel = 0.85

            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                val s = sin(2 * PI * freq * t) +
                        0.35 * sin(2 * PI * freq * 2 * t) +
                        0.12 * sin(2 * PI * freq * 3 * t)
                val env = when {
                    i < attack -> (i.toDouble() / attack)
                    i < attack + decay -> 1.0 - (1.0 - sustainLevel) * ((i - attack).toDouble() / decay)
                    i > n - release -> {
                        val relPos = (n - i).toDouble() / release
                        sustainLevel * (0.5 * (1.0 - kotlin.math.cos(relPos * PI)))
                    }
                    else -> sustainLevel
                }
                val v = (s * env * 0.45 * Short.MAX_VALUE).toInt()
                out[i] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        fun pcmToWav(pcm: ShortArray, sampleRate: Int): ByteArray {
            val byteCount = pcm.size * 2
            val out = ByteArrayOutputStream(44 + byteCount)
            val header = ByteArray(44)
            fun writeStr(offset: Int, s: String) {
                for (i in s.indices) header[offset + i] = s[i].code.toByte()
            }
            fun writeInt(offset: Int, v: Int) {
                header[offset] = (v and 0xff).toByte()
                header[offset + 1] = ((v shr 8) and 0xff).toByte()
                header[offset + 2] = ((v shr 16) and 0xff).toByte()
                header[offset + 3] = ((v shr 24) and 0xff).toByte()
            }
            fun writeShort(offset: Int, v: Int) {
                header[offset] = (v and 0xff).toByte()
                header[offset + 1] = ((v shr 8) and 0xff).toByte()
            }
            writeStr(0, "RIFF")
            writeInt(4, 36 + byteCount)
            writeStr(8, "WAVE")
            writeStr(12, "fmt ")
            writeInt(16, 16)
            writeShort(20, 1)
            writeShort(22, 1)
            writeInt(24, sampleRate)
            writeInt(28, sampleRate * 2)
            writeShort(32, 2)
            writeShort(34, 16)
            writeStr(36, "data")
            writeInt(40, byteCount)
            out.write(header)
            val bytes = ByteArray(byteCount)
            for (i in pcm.indices) {
                bytes[i * 2] = (pcm[i].toInt() and 0xff).toByte()
                bytes[i * 2 + 1] = ((pcm[i].toInt() shr 8) and 0xff).toByte()
            }
            out.write(bytes)
            return out.toByteArray()
        }
    }
}
