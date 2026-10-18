package com.dakyodream.notetrainer.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Note
import com.dakyodream.notetrainer.core.Notes
import kotlin.math.roundToInt

data class StaffNote(
    val note: Note,
    val color: Color = Color(0xFF1B1B1B)
)

data class StaffHitResult(val step: Int, val accidental: Int)

// Symboles musicaux SMuFL (Bravura Font)
private const val SMUFL_CLEF_TREBLE = "\uE050"  // gClef
private const val SMUFL_CLEF_BASS = "\uE062"    // fClef
private const val SMUFL_NOTEHEAD_BLACK = "\uE0A4" // noteheadBlack
private const val SMUFL_ACCIDENTAL_SHARP = "\uE260"
private const val SMUFL_ACCIDENTAL_NATURAL = "\uE261"
private const val SMUFL_ACCIDENTAL_FLAT = "\uE262"

private var bravuraTypeface: Typeface? = null

private fun getBravuraTypeface(context: Context): Typeface {
    if (bravuraTypeface == null) {
        bravuraTypeface = Typeface.createFromAsset(context.assets, "Bravura.otf")
    }
    return bravuraTypeface!!
}

/**
 * Calcule l'index diatonique d'une note (0 pour C0, 7 pour C1, etc.).
 */
private fun Note.diatonicIndex(): Int {
    val letterIdx = when (letter) {
        'C' -> 0; 'D' -> 1; 'E' -> 2; 'F' -> 3; 'G' -> 4; 'A' -> 5; 'B' -> 6
        else -> 0
    }
    return letterIdx + octave * 7
}

/**
 * Calcule le step (demi-interligne) d'une note selon la clé.
 * step 0 = ligne du haut (F5 en Sol, A3 en Fa)
 * step 4 = ligne du milieu (B4 en Sol, D3 en Fa)
 * step 8 = ligne du bas (E4 en Sol, G2 en Fa)
 */
fun noteToStep(note: Note, clef: Clef): Int {
    val topDiatonic = Notes.topLineDiatonic(clef)
    val noteDiatonic = note.diatonicIndex()
    return topDiatonic - noteDiatonic
}

@Composable
fun StaffView(
    clef: Clef,
    notes: List<StaffNote>,
    modifier: Modifier = Modifier,
    inkColor: Color = Color(0xFF1B1B1B),
    heightDp: Dp = 260.dp,
    draftStep: Int? = null,
    draftAccidental: Int = 0,
    showGuideLedgerLines: Boolean = true,
    onStaffTap: ((StaffHitResult) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val typeface = remember(context) { getBravuraTypeface(context) }
    val primaryHighlightColor = MaterialTheme.colorScheme.primary

    var activeHoverStep by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .let { m ->
                    if (onStaffTap != null) {
                        m.pointerInput(clef, heightDp) {
                            val centerY = size.height / 2f
                            val spaceHeight = with(density) { 22.dp.toPx() }
                            val stepSize = spaceHeight / 2f

                            detectTapGestures { offset ->
                                val rawStep = ((offset.y - centerY) / stepSize + 4f).roundToInt()
                                val clampedStep = rawStep.coerceIn(Notes.RANGE_MIN_STEP, Notes.RANGE_MAX_STEP)
                                onStaffTap(StaffHitResult(step = clampedStep, accidental = 0))
                            }
                        }.pointerInput(clef, heightDp) {
                            val centerY = size.height / 2f
                            val spaceHeight = with(density) { 22.dp.toPx() }
                            val stepSize = spaceHeight / 2f

                            detectDragGestures(
                                onDragStart = { offset ->
                                    val rawStep = ((offset.y - centerY) / stepSize + 4f).roundToInt()
                                    activeHoverStep = rawStep.coerceIn(Notes.RANGE_MIN_STEP, Notes.RANGE_MAX_STEP)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val rawStep = ((change.position.y - centerY) / stepSize + 4f).roundToInt()
                                    activeHoverStep = rawStep.coerceIn(Notes.RANGE_MIN_STEP, Notes.RANGE_MAX_STEP)
                                },
                                onDragEnd = {
                                    val finalStep = activeHoverStep
                                    activeHoverStep = null
                                    finalStep?.let { step ->
                                        onStaffTap(StaffHitResult(step = step, accidental = 0))
                                    }
                                },
                                onDragCancel = {
                                    activeHoverStep = null
                                }
                            )
                        }
                    } else m
                }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // 1 interligne = 22dp. En SMuFL, la taille de police = 4 x interligne
            val spaceHeight = 22.dp.toPx()
            val stepSize = spaceHeight / 2f
            val lineThickness = 2.dp.toPx()
            val fontSizePx = spaceHeight * 4f

            val musicPaint = Paint().apply {
                this.typeface = typeface
                this.textSize = fontSizePx
                this.color = inkColor.toArgb()
                this.isAntiAlias = true
            }

            // 1. Lignes supplémentaires de repère (guide ledger lines) en filigrane clair
            if (showGuideLedgerLines) {
                val guideSteps = listOf(-4, -2, 10, 12)
                val guideWidth = spaceHeight * 2.2f
                val guideX = width / 2f
                val guideColor = inkColor.copy(alpha = 0.22f)
                guideSteps.forEach { gStep ->
                    val gy = centerY + (gStep - 4) * stepSize
                    drawLine(
                        color = guideColor,
                        start = Offset(guideX - guideWidth / 2f, gy),
                        end = Offset(guideX + guideWidth / 2f, gy),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }

            // 2. Dessin des 5 lignes principales de la portée de bord à bord (x: 0 -> width)
            val staffLinesSteps = listOf(0, 2, 4, 6, 8)
            staffLinesSteps.forEach { s ->
                val lineY = centerY + (s - 4) * stepSize
                drawLine(
                    color = inkColor,
                    start = Offset(0f, lineY),
                    end = Offset(width, lineY),
                    strokeWidth = lineThickness
                )
            }

            // 3. Dessin de la Clé (Sol / Fa) en typographie SMuFL Bravura
            val clefX = 20.dp.toPx()
            if (clef == Clef.TREBLE) {
                val solY = centerY + (6 - 4) * stepSize
                drawContext.canvas.nativeCanvas.drawText(
                    SMUFL_CLEF_TREBLE,
                    clefX,
                    solY,
                    musicPaint
                )
            } else {
                val faY = centerY + (2 - 4) * stepSize
                drawContext.canvas.nativeCanvas.drawText(
                    SMUFL_CLEF_BASS,
                    clefX,
                    faY,
                    musicPaint
                )
            }

            val effectiveDraftStep = activeHoverStep ?: draftStep

            // 4. Surlignage "sous le doigt" (hover highlight) si un step est sélectionné/survolé
            if (effectiveDraftStep != null) {
                val draftY = centerY + (effectiveDraftStep - 4) * stepSize
                val highlightHeight = spaceHeight * 0.95f
                val highlightWidth = spaceHeight * 3.5f
                drawRoundRect(
                    color = primaryHighlightColor.copy(alpha = 0.25f),
                    topLeft = Offset(width / 2f - highlightWidth / 2f, draftY - highlightHeight / 2f),
                    size = Size(highlightWidth, highlightHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            // 5. Dessin des notes réelles ou de la note en cours de survol/placement (draft)
            val renderNotes = if (notes.isNotEmpty()) notes else if (effectiveDraftStep != null) {
                val draftMidi = Notes.stepToMidi(effectiveDraftStep, clef) + draftAccidental
                val draftNote = Notes.fromMidi(draftMidi, preferSharps = draftAccidental >= 0)
                listOf(StaffNote(draftNote, color = primaryHighlightColor))
            } else emptyList()

            if (renderNotes.isNotEmpty()) {
                val getNoteX: (Int) -> Float = { index ->
                    when (renderNotes.size) {
                        1 -> width / 2f
                        2 -> if (index == 0) width / 2f - 24.dp.toPx() else width / 2f + 24.dp.toPx()
                        else -> {
                            val startX = 90.dp.toPx()
                            val availableWidth = width - startX - 28.dp.toPx()
                            val spacing = (availableWidth / (renderNotes.size - 1).coerceAtLeast(1)).coerceAtMost(65.dp.toPx())
                            startX + index * spacing
                        }
                    }
                }

                renderNotes.forEachIndexed { index, staffNote ->
                    val noteX = getNoteX(index)
                    val step = noteToStep(staffNote.note, clef)
                    val noteY = centerY + (step - 4) * stepSize

                    val notePaint = Paint(musicPaint).apply {
                        color = staffNote.color.toArgb()
                    }

                    // Lignes supplémentaires actives
                    val ledgerWidth = spaceHeight * 1.8f
                    val ledgerThickness = 2.dp.toPx()

                    if (step > 8) {
                        var lStep = 10
                        while (lStep <= step) {
                            val ly = centerY + (lStep - 4) * stepSize
                            drawLine(
                                color = staffNote.color,
                                start = Offset(noteX - ledgerWidth / 2f, ly),
                                end = Offset(noteX + ledgerWidth / 2f, ly),
                                strokeWidth = ledgerThickness
                            )
                            lStep += 2
                        }
                    } else if (step < 0) {
                        var lStep = -2
                        while (lStep >= step) {
                            val ly = centerY + (lStep - 4) * stepSize
                            drawLine(
                                color = staffNote.color,
                                start = Offset(noteX - ledgerWidth / 2f, ly),
                                end = Offset(noteX + ledgerWidth / 2f, ly),
                                strokeWidth = ledgerThickness
                            )
                            lStep -= 2
                        }
                    }

                    // Tête de note SMuFL
                    val bounds = Rect()
                    notePaint.getTextBounds(SMUFL_NOTEHEAD_BLACK, 0, 1, bounds)
                    val noteheadWidth = notePaint.measureText(SMUFL_NOTEHEAD_BLACK)

                    drawContext.canvas.nativeCanvas.drawText(
                        SMUFL_NOTEHEAD_BLACK,
                        noteX - noteheadWidth / 2f,
                        noteY,
                        notePaint
                    )

                    // Hampe (Stem)
                    val stemLength = spaceHeight * 3.5f
                    val stemThickness = 2.2.dp.toPx()

                    if (step >= 4) {
                        val stemX = noteX + noteheadWidth * 0.42f
                        drawLine(
                            color = staffNote.color,
                            start = Offset(stemX, noteY),
                            end = Offset(stemX, noteY - stemLength),
                            strokeWidth = stemThickness
                        )
                    } else {
                        val stemX = noteX - noteheadWidth * 0.42f
                        drawLine(
                            color = staffNote.color,
                            start = Offset(stemX, noteY),
                            end = Offset(stemX, noteY + stemLength),
                            strokeWidth = stemThickness
                        )
                    }

                    // Altération (♭, ♯, ♮)
                    if (staffNote.note.accidental != 0) {
                        val accGlyph = when (staffNote.note.accidental) {
                            1 -> SMUFL_ACCIDENTAL_SHARP
                            -1 -> SMUFL_ACCIDENTAL_FLAT
                            else -> SMUFL_ACCIDENTAL_NATURAL
                        }
                        val accWidth = notePaint.measureText(accGlyph)
                        drawContext.canvas.nativeCanvas.drawText(
                            accGlyph,
                            noteX - noteheadWidth / 2f - accWidth - 6.dp.toPx(),
                            noteY,
                            notePaint
                        )
                    }
                }
            }
        }
    }
}
