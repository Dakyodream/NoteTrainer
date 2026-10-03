package com.dakyodream.notetrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Note
import com.dakyodream.notetrainer.core.Notes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Portée dessinée en Canvas, style gravure classique inspiré de LilyPond
 * (https://github.com/lilypond/lilypond) :
 * - lignes espacées d'un interligne complet (1 step = demi-interligne) ;
 * - clé de Sol en courbe continue (crochet, S central, boucle basse) ;
 * - clé de Fa (virgule épaisse + 2 points) ;
 * - tête de note ovale inclinée, queue en fuseau, direction selon la position ;
 * - lignes supplémentaires courtes de part et d'autre de la tête.
 * Géométrie : step 0 = ligne du haut (F5 en clé de Sol / A3 en clé de Fa) ;
 * une lettre = 1 step ; une ligne de portée = 2 steps.
 */
data class StaffNote(
    val note: Note,
    val color: Color = Color(0xFF1B1B1B)
)

data class StaffHitResult(val step: Int, val accidental: Int)

/** Nombre total de steps visibles au-dessus/au-dessous de la portée (marges incluses). */
private const val STEPS_ABOVE = 12f
private const val STEPS_BELOW = 12f

@Composable
fun StaffView(
    clef: Clef,
    notes: List<StaffNote>,
    modifier: Modifier = Modifier,
    inkColor: Color = Color(0xFF1B1B1B),
    onStaffTap: ((StaffHitResult) -> Unit)? = null
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .pointerInput(clef, onStaffTap) {
                if (onStaffTap != null) {
                    detectTapGestures { offset ->
                        val unit = size.height / (STEPS_ABOVE + STEPS_BELOW)
                        val topLineY = STEPS_ABOVE * unit
                        val rawStep = (offset.y - topLineY) / unit
                        onStaffTap(StaffHitResult(step = rawStep.toInt(), accidental = 0))
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val unit = h / (STEPS_ABOVE + STEPS_BELOW)
        val topLineY = STEPS_ABOVE * unit

        fun stepToY(step: Float): Float = topLineY + step * unit

        fun noteStep(n: Note): Float {
            val letterIdx = when (n.letter) {
                'C' -> 0; 'D' -> 1; 'E' -> 2; 'F' -> 3; 'G' -> 4; 'A' -> 5; 'B' -> 6
                else -> 0
            }
            return (Notes.topLineDiatonic(clef) - (letterIdx + n.octave * 7)).toFloat()
        }

        // --- Portée : 5 lignes espacées d'un interligne (2 steps) ---
        val staffLeft = 2f
        val staffRight = w - 2f
        for (i in 0 until 5) {
            drawLine(
                color = inkColor,
                start = Offset(staffLeft, topLineY + i * 2f * unit),
                end = Offset(staffRight, topLineY + i * 2f * unit),
                strokeWidth = unit * 0.14f
            )
        }

        // --- Clé ---
        when (clef) {
            Clef.TREBLE -> drawTrebleClef(staffLeft + unit * 3f, topLineY, unit, inkColor)
            Clef.BASS -> drawBassClef(staffLeft + unit * 2.4f, topLineY, unit, inkColor)
        }

        // --- Notes ---
        val cx = w * 0.62f
        notes.forEach { sn ->
            val step = noteStep(sn.note)
            val cy = stepToY(step)
            // Convention de gravure : note au-dessus de la ligne médiane (step < 4)
            // => queue vers le bas ; sinon queue vers le haut.
            val stemUp = step >= 4f
            drawNoteHead(cx, cy, unit, stemUp, sn.color)
            drawLedgerLines(cx, step, topLineY, unit, inkColor)
            if (sn.note.accidental != 0) {
                drawAccidental(cx - unit * 2.8f, cy, unit, sn.note.accidental, sn.color)
            }
        }
    }
}

/** Tête de note : ovale incliné plein, avec queue en fuseau. */
private fun DrawScope.drawNoteHead(cx: Float, cy: Float, unit: Float, stemUp: Boolean, color: Color) {
    val rx = unit * 0.58f
    val ry = unit * 0.40f
    val rot = -0.32f
    val cosR = cos(rot)
    val sinR = sin(rot)
    val path = Path()
    val segments = 20
    for (i in 0..segments) {
        val t = i.toFloat() / segments * 2f * PI.toFloat()
        val x = rx * cos(t)
        val y = ry * sin(t)
        val xr = cx + x * cosR - y * sinR
        val yr = cy + x * sinR + y * cosR
        if (i == 0) path.moveTo(xr, yr) else path.lineTo(xr, yr)
    }
    path.close()
    drawPath(path, color)

    // Queue en fuseau (vers le haut ou le bas selon stemUp)
    val stemX = cx + rx * 0.95f
    val stemLen = unit * 6.5f
    val yEnd = if (stemUp) cy - stemLen else cy + stemLen
    drawLine(
        color = color,
        start = Offset(stemX, cy + if (stemUp) -ry * 0.4f else ry * 0.4f),
        end = Offset(stemX, yEnd),
        strokeWidth = unit * 0.17f,
        cap = StrokeCap.Round
    )
}

/** Lignes supplémentaires : au-dessus (step < 0) et au-dessous (step > 8). */
private fun DrawScope.drawLedgerLines(
    cx: Float, step: Float, topLineY: Float, unit: Float, color: Color
) {
    val halfLen = unit * 1.5f
    if (step > 8f) {
        var s = 8f
        while (s < step) {
            s += 2f
            if (s <= step) {
                val y = topLineY + s * unit
                drawLine(color, Offset(cx - halfLen, y), Offset(cx + halfLen, y), unit * 0.14f)
            }
        }
    }
    if (step < 0f) {
        var s = 0f
        while (s > step) {
            s -= 2f
            if (s >= step) {
                val y = topLineY + s * unit
                drawLine(color, Offset(cx - halfLen, y), Offset(cx + halfLen, y), unit * 0.14f)
            }
        }
    }
}

/** Clé de Sol : courbe continue type gravure — crochet haut, S central, boucle basse. */
private fun DrawScope.drawTrebleClef(x: Float, topLineY: Float, unit: Float, color: Color) {
    val strokeWidth = unit * 0.24f
    val path = Path().apply {
        // Départ : pointe basse, sous la 5e ligne
        moveTo(x, topLineY + unit * 5.2f)
        // Grande boucle du bas (sous la portée)
        cubicTo(x - unit * 1.1f, topLineY + unit * 6.2f, x + unit * 0.6f, topLineY + unit * 6.4f, x + unit * 0.4f, topLineY + unit * 4.2f)
        // Remontée vers le cœur du S (3e ligne)
        cubicTo(x + unit * 0.3f, topLineY + unit * 2.6f, x - unit * 1.5f, topLineY + unit * 2.2f, x - unit * 1.3f, topLineY + unit * 0.6f)
        // Boucle supérieure du S (autour de la 2e ligne)
        cubicTo(x - unit * 1.1f, topLineY - unit * 0.8f, x + unit * 0.4f, topLineY - unit * 0.6f, x + unit * 0.6f, topLineY - unit * 1.6f)
        // Crochet terminal au-dessus de la portée
        cubicTo(x + unit * 0.8f, topLineY - unit * 2.8f, x - unit * 1.0f, topLineY - unit * 3.0f, x - unit * 1.2f, topLineY - unit * 1.8f)
    }
    drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

/** Clé de Fa : virgule épaisse + deux points. */
private fun DrawScope.drawBassClef(x: Float, topLineY: Float, unit: Float, color: Color) {
    val path = Path().apply {
        moveTo(x + unit * 0.5f, topLineY - unit * 1.0f)
        // Tête : grand arc vers la gauche
        cubicTo(x - unit * 1.6f, topLineY + unit * 0.2f, x - unit * 1.2f, topLineY + unit * 2.6f, x + unit * 0.4f, topLineY + unit * 3.2f)
        // Retour intérieur
        cubicTo(x + unit * 0.2f, topLineY + unit * 2.2f, x + unit * 0.2f, topLineY + unit * 1.4f, x + unit * 0.5f, topLineY - unit * 1.0f)
    }
    drawPath(path, color, style = Stroke(width = unit * 0.4f, cap = StrokeCap.Round))

    drawCircle(color, radius = unit * 0.24f, center = Offset(x + unit * 1.1f, topLineY + unit * 2.0f))
    drawCircle(color, radius = unit * 0.24f, center = Offset(x + unit * 1.1f, topLineY + unit * 2.8f))
}

/** Altération : dièse (2 verticales + 2 obliques) ou bémol (verticale + boucle). */
private fun DrawScope.drawAccidental(x: Float, y: Float, unit: Float, accidental: Int, color: Color) {
    if (accidental == 1) {
        val h = unit * 1.4f
        drawLine(color, Offset(x - unit * 0.18f, y - h), Offset(x - unit * 0.18f, y + h), unit * 0.15f)
        drawLine(color, Offset(x + unit * 0.18f, y - h), Offset(x + unit * 0.18f, y + h), unit * 0.15f)
        drawLine(color, Offset(x - unit * 0.45f, y - unit * 0.1f), Offset(x + unit * 0.45f, y - unit * 0.3f), unit * 0.17f)
        drawLine(color, Offset(x - unit * 0.45f, y + unit * 0.5f), Offset(x + unit * 0.45f, y + unit * 0.3f), unit * 0.17f)
    } else if (accidental == -1) {
        drawLine(color, Offset(x, y - unit * 1.3f), Offset(x, y + unit * 0.5f), unit * 0.17f)
        val loop = Path().apply {
            moveTo(x, y + unit * 0.5f)
            cubicTo(x - unit * 0.6f, y + unit * 0.85f, x - unit * 0.55f, y - unit * 0.1f, x, y - unit * 0.05f)
        }
        drawPath(loop, color, style = Stroke(width = unit * 0.17f, cap = StrokeCap.Round))
    }
}
