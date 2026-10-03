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
 * Rendu de portée inspiré de LilyPond (https://github.com/lilypond/lilypond) :
 * portée fine et régulière, clés de Sol et de Fa dessinées en courbes de Bézier,
 * têtes de notes ovales inclinées, queues en fuseau, lignes supplémentaires courtes,
 * altérations (dièse/bémol) positionnées à gauche de la tête.
 * 1 step = demi-interligne ; step 0 = ligne du haut de la portée.
 */
data class StaffNote(
    val note: Note,
    val color: Color = Color(0xFF1B1B1B),
    val label: String? = null
)

data class StaffHitResult(val step: Int, val accidental: Int)

@Composable
fun StaffView(
    clef: Clef,
    notes: List<StaffNote>,
    modifier: Modifier = Modifier,
    noteColor: Color = Color(0xFF1B1B1B),
    staffColor: Color = Color(0xFF333333),
    onStaffTap: ((StaffHitResult) -> Unit)? = null
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .pointerInput(clef, onStaffTap) {
                if (onStaffTap != null) {
                    detectTapGestures { offset ->
                        val unit = size.height / 24f
                        val topLineY = 14f * unit
                        val rawStep = (topLineY - offset.y) / unit
                        onStaffTap(StaffHitResult(step = rawStep.toInt(), accidental = 0))
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val unit = h / 24f
        val topLineY = 14f * unit

        fun stepToY(step: Float): Float = topLineY - step * unit

        fun noteStep(n: Note): Float {
            val letterIdx = when (n.letter) {
                'C' -> 0; 'D' -> 1; 'E' -> 2; 'F' -> 3; 'G' -> 4; 'A' -> 5; 'B' -> 6
                else -> 0
            }
            val diatonic = letterIdx + n.octave * 7
            return (Notes.topLineDiatonic(clef) - diatonic).toFloat()
        }

        // Portée
        val startX = 4f
        val endX = w - 8f
        for (i in 0 until 5) {
            drawLine(
                color = staffColor,
                start = Offset(startX, topLineY + i * unit),
                end = Offset(endX, topLineY + i * unit),
                strokeWidth = unit * 0.13f
            )
        }

        // Clé
        when (clef) {
            Clef.TREBLE -> drawTrebleClef(startX + unit * 1.4f, topLineY, unit, staffColor)
            Clef.BASS -> drawBassClef(startX + unit * 1.2f, topLineY, unit, staffColor)
        }

        // Notes
        val cx = w * 0.62f
        notes.forEach { sn ->
            val step = noteStep(sn.note)
            val cy = stepToY(step)
            val stemUp = step < 2f  // queue vers le haut si la note est au-dessus de la ligne médiane (B4/Sol)
            drawNote(cx, cy, unit, stemUp, sn.color)
            drawLedgerLines(cx, cy, step, topLineY, unit, staffColor)
            if (sn.note.accidental != 0) {
                drawAccidental(cx - unit * 2.6f, cy, unit, sn.note.accidental, sn.color)
            }
        }

        // Zone de réponse visible (mode placement) : ligne pointillée de l'étendue jouable
        if (onStaffTap != null) {
            for (s in Notes.RANGE_MIN_STEP..Notes.RANGE_MAX_STEP) {
                val y = stepToY(s.toFloat())
                val isLine = s % 2 == 0
                if (s < 0 || s > 8) {
                    // positions hors portée : petit tiret repère sur la gauche
                    drawLine(
                        color = staffColor.copy(alpha = 0.25f),
                        start = Offset(cx - unit * 2.2f, y),
                        end = Offset(cx - unit * 1.4f, y),
                        strokeWidth = unit * 0.1f
                    )
                }
                if (isLine && (s < 0 || s > 8)) {
                    drawLine(
                        color = staffColor.copy(alpha = 0.6f),
                        start = Offset(cx - unit * 1.2f, y),
                        end = Offset(cx + unit * 1.2f, y),
                        strokeWidth = unit * 0.12f
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNote(cx: Float, cy: Float, unit: Float, stemUp: Boolean, color: Color) {
    val rx = unit * 0.62f
    val ry = unit * 0.42f
    // Tête ovale inclinée (style LilyPond)
    val headPath = Path().apply {
        // ellipse tournée d'environ -20°
        val pts = 24
        for (i in 0..pts) {
            val t = i.toFloat() / pts * (2f * PI.toFloat())
            val x = rx * cos(t)
            val y = ry * sin(t)
            val rot = -0.35f
            val xr = cx + x * cos(rot) - y * sin(rot)
            val yr = cy + x * sin(rot) + y * cos(rot)
            if (i == 0) moveTo(xr, yr) else lineTo(xr, yr)
        }
        close()
    }
    drawPath(headPath, color)

    // Queue en fuseau, à droite de la tête
    val stemX = cx + rx * 0.92f
    val stemLen = unit * 3.3f
    val stemTop = if (stemUp) cy - stemLen else cy + stemLen
    val stemBottom = cy
    drawLine(
        color = color,
        start = Offset(stemX, if (stemUp) stemBottom - unit * 0.2f else stemBottom + unit * 0.2f),
        end = Offset(stemX, stemTop),
        strokeWidth = unit * 0.16f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawLedgerLines(
    cx: Float, cy: Float, step: Float,
    topLineY: Float, unit: Float, color: Color
) {
    if (step < 0f) {
        var s = 0f
        while (s > step) {
            s -= 2f
            if (s >= step) {
                val y = topLineY - s * unit
                drawLine(color, Offset(cx - unit * 1.4f, y), Offset(cx + unit * 1.4f, y), unit * 0.13f)
            }
        }
    }
    if (step > 8f) {
        var s = 8f
        while (s < step) {
            s += 2f
            if (s <= step) {
                val y = topLineY - s * unit
                drawLine(color, Offset(cx - unit * 1.4f, y), Offset(cx + unit * 1.4f, y), unit * 0.13f)
            }
        }
    }
}

/** Clé de Sol stylisée, dessinée en courbes de Bézier à la manière de LilyPond. */
private fun DrawScope.drawTrebleClef(x: Float, topLineY: Float, unit: Float, color: Color) {
    val p = Path()
    // Partie inférieure : grande boucle sous la portée
    p.moveTo(x, topLineY + unit * 4.6f)
    p.cubicTo(x - unit * 0.9f, topLineY + unit * 5.8f, x + unit * 0.5f, topLineY + unit * 6.4f, x + unit * 0.35f, topLineY + unit * 4.4f)
    // Boucle centrale traversant la portée (le "cœur" du S)
    p.cubicTo(x + unit * 0.2f, topLineY + unit * 2.9f, x - unit * 1.6f, topLineY + unit * 2.4f, x - unit * 1.5f, topLineY + unit * 0.8f)
    p.cubicTo(x - unit * 1.4f, topLineY - unit * 0.6f, x + unit * 0.6f, topLineY - unit * 0.5f, x + unit * 0.7f, topLineY - unit * 2.2f)
    // Crochet supérieur
    p.cubicTo(x + unit * 0.75f, topLineY - unit * 3.4f, x - unit * 0.9f, topLineY - unit * 3.6f, x - unit * 1.2f, topLineY - unit * 2.4f)
    drawPath(p, color, style = Stroke(width = unit * 0.22f, cap = StrokeCap.Round))

    // Boucle pleine du bas (point d'attache)
    val dot = Path().apply {
        addOval(Rect(center = Offset(x - unit * 0.05f, topLineY + unit * 3.6f), radius = unit * 0.5f))
    }
    drawPath(dot, color)
}

/** Clé de Fa : deux points et une virgule épaisse. */
private fun DrawScope.drawBassClef(x: Float, topLineY: Float, unit: Float, color: Color) {
    val p = Path()
    p.moveTo(x + unit * 0.4f, topLineY - unit * 0.8f)
    p.cubicTo(x - unit * 1.4f, topLineY + unit * 0.1f, x - unit * 1.2f, topLineY + unit * 2.4f, x + unit * 0.3f, topLineY + unit * 3.0f)
    p.cubicTo(x + unit * 0.1f, topLineY + unit * 2.0f, x + unit * 0.1f, topLineY + unit * 1.2f, x + unit * 0.4f, topLineY - unit * 0.8f)
    drawPath(p, color, style = Stroke(width = unit * 0.35f, cap = StrokeCap.Round))

    drawCircle(color = color, radius = unit * 0.22f, center = Offset(x + unit * 0.9f, topLineY + unit * 2.0f))
    drawCircle(color = color, radius = unit * 0.22f, center = Offset(x + unit * 0.9f, topLineY + unit * 2.8f))
}

private fun DrawScope.drawAccidental(x: Float, y: Float, unit: Float, accidental: Int, color: Color) {
    if (accidental == 1) {
        // Dièse : deux verticales + deux obliques
        val h = unit * 1.3f
        drawLine(color, Offset(x - unit * 0.15f, y - h), Offset(x - unit * 0.15f, y + h), unit * 0.14f)
        drawLine(color, Offset(x + unit * 0.15f, y - h), Offset(x + unit * 0.15f, y + h), unit * 0.14f)
        drawLine(color, Offset(x - unit * 0.4f, y - unit * 0.15f), Offset(x + unit * 0.4f, y - unit * 0.35f), unit * 0.16f)
        drawLine(color, Offset(x - unit * 0.4f, y + unit * 0.35f), Offset(x + unit * 0.4f, y + unit * 0.15f), unit * 0.16f)
    } else if (accidental == -1) {
        // Bémol : verticale + boucle
        drawLine(color, Offset(x, y - unit * 1.2f), Offset(x, y + unit * 0.4f), unit * 0.16f)
        val loop = Path().apply {
            moveTo(x, y + unit * 0.4f)
            cubicTo(x - unit * 0.55f, y + unit * 0.75f, x - unit * 0.5f, y - unit * 0.15f, x, y - unit * 0.05f)
        }
        drawPath(loop, color, style = Stroke(width = unit * 0.16f, cap = StrokeCap.Round))
    }
}
