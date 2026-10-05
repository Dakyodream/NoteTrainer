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
import androidx.compose.ui.unit.dp
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Portée rendue par alphaTab (https://alphatab.net) en mode headless :
 * ScoreRenderer produit des Bitmaps Android (moteur "android", Canvas pur,
 * sans lib native), assemblées et affichées dans un Image() Compose.
 *
 * IMPORTANT : la plateforme alphaTab (police Bravura + Environment.highDpiFactor)
 * n'est initialisée que par AlphaTabView.init -> on instancie une vue hors-écran
 * une seule fois par contexte avant tout rendu, sans ça beginRender produit une
 * bitmap vide (rectangle noir).
 *
 * Géométrie du tap : renderer.boundsLookup (staffSystems[0].bars[0].lineAlignedBounds),
 * y = ligne du haut (step 0), h = 4 interlignes = 8 steps (1 step = demi-interligne).
 */
data class StaffNote(
    val note: Note,
    val color: Color = Color(0xFF1B1B1B)
)

data class StaffHitResult(val step: Int, val accidental: Int)

private const val RENDER_SCALE = 1.3
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
    // En 1.6.1 : \instrument = staff meta (AVANT le '.'), \clef = bar meta
    // (APRES le '.') — les inverser déclenche "Error on block metaDataTags".
    // instrument 0 (piano) : la piste guitare par défaut afficherait une tablature.
    return "\\instrument 0 . \\clef $clefName $body"
}

private val scoreCache = android.util.LruCache<String, RenderOutput>(30)

private fun ensurePlatform(context: Context) {
    if (!platformReady) {
        // Déclenche AndroidEnvironment.initializeAndroid : charge Bravura.otf
        // dans AndroidCanvas.MusicFont et règle Environment.highDpiFactor.
        // initializeAndroid appelle aussi AlphaSkiaAndroid.initialize()
        // (System.loadLibrary("alphaskiajni")) : si la lib est absente/refusée
        // (pages 16 KB), on tolère l'échec — Bravura et la densité sont déjà
        // initialisés à ce stade et le moteur "android" n'utilise PAS alphaSkia
        // (seul le moteur "skia" en dépend).
        try {
            AlphaTabView(context, null)
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.w("NoteTrainer.Staff", "alphaSkia indisponible (${e.message}), le moteur 'android' continue sans elle")
        } catch (e: Exception) {
            android.util.Log.e("NoteTrainer.Staff", "init alphaTab: ${e.message}", e)
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

private fun renderScoreInternal(tex: String, widthPx: Int, argb: Int, density: Float): RenderOutput {
    // Passe 1 : rendu à RENDER_SCALE pour mesurer la largeur naturelle du contenu
    // (une seule mesure ne remplit pas renderer.width : le système s'arrête au contenu).
    val first = renderOnce(tex, widthPx, argb, density, RENDER_SCALE)
    val measured = first.contentWidthUnits
    if (measured <= 0) return first
    // Passe 2 : scale ajusté pour que le contenu occupe toute la largeur disponible
    // (rendu net, pas d'agrandissement bitmap). On garde une petite marge à droite.
    val logicalWidth = widthPx / density
    val targetScale = (logicalWidth / measured) * RENDER_SCALE
    val second = renderOnce(tex, widthPx, argb, density, targetScale)
    return if (second.bitmap != null) second else first
}

private class RenderOutput(
    val bitmap: Bitmap?,
    val staffTopY: Float,
    val staffHeight: Float,
    val contentWidthUnits: Double = 0.0
)

private fun renderOnce(
    tex: String,
    widthPx: Int,
    argb: Int,
    density: Float,
    scale: Double
): RenderOutput {
    val empty = RenderOutput(null, -1f, -1f, 0.0)
    val settings = Settings().apply {
        core.engine = "android"
        // Sans ceci, les tranches sont "lazy" et ne sont jamais rendues hors
        // AlphaTabView (renderLazyPartial n'est appelé par personne) -> bitmap vide.
        core.enableLazyLoading = false
        display.scale = scale
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
        // registerPartial a déjà multiplié x/y/totalWidth/totalHeight par display.scale ;
        // beginRender (AndroidCanvas) multiplie ensuite par highDpiFactor (= density).
        val tw = (args.totalWidth * density).roundToInt()
        val th = (args.totalHeight * density).roundToInt()
        // Largeur réelle du contenu (unités layout x scale), padding droit exclu :
        // sert à calculer le scale de la passe 2 qui remplit toute la largeur.
        val contentRightUnits = renderer.boundsLookup
            ?.staffSystems
            ?.maxOfOrNull { sys -> sys.bars.maxOfOrNull { it.realBounds.x + it.realBounds.w } ?: 0.0 }
            ?: 0.0
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
            // boundsLookup.finish(scale) est appelé avant renderFinished :
            // bounds en unités canvas-logiques (layout x scale) -> x density = pixels bitmap.
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
                    staffHeight = (lineAligned.h * density).toFloat(),
                    contentWidthUnits = contentRightUnits
                )
            } else {
                result = RenderOutput(composed, -1f, -1f, contentRightUnits)
            }
        }
    }
    renderer.error.on { e ->
        android.util.Log.e("NoteTrainer.Staff", "alphaTab render error: ${e.message}", e)
    }
    // largeur logique (unités layout) : pixels / density, comme AndroidUiFacade
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
    onStaffTap: ((StaffHitResult) -> Unit)? = null
) {
    val argb = inkColor.toArgbCompat()
    val tex = remember(clef, notes) { texFor(clef, notes) }
    val context = LocalContext.current
    val density = LocalDensity.current.density
    remember(context) { ensurePlatform(context); true }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(230.dp)) {
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
                        .height(230.dp)
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

/**
 * Tap -> step : la bitmap recadrée est affichée avec FillWidth (échelle uniforme
 * s = displayedWidth / bmpWidth). L'Image est aligné TopStart : si la bitmap
 * agrandie est plus haute que le conteneur, elle est rognée par le bas (le haut
 * reste aligné, pas d'offset vertical à compenser).
 * staffTopY/staffHeight sont déjà en pixels bitmap.
 * step 0 = ligne du haut, la hauteur couvre 4 interlignes = 8 steps.
 */
private fun hitToStep(
    out: RenderOutput,
    bmp: Bitmap,
    tapY: Float,
    displayedWidth: Float
): Int? {
    if (out.staffTopY < 0 || out.staffHeight <= 0 || displayedWidth <= 0) return null
    val s = displayedWidth / bmp.width
    val yInBmp = tapY / s
    val stepSize = out.staffHeight / 8f
    return ((yInBmp - out.staffTopY) / stepSize).roundToInt()
}
