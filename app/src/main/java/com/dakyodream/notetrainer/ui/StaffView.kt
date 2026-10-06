@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package com.dakyodream.notetrainer.ui

import alphaTab.AlphaTabView
import alphaTab.Settings
import alphaTab.importer.AlphaTexImporter
import alphaTab.model.Color as AlphaColor
import alphaTab.rendering.RenderFinishedEventArgs
import alphaTab.rendering.ScoreRenderer
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Note
import com.dakyodream.notetrainer.core.Notes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class StaffNote(
    val note: Note,
    val color: Color = Color(0xFF1B1B1B)
)

data class StaffHitResult(val step: Int, val accidental: Int)

private const val RENDER_SCALE = 1.45
private var platformReady = false

private fun Note.texName(): String =
    letter.lowercaseChar().toString() + when (accidental) {
        1 -> "#"
        -1 -> "b"
        else -> ""
    } + octave

private fun texFor(clef: Clef, notes: List<StaffNote>): String {
    val clefName = if (clef == Clef.TREBLE) "treble" else "bass"
    val body = if (notes.isEmpty()) "r" else notes.joinToString(" ") { it.note.texName() }
    return "\\instrument 0 . \\clef $clefName $body"
}

private val scoreCache = android.util.LruCache<String, RenderOutput>(30)

private class RenderOutput(
    val bitmap: Bitmap?,
    val staffTopY: Float,
    val staffHeight: Float
)

private fun ensurePlatform(context: Context) {
    if (!platformReady) {
        try {
            AlphaTabView(context, null)
        } catch (e: Exception) {
            android.util.Log.e("NoteTrainer.Staff", "Failed to initialize alphaTab environment", e)
        }
        platformReady = true
    }
}

private fun renderScore(tex: String, widthPx: Int, argb: Int, density: Float): RenderOutput {
    val empty = RenderOutput(null, -1f, -1f)
    if (widthPx <= 0 || density <= 0) return empty
    val cacheKey = "$tex|$widthPx|$argb|$density"
    scoreCache.get(cacheKey)?.let { return it }
    return try {
        val out = renderScoreInternal(tex, widthPx, argb, density)
        if (out.bitmap != null) {
            scoreCache.put(cacheKey, out)
        }
        out
    } catch (e: Exception) {
        android.util.Log.e("NoteTrainer.Staff", "alphaTab render failed: ${e.message}", e)
        empty
    }
}

private fun renderScoreInternal(
    tex: String,
    widthPx: Int,
    argb: Int,
    density: Float
): RenderOutput {
    val empty = RenderOutput(null, -1f, -1f)
    val settings = Settings().apply {
        core.engine = "android"
        core.enableLazyLoading = false
        display.scale = RENDER_SCALE
        player.playerMode = alphaTab.PlayerMode.Disabled
        display.resources.mainGlyphColor = argb.toAlphaColor()
        display.resources.staffLineColor = argb.toAlphaColor()
        display.resources.scoreInfoColor = argb.toAlphaColor()
        display.resources.secondaryGlyphColor = AlphaColor(
            ((argb shr 16) and 0xFF).toDouble(),
            ((argb shr 8) and 0xFF).toDouble(),
            (argb and 0xFF).toDouble(),
            100.0
        )
    }
    val importer = AlphaTexImporter()
    importer.logErrors = true
    importer.initFromString(tex, settings)
    val score = importer.readScore()

    var result: RenderOutput = empty
    val renderer = ScoreRenderer(settings)
    val slices = ArrayList<RenderFinishedEventArgs>()
    renderer.partialRenderFinished.on { args ->
        if (args.renderResult is Bitmap) slices.add(args)
    }
    renderer.renderFinished.on { args ->
        val tw = (args.totalWidth * density).roundToInt()
        val th = (args.totalHeight * density).roundToInt()
        val composed: Bitmap? = if (tw > 0 && th > 0 && slices.isNotEmpty()) {
            val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            slices.forEach { s ->
                val b = s.renderResult as? Bitmap ?: return@forEach
                canvas.drawBitmap(
                    b,
                    (s.x * density).toFloat(),
                    (s.y * density).toFloat(),
                    null
                )
            }
            bmp
        } else {
            slices.firstOrNull()?.renderResult as? Bitmap
        }
        if (composed != null) {
            val lineAligned = renderer.boundsLookup
                ?.staffSystems
                ?.firstOrNull()
                ?.bars
                ?.firstOrNull()
                ?.lineAlignedBounds
            if (lineAligned != null && lineAligned.h > 0) {
                result = RenderOutput(
                    bitmap = composed,
                    staffTopY = (lineAligned.y * density).toFloat(),
                    staffHeight = (lineAligned.h * density).toFloat()
                )
            } else {
                result = RenderOutput(composed, -1f, -1f)
            }
        }
    }
    renderer.error.on { e ->
        android.util.Log.e("NoteTrainer.Staff", "alphaTab render error: ${e.message}", e)
    }
    renderer.width = (widthPx / density).toDouble()
    if (score.tracks.length > 0) {
        renderer.renderTracks(score.tracks)
    }
    return result
}

private fun Int.toAlphaColor(): AlphaColor = AlphaColor(
    ((this shr 16) and 0xFF).toDouble(),
    ((this shr 8) and 0xFF).toDouble(),
    (this and 0xFF).toDouble(),
    ((this shr 24) and 0xFF).toDouble()
)

@Composable
fun StaffView(
    clef: Clef,
    notes: List<StaffNote>,
    modifier: Modifier = Modifier,
    inkColor: Color = Color(0xFF1B1B1B),
    heightDp: Dp = 250.dp,
    onStaffTap: ((StaffHitResult) -> Unit)? = null
) {
    val argb = inkColor.toArgbCompat()
    val tex = remember(clef, notes) { texFor(clef, notes) }
    val context = LocalContext.current
    val density = LocalDensity.current.density
    remember(context) { ensurePlatform(context); true }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(heightDp)) {
        val widthPx = constraints.maxWidth
        var output by remember(tex) { mutableStateOf<RenderOutput?>(null) }

        LaunchedEffect(tex, widthPx, argb, density) {
            output = withContext(Dispatchers.Default) {
                renderScore(tex, widthPx, argb, density)
            }
        }

        output?.let { out ->
            out.bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightDp)
                        .let { m ->
                            if (onStaffTap != null) {
                                m.pointerInput(tex) {
                                    detectTapGestures { offset ->
                                        val step = hitToStep(out, bmp, offset.y, size.width.toFloat())
                                        if (step != null) {
                                            onStaffTap(StaffHitResult(step = step, accidental = 0))
                                        }
                                    }
                                }
                            } else m
                        },
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt()
)

private fun hitToStep(
    out: RenderOutput,
    bmp: Bitmap,
    tapY: Float,
    displayedWidth: Float
): Int? {
    if (out.staffTopY < 0 || out.staffHeight <= 0 || displayedWidth <= 0 || bmp.width <= 0) return null
    val scale = displayedWidth / bmp.width
    val yInBmp = tapY / scale
    val stepSize = out.staffHeight / 8f
    val step = ((yInBmp - out.staffTopY) / stepSize).roundToInt()
    return step.coerceIn(Notes.RANGE_MIN_STEP, Notes.RANGE_MAX_STEP)
}
