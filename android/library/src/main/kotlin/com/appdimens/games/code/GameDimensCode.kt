package com.appdimens.games.code

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Inverter
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.core.GameScreenConstants as C
import com.appdimens.games.math.GameMath

/**
 * [EN] Scaled dimension extensions — **identical usage to `appdimens-dynamic` / `-kmp`**.
 * Stems `sdp/hdp/wdp` with suffixes `a` (aspect-ratio refinement), `i`
 * (`ignoreMultiWindows`: invariant under resized windows / multi-window),
 * `ia` (both). Code-side results are **pixels** (family convention); Compose
 * counterparts return [androidx.compose.ui.unit.Dp].
 *
 * [PT] Extensões de dimensão escalada — **uso idêntico ao appdimens-dynamic/-kmp**.
 * Stems `sdp/hdp/wdp` com sufixos `a`, `i` e `ia`. No lado code o resultado é em
 * **pixels** (convenção da família); no Compose retorna-se Dp.
 */

@PublishedApi
internal fun resolveMetrics(ignoreMultiWindows: Boolean): GameMetrics =
    if (ignoreMultiWindows) GameScreen.invariantMetrics() else GameScreen.metrics()

@PublishedApi
internal fun isConstrained(metrics: GameMetrics): Boolean =
    !metrics.isFullscreen || metrics.minDimensionDp <= 0f

private fun Float.pxOf(m: GameMetrics): Float = this * m.density

// ─── sdp family (SMALL_WIDTH anchored — rotation-invariant) ────────────────

/** [EN] Scaled by smallest width, in px. [PT] Escala pela menor largura, em px. */
fun Int.sdp(context: Context?): Float {
    val m = resolveMetrics(false)
    return (toFloat() * m.scale).pxOf(m)
}

fun Int.sdpa(context: Context?): Float {
    val m = resolveMetrics(false)
    return (toFloat() * m.defaultScaledAspectRatioMultiplier).pxOf(m)
}

/** `i` — ignores multi-window/resized-window adjustments (invariant). */
fun Int.sdpi(context: Context?): Float {
    val m = resolveMetrics(true)
    val v = if (isConstrained(m)) toFloat() else toFloat() * m.scale
    return v.pxOf(m)
}

fun Int.sdpia(context: Context?): Float {
    val m = resolveMetrics(true)
    val v = if (isConstrained(m)) toFloat() else toFloat() * m.defaultScaledAspectRatioMultiplier
    return v.pxOf(m)
}

// ─── hdp family (HEIGHT qualifier) ─────────────────────────────────────────

fun Int.hdp(context: Context?): Float {
    val m = resolveMetrics(false)
    return (toFloat() * m.screenHeightFactor).pxOf(m)
}
fun Int.hdpa(context: Context?): Float {
    val m = resolveMetrics(false)
    return (toFloat() * m.screenHeightFactor * m.defaultAspectRatioMultiplier).pxOf(m)
}
fun Int.hdpi(context: Context?): Float {
    val m = resolveMetrics(true)
    val v = if (isConstrained(m)) toFloat() else toFloat() * m.screenHeightFactor
    return v.pxOf(m)
}
fun Int.hdpia(context: Context?): Float {
    val m = resolveMetrics(true)
    val v = if (isConstrained(m)) toFloat()
    else toFloat() * m.screenHeightFactor * m.defaultAspectRatioMultiplier
    return v.pxOf(m)
}

// ─── wdp family (WIDTH qualifier) ──────────────────────────────────────────

fun Int.wdp(context: Context?): Float {
    val m = resolveMetrics(false)
    return (toFloat() * m.screenWidthFactor).pxOf(m)
}
fun Int.wdpa(context: Context?): Float {
    val m = resolveMetrics(false)
    return (toFloat() * m.screenWidthFactor * m.defaultAspectRatioMultiplier).pxOf(m)
}
fun Int.wdpi(context: Context?): Float {
    val m = resolveMetrics(true)
    val v = if (isConstrained(m)) toFloat() else toFloat() * m.screenWidthFactor
    return v.pxOf(m)
}
fun Int.wdpia(context: Context?): Float {
    val m = resolveMetrics(true)
    val v = if (isConstrained(m)) toFloat()
    else toFloat() * m.screenWidthFactor * m.defaultAspectRatioMultiplier
    return v.pxOf(m)
}

// ─── Inverters (family naming: sdpPh/Lh/Pw/Lw · hdpLw/hdpPw · wdpLh/wdpPh) ──

/** SW→PH in portrait. */
fun Int.sdpPh(context: Context?, ignoreMultiWindows: Boolean = false): Float =
    inverted(context, Inverter.SW_TO_PH, ignoreMultiWindows)

/** SW→LH in landscape. */
fun Int.sdpLh(context: Context?, ignoreMultiWindows: Boolean = false): Float =
    inverted(context, Inverter.SW_TO_LH, ignoreMultiWindows)

/** SW→PW in portrait. */
fun Int.sdpPw(context: Context?, ignoreMultiWindows: Boolean = false): Float =
    inverted(context, Inverter.SW_TO_PW, ignoreMultiWindows)

/** SW→LW in landscape. */
fun Int.sdpLw(context: Context?, ignoreMultiWindows: Boolean = false): Float =
    inverted(context, Inverter.SW_TO_LW, ignoreMultiWindows)

/** PH→LW (HEIGHT behaves as WIDTH in landscape). */
fun Int.hdpLw(context: Context?, ignoreMultiWindows: Boolean = false): Float =
    inverted(context, Inverter.PH_TO_LW, ignoreMultiWindows, DpQualifier.HEIGHT)

/** LH→PW (landscape HEIGHT behaves as portrait WIDTH). */
fun Int.hdpPw(context: Context?, ignoreMultiWindows: Boolean = false): Float =
    inverted(context, Inverter.LH_TO_PW, ignoreMultiWindows, DpQualifier.HEIGHT)

/** PW→LH (WIDTH behaves as HEIGHT in landscape). */
fun Int.wdpLh(context: Context?, ignoreMultiWindows: Boolean = false): Float =
    inverted(context, Inverter.PW_TO_LH, ignoreMultiWindows, DpQualifier.WIDTH)

/** LW→PH (landscape WIDTH behaves as portrait HEIGHT). */
fun Int.wdpPh(context: Context?, ignoreMultiWindows: Boolean = false): Float =
    inverted(context, Inverter.LW_TO_PH, ignoreMultiWindows, DpQualifier.WIDTH)

private fun Number.inverted(
    @Suppress("UNUSED_PARAMETER") context: Context?,
    inverter: Inverter,
    ignoreMultiWindows: Boolean,
    qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
): Float {
    val m = resolveMetrics(ignoreMultiWindows)
    if (ignoreMultiWindows && isConstrained(m)) return toFloat().pxOf(m)
    return GameMath.toPx(GameMath.calculateScaledDp(toFloat(), m, qualifier, inverter), m)
}

// ─── Escape hatches (family signatures) ────────────────────────────────────

/**
 * [EN] Full-control resolver returning **dp** — mirrors `toDynamicScaledDp`.
 * [PT] Resolver de controle total retornando **dp** — espelha `toDynamicScaledDp`.
 */
fun Number.toDynamicScaledDp(
    context: Context?,
    qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
    inverter: Inverter = Inverter.DEFAULT,
    ignoreMultiWindows: Boolean = false,
    applyAspectRatio: Boolean = false,
    customSensitivityK: Float? = null,
): Float {
    val m = resolveMetrics(ignoreMultiWindows)
    if (ignoreMultiWindows && isConstrained(m)) return toFloat()
    return GameMath.calculateScaledDp(toFloat(), m, qualifier, inverter, applyAspectRatio, customSensitivityK)
}

/** Pixel variant of [toDynamicScaledDp]. */
fun Number.toDynamicScaledPx(
    context: Context?,
    qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
    inverter: Inverter = Inverter.DEFAULT,
    ignoreMultiWindows: Boolean = false,
    applyAspectRatio: Boolean = false,
    customSensitivityK: Float? = null,
): Float = toDynamicScaledDp(
    context, qualifier, inverter, ignoreMultiWindows, applyAspectRatio, customSensitivityK
) * resolveMetrics(ignoreMultiWindows).density
