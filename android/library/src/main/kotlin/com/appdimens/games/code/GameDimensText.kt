package com.appdimens.games.code

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen

/**
 * [EN] Scaled text extensions — family parity with `ssp/hsp/wsp` (respect system
 * font scale) and `sem/hem/wem` (fixed). Code-side returns **px**, computed as
 * single-multiply chains over precomputed snapshot factors (zero boxing/alloc).
 *
 * [PT] Extensões de texto escalado — paridade com a família: `ssp/hsp/wsp`
 * (respeitam a escala de fonte do sistema) e `sem/hem/wem` (fixos). O code retorna
 * **px** como cadeias de multiplicação única sobre fatores pré-computados.
 */

// ─── ssp family (SMALL_WIDTH anchored, respects fontScale) ─────────────────

/** sp scaled by smallest width; px includes fontScale (visual follows system setting). */
fun Int.ssp(context: Context?): Float {
    val m = GameScreen.metrics()
    return toFloat() * m.scale * m.density * m.fontScale
}

fun Int.sspa(context: Context?): Float {
    val m = GameScreen.metrics()
    return toFloat() * m.defaultScaledAspectRatioMultiplier * m.density * m.fontScale
}

/** `i` — invariant under resized windows / multi-window. */
fun Int.sspi(context: Context?): Float {
    val m = GameScreen.invariantMetrics()
    val v = if (!m.isFullscreen) toFloat() else toFloat() * m.scale
    return v * m.density * m.fontScale
}

fun Int.sspia(context: Context?): Float {
    val m = GameScreen.invariantMetrics()
    val v = if (!m.isFullscreen) toFloat() else toFloat() * m.defaultScaledAspectRatioMultiplier
    return v * m.density * m.fontScale
}

// ─── hsp / wsp (axis variants) ─────────────────────────────────────────────

fun Int.hsp(context: Context?): Float = spAxis(this, GameScreen.metrics().let { it to DpQualifier.HEIGHT })
fun Int.wsp(context: Context?): Float = spAxis(this, GameScreen.metrics().let { it to DpQualifier.WIDTH })

private inline fun spAxis(value: Int, pair: Pair<GameMetrics, DpQualifier>): Float =
    value * pair.first.axisDp(pair.second) * com.appdimens.games.core.GameScreenConstants.INV_BASE_RATIO *
        pair.first.density * pair.first.fontScale

// ─── sem family (FIXED — independent of system font scale) ────────────────

fun Int.sem(context: Context?): Float {
    val m = GameScreen.metrics()
    return toFloat() * m.scale * m.density
}
fun Int.hem(context: Context?): Float = fixedSp(this, DpQualifier.HEIGHT)
fun Int.wem(context: Context?): Float = fixedSp(this, DpQualifier.WIDTH)

private inline fun fixedSp(value: Int, qualifier: DpQualifier): Float {
    val m = GameScreen.metrics()
    return value * m.axisDp(qualifier) *
        com.appdimens.games.core.GameScreenConstants.INV_BASE_RATIO * m.density
}

// ─── Float receivers (game-loop friendly, no conversion overhead) ──────────

fun Float.ssp(context: Context?): Float {
    val m = GameScreen.metrics()
    return this * m.scale * m.density * m.fontScale
}
