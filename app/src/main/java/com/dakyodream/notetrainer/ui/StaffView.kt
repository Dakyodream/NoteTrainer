package com.dakyodream.notetrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Note

/** Conversion d'une note vers sa position verticale sur la portée.
 *  1 step = demi-interligne ; les lettres consécutives sont distantes d'1 step. */
object StaffGeometry {
    const val LINES = 5

    /** Index diatonique de la ligne du haut : F5 en clé de Sol, A3 en clé de Fa. */
    fun topLineDiatonic(clef: Clef): Int = when (clef) {
        Clef.TREBLE -> 3 + 5 * 7  // F5
        Clef.BASS -> 5 + 3 * 7   // A3
    }

    fun noteToStep(note: Note, clef: Clef): Float {
        val letterIdx = when (note.letter) {
            'C' -> 0; 'D' -> 1; 'E' -> 2; 'F' -> 3; 'G' -> 4; 'A' -> 5; 'B' -> 6
            else -> 0
        }
        val diatonic = letterIdx + note.octave * 7
        return (topLineDiatonic(clef) - diatonic).toFloat()
    }
}

data class StaffNote(
    val note: Note,
    val color: Color = Color.Black,
    val showLabel: Boolean = false,
    val accidentalSymbol: String? = null
)

@Composable
fun StaffView(
    clef: Clef,
    notes: List<StaffNote>,
    modifier: Modifier = Modifier,
    selectedStepY: Float? = null,
    onStepTap: ((Float) -> Unit)? = null
) {
    val lineColor = Color(0xFF444444)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .pointerInput(clef, onStepTap) {
                if (onStepTap != null) {
                    detectTapGestures { offset ->
                        val unit = size.height / 16f
                        val topLineY = size.height / 2f - 2f * unit
                        onStepTap((topLineY - offset.y) / unit)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val unit = h / 16f
        val topLineY = h / 2f - 2f * unit

        fun stepToY(step: Float): Float = topLineY - step * unit

        drawStaff(w, topLineY, unit, lineColor)
        drawClef(clef, topLineY, unit)

        notes.forEach { sn ->
            val y = stepToY(StaffGeometry.noteToStep(sn.note, clef))
            val x = w * 0.62f
            drawNoteHead(x, y, sn.color)
            drawLedgerLinesIfNeeded(x, y, topLineY, unit, lineColor)
            sn.accidentalSymbol?.let { sym ->
                drawAccidentalSym(x - 2.2f * unit, y, unit, sym, sn.color)
            }
            if (sn.showLabel) {
                drawNoteLabel(x, y, unit, sn.note.displayName)
            }
        }
    }
}

private fun DrawScope.drawStaff(w: Float, topLineY: Float, unit: Float, color: Color) {
    for (i in 0 until 5) {
        drawLine(
            color = color,
            start = Offset(0f, topLineY + i * unit),
            end = Offset(w, topLineY + i * unit),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawNoteHead(cx: Float, cy: Float, color: Color) {
    drawCircle(color = color, radius = 11f, center = Offset(cx, cy))
    drawLine(
        color = color,
        start = Offset(cx + 10f, cy - 2f),
        end = Offset(cx + 10f, cy - 38f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawLedgerLinesIfNeeded(cx: Float, y: Float, topLineY: Float, unit: Float, color: Color) {
    if (y < topLineY) {
        var lineY = topLineY - unit
        while (lineY >= y + 1f) {
            drawLine(color, Offset(cx - 20f, lineY), Offset(cx + 22f, lineY), 2f)
            lineY -= unit
        }
    }
    val bottomLineY = topLineY + 4f * unit
    if (y > bottomLineY) {
        var lineY = bottomLineY + unit
        while (lineY <= y - 1f) {
            drawLine(color, Offset(cx - 20f, lineY), Offset(cx + 22f, lineY), 2f)
            lineY += unit
        }
    }
}

private fun DrawScope.drawClef(clef: Clef, topLineY: Float, unit: Float) {
    when (clef) {
        Clef.TREBLE -> drawTrebleClef(60f, topLineY, unit)
        Clef.BASS -> drawBassClef(60f, topLineY, unit)
    }
}

private fun DrawScope.drawTrebleClef(x: Float, topLineY: Float, unit: Float) {
    val color = Color(0xFF1B1B1B)
    val path = Path().apply {
        cubicTo(x - unit * 0.8f, topLineY - unit * 3f, x + unit * 1.6f, topLineY + unit * 1f, x - unit * 0.4f, topLineY + unit * 5f)
        cubicTo(x - unit * 1.4f, topLineY + unit * 6f, x + unit * 1.2f, topLineY + unit * 7f, x + unit * 0.4f, topLineY + unit * 4.5f)
        cubicTo(x + unit * 1.8f, topLineY + unit * 2.5f, x - unit * 1.5f, topLineY + unit * 1.5f, x - unit * 0.6f, topLineY - unit * 1.5f)
        cubicTo(x - unit * 1.2f, topLineY - unit * 3.5f, x + unit * 1.4f, topLineY - unit * 3.2f, x + unit * 0.6f, topLineY - unit * 1.2f)
    }
    drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
    drawCircle(color, radius = unit * 0.4f, center = Offset(x - unit * 0.5f, topLineY + unit * 4.2f))
    drawLine(color, Offset(x + unit * 0.4f, topLineY - unit * 1.2f), Offset(x + unit * 1.3f, topLineY - unit * 2.6f), 3f)
}

private fun DrawScope.drawBassClef(x: Float, topLineY: Float, unit: Float) {
    val color = Color(0xFF1B1B1B)
    drawArc(
        color = color,
        startAngle = 250f,
        sweepAngle = 230f,
        useCenter = false,
        topLeft = Offset(x - unit * 1.1f, topLineY + unit * 0.5f),
        size = androidx.compose.ui.geometry.Size(unit * 2.2f, unit * 3.4f),
        style = Stroke(width = 5f, cap = StrokeCap.Round)
    )
    drawCircle(color, radius = unit * 0.28f, center = Offset(x + unit * 0.9f, topLineY + unit * 1.7f))
    drawCircle(color, radius = unit * 0.28f, center = Offset(x + unit * 0.9f, topLineY + unit * 2.6f))
}

private fun DrawScope.drawAccidentalSym(x: Float, y: Float, unit: Float, symbol: String, color: Color) {
    if (symbol.contains("♯")) {
        drawLine(color, Offset(x, y - unit), Offset(x, y + unit), 5f)
        drawLine(color, Offset(x - unit * 0.6f, y - unit * 0.25f), Offset(x + unit * 0.6f, y - unit * 0.25f), 3f)
        drawLine(color, Offset(x - unit * 0.6f, y + unit * 0.35f), Offset(x + unit * 0.6f, y + unit * 0.35f), 3f)
    } else if (symbol.contains("♭")) {
        drawLine(color, Offset(x, y - unit), Offset(x, y + unit * 0.3f), 4f)
        drawArc(color, 200f, 200f, false, Offset(x - unit * 0.4f, y - unit * 0.2f),
            androidx.compose.ui.geometry.Size(unit * 0.8f, unit * 1.0f), style = Stroke(4f))
    }
}

private fun DrawScope.drawNoteLabel(x: Float, y: Float, unit: Float, text: String) {
    drawLine(Color(0xFF888888), Offset(x - 20f, y + unit * 2.6f), Offset(x + 30f, y + unit * 2.6f), 1f)
}
