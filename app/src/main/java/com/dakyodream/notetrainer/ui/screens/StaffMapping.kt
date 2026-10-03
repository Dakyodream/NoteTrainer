package com.dakyodream.notetrainer.ui.screens

import com.dakyodream.notetrainer.core.Clef
import kotlin.math.roundToInt

/**
 * Convertit la position tapée sur la portée (en steps, 1 step = demi-interligne)
 * vers le numéro MIDI correspondant, selon la clé.
 * Ligne du haut : F5 en clé de Sol, A3 en clé de Fa.
 */
fun stepToMidi(step: Float, clef: Clef): Int {
    val topLine = when (clef) {
        Clef.TREBLE -> 3 + 5 * 7  // F5
        Clef.BASS -> 5 + 3 * 7   // A3
    }
    val d = topLine - step.roundToInt()
    val letterIdx = Math.floorMod(d, 7)
    val octave = Math.floorDiv(d, 7)
    val pc = intArrayOf(0, 2, 4, 5, 7, 9, 11)[letterIdx]
    return (octave + 1) * 12 + pc
}
