@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package com.dakyodream.notetrainer.ui

import alphaTab.AlphaTabView
import alphaTab.PlayerMode
import alphaTab.model.Color as AlphaColor
import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dakyodream.notetrainer.core.Clef
import com.dakyodream.notetrainer.core.Note
import kotlin.math.roundToInt

/**
 * Portée rendue par alphaTab (https://alphatab.net), moteur de notation musicale
 * natif Kotlin/Android (police SMuFL Bravura, licence MPL-2.0).
 * - Rendu gravé professionnel via Skia (clés, têtes, queues, lignes supplémentaires) ;
 * - Géométrie du tap alignée sur les lignes réelles de la portée via boundsLookup
 *   (lineAlignedBounds.y = ligne du haut, h = 4 interlignes = 8 steps).
 * 1 step = demi-interligne, step 0 = ligne du haut (F5 clé de Sol / A3 clé de Fa).
 */
data class StaffNote(
    val note: Note,
    val color: Color = Color(0xFF1B1B1B)
)

data class StaffHitResult(val step: Int, val accidental: Int)

private fun Note.texName(): String =
    letter.lowercaseChar().toString() + when (accidental) {
        1 -> "#"
        -1 -> "b"
        else -> ""
    } + octave

private fun texFor(clef: Clef, notes: List<StaffNote>): String {
    val clefName = if (clef == Clef.TREBLE) "treble" else "bass"
    val body = if (notes.isEmpty()) "r" else notes.joinToString(" ") { it.note.texName() }
    // instrument 25 (guitare, défaut) afficherait une tablature ; 0 = piano -> portée seule
    return "\\instrument 0 \\clef $clefName . $body"
}

/** Conteneur qui intercepte les taps (les ScrollViews internes d'alphaTab ne les consomment pas). */
@SuppressLint("ClickableViewAccessibility")
private class TapInterceptor(context: Context) : FrameLayout(context) {
    var onTap: ((Float, Float) -> Unit)? = null
    var lastTex: String? = null
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_UP && ev.eventTime - ev.downTime < 600L) {
            onTap?.invoke(ev.x, ev.y)
        }
        return true
    }
}

@Composable
fun StaffView(
    clef: Clef,
    notes: List<StaffNote>,
    modifier: Modifier = Modifier,
    inkColor: Color = Color(0xFF1B1B1B),
    onStaffTap: ((StaffHitResult) -> Unit)? = null
) {
    val argb = inkColor.toArgb()
    val backgroundArgb = Color(0x00000000).toArgb()
    val tex = remember(clef, notes) { texFor(clef, notes) }
    val context = LocalContext.current
    val tapInterceptor = remember { TapInterceptor(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    } }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(230.dp),
        factory = { ctx ->
            tapInterceptor.onTap = null
            val alphaTabView = AlphaTabView(ctx, null).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.player.playerMode = PlayerMode.Disabled
                settings.player.enableCursor = false
                settings.player.enableUserInteraction = false
                // moteur "android" = Canvas Android pur, sans lib native (libalphaskiajni.so
                // n'est pas alignée 16 KB : chargement bloqué sur appareils Android 15+ en pages 16 KB)
                settings.core.engine = "android"
                settings.display.scale = 1.3
            }
            addChildOnce(tapInterceptor, alphaTabView)
            tapInterceptor
        },
        update = { container ->
            val view = container.getChildAt(0) as? AlphaTabView ?: return@AndroidView
            view.setBackgroundColor(backgroundArgb)
            view.settings.display.resources.mainGlyphColor = argb.toAlphaColor()
            view.settings.display.resources.staffLineColor = argb.toAlphaColor()
            view.settings.display.resources.scoreInfoColor = argb.toAlphaColor()
            view.settings.display.resources.secondaryGlyphColor = AlphaColor(
                ((argb shr 16) and 0xFF).toDouble(),
                ((argb shr 8) and 0xFF).toDouble(),
                (argb and 0xFF).toDouble(),
                100.0
            )
            view.api.updateSettings()
            if (tapInterceptor.lastTex != tex) {
                tapInterceptor.lastTex = tex
                view.api.tex(tex)
            }
            tapInterceptor.onTap = onStaffTap?.let { callback ->
                { x: Float, y: Float ->
                    val step = hitToStep(view, x, y)
                    if (step != null) callback(StaffHitResult(step = step, accidental = 0))
                }
            }
        }
    )
}

private fun addChildOnce(container: TapInterceptor, child: AlphaTabView) {
    if (container.childCount == 0) container.addView(child)
}

private fun Int.toAlphaColor(): AlphaColor = AlphaColor(
    ((this shr 16) and 0xFF).toDouble(),
    ((this shr 8) and 0xFF).toDouble(),
    (this and 0xFF).toDouble(),
    ((this shr 24) and 0xFF).toDouble()
)

/** Recherche le ScrollView vertical interne d'alphaTab (contenu plus haut que la vue). */
private fun findVerticalScroll(view: android.view.ViewGroup): android.widget.ScrollView? {
    for (i in 0 until view.childCount) {
        val child = view.getChildAt(i)
        if (child is android.widget.ScrollView) return child
        if (child is android.view.ViewGroup) {
            findVerticalScroll(child)?.let { return it }
        }
    }
    return null
}

/**
 * Convertit un tap (pixels de la vue) en step diatonique :
 * coordonnées logiques = pixels / highDpiFactor, puis alignement sur les
 * lignes de la portée rendue (boundsLookup). step 0 = ligne du haut.
 */
private fun hitToStep(view: AlphaTabView, x: Float, y: Float): Int? {
    val dpi = view.context.resources.displayMetrics.density.toDouble()
    if (dpi <= 0) return null
    val scrollY = findVerticalScroll(view)?.scrollY ?: 0
    val ly = (y + scrollY) / dpi
    val lookup = view.api.boundsLookup ?: return null
    val line = lookup.staffSystems.firstOrNull()?.bars?.firstOrNull()?.lineAlignedBounds ?: return null
    val stepHeight = line.h / 8.0
    if (stepHeight <= 0.0) return null
    val rawStep = ((ly - line.y) / stepHeight).roundToInt()
    if (rawStep < -14 || rawStep > 22) return null
    return rawStep
}
