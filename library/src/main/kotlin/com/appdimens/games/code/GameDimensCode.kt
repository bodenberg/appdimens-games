package com.appdimens.games.code

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Inverter
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Code-side (non-Compose) scaled dimension extensions for Kotlin & Java games.
 * Family-identical stems: `sdp/hdp/wdp` (+`a` aspect-ratio, `i` resize-invariant, `ia`),
 * `Px` variants, inverters and facilitators. All read the live [GameScreen] snapshot,
 * so values auto-adjust on window resize; `i` variants stay anchored to the frozen
 * fullscreen reference.
 *
 * [PT] Extensões de dimensão escalada para jogos Kotlin/Java (fora do Compose).
 * Stems idênticos à família: `sdp/hdp/wdp` (+`a` proporção, `i` invariante a resize, `ia`),
 * variantes `Px`, inversores e facilitadores. Todas leem o snapshot vivo do [GameScreen],
 * então os valores se ajustam automaticamente no redimensionamento; as variantes `i`
 * permanecem ancoradas na referência fullscreen congelada.
 *
 * ```kotlin
 * val button = 48.sdp(context)      // auto-adjusts on resize
 * val hud    = 48.sdpi(context)     // invariant under split-screen / resized window
 * ```
 */

// ─── Core resolvers ────────────────────────────────────────────────────────

/** Resolves with live metrics unless [ignoreResize] anchors to the frozen fullscreen snapshot. */
@PublishedApi
internal fun resolveMetrics(ignoreResize: Boolean): GameMetrics =
    if (ignoreResize) GameScreen.invariantMetrics() else GameScreen.metrics()

@PublishedApi
internal fun isConstrained(metrics: GameMetrics): Boolean =
    !metrics.isFullscreen || metrics.minDimensionDp <= 0f

// ─── sdp family (SMALL_WIDTH anchored — rotation-invariant) ────────────────

/** [EN] Scaled dp by smallest width. [PT] Dp escalado pela menor largura. */
fun Int.sdp(context: Context?): Float = toFloat() * resolveMetrics(false).scale

/** Aspect-ratio aware SCALED. */
fun Int.sdpa(context: Context?): Float = toFloat() * resolveMetrics(false).defaultScaledAspectRatioMultiplier

/** Resize-invariant SCALED (`i`). */
fun Int.sdpi(context: Context?): Float {
    val m = resolveMetrics(true)
    return if (isConstrained(m)) toFloat() else toFloat() * m.scale
}

/** Invariant + aspect ratio (`ia`). */
fun Int.sdpia(context: Context?): Float {
    val m = resolveMetrics(true)
    return if (isConstrained(m)) toFloat() else toFloat() * m.defaultScaledAspectRatioMultiplier
}

/** [EN] Pixel result of [sdp]. [PT] Resultado em pixels de [sdp]. */
fun Int.sdpPx(context: Context?): Float {
    val m = resolveMetrics(false)
    return toFloat() * m.scale * m.density
}

fun Int.sdpaPx(context: Context?): Float {
    val m = resolveMetrics(false)
    return toFloat() * m.defaultScaledAspectRatioMultiplier * m.density
}

fun Int.sdpiPx(context: Context?): Float {
    val m = resolveMetrics(true)
    return if (isConstrained(m)) toFloat() * m.density else toFloat() * m.scale * m.density
}

// ─── hdp family (HEIGHT qualifier) ─────────────────────────────────────────

/** Scaled dp by current height. */
fun Int.hdp(context: Context?): Float = toFloat() * resolveMetrics(false).screenHeightFactor

fun Int.hdpa(context: Context?): Float =
    toFloat() * resolveMetrics(false).screenHeightFactor * resolveMetrics(false).defaultAspectRatioMultiplier

fun Int.hdpi(context: Context?): Float {
    val m = resolveMetrics(true)
    return if (isConstrained(m)) toFloat() else toFloat() * m.screenHeightFactor
}

fun Int.hdpia(context: Context?): Float {
    val m = resolveMetrics(true)
    return if (isConstrained(m)) toFloat()
    else toFloat() * m.screenHeightFactor * m.defaultAspectRatioMultiplier
}

fun Int.hdpPx(context: Context?): Float = hdp(context) * resolveMetrics(false).density
fun Int.hdpiPx(context: Context?): Float {
    val m = resolveMetrics(true)
    return (if (isConstrained(m)) toFloat() else toFloat() * m.screenHeightFactor) * m.density
}

// ─── wdp family (WIDTH qualifier) ──────────────────────────────────────────

/** Scaled dp by current width. */
fun Int.wdp(context: Context?): Float = toFloat() * resolveMetrics(false).screenWidthFactor

fun Int.wdpa(context: Context?): Float =
    toFloat() * resolveMetrics(false).screenWidthFactor * resolveMetrics(false).defaultAspectRatioMultiplier

fun Int.wdpi(context: Context?): Float {
    val m = resolveMetrics(true)
    return if (isConstrained(m)) toFloat() else toFloat() * m.screenWidthFactor
}

fun Int.wdpia(context: Context?): Float {
    val m = resolveMetrics(true)
    return if (isConstrained(m)) toFloat()
    else toFloat() * m.screenWidthFactor * m.defaultAspectRatioMultiplier
}

fun Int.wdpPx(context: Context?): Float = wdp(context) * resolveMetrics(false).density
fun Int.wdpiPx(context: Context?): Float {
    val m = resolveMetrics(true)
    return (if (isConstrained(m)) toFloat() else toFloat() * m.screenWidthFactor) * m.density
}

// ─── Inverters (family parity) ─────────────────────────────────────────────

/**
 * [EN] SW behaves as WIDTH in landscape (`SW_TO_LW`) — landscape-first HUD.
 * [PT] SW comporta-se como largura em paisagem — HUD landscape-first.
 */
fun Int.sdpLw(context: Context?, ignoreResize: Boolean = false): Float =
    invertScaled(this, context, Inverter.SW_TO_LW, ignoreResize)

/** HEIGHT behaves as WIDTH in landscape (`PH_TO_LW`). */
fun Int.hdpLw(context: Context?, ignoreResize: Boolean = false): Float =
    invertScaled(this, context, Inverter.PH_TO_LW, ignoreResize)

/** SMALL_WIDTH behaves as HEIGHT in portrait (`SW_TO_PH`). */
fun Int.sdpPh(context: Context?, ignoreResize: Boolean = false): Float =
    invertScaled(this, context, Inverter.SW_TO_PH, ignoreResize)

private fun Number.invertScaled(
    receiver: Number, @Suppress("UNUSED_PARAMETER") ctx: Context?,
    inverter: Inverter, ignoreResize: Boolean,
): Float {
    val m = resolveMetrics(ignoreResize)
    if (ignoreResize && isConstrained(m)) return toFloat()
    return GameMath.calculateScaledDp(receiver.toFloat(), m, DpQualifier.SMALL_WIDTH, inverter)
}

// ─── Generic escape hatch ──────────────────────────────────────────────────

/**
 * [EN] Full-control resolver (mirrors Dynamic's `toDynamicScaledDp`).
 * [PT] Resolver de controle total (espelha o `toDynamicScaledDp` do Dynamic).
 */
fun Number.toGameScaledDp(
    context: Context?,
    qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
    inverter: Inverter = Inverter.DEFAULT,
    ignoreResize: Boolean = false,
    applyAspectRatio: Boolean = false,
    customSensitivityK: Float? = null,
): Float {
    val m = resolveMetrics(ignoreResize)
    if (ignoreResize && isConstrained(m)) return toFloat()
    return GameMath.calculateScaledDp(
        toFloat(), m, qualifier, inverter, applyAspectRatio, customSensitivityK
    )
}

/** Pixel version of [toGameScaledDp]. */
fun Number.toGameScaledPx(
    context: Context?,
    qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
    inverter: Inverter = Inverter.DEFAULT,
    ignoreResize: Boolean = false,
    applyAspectRatio: Boolean = false,
    customSensitivityK: Float? = null,
): Float = toGameScaledDp(context, qualifier, inverter, ignoreResize, applyAspectRatio, customSensitivityK)
    .let { dp -> dp * resolveMetrics(ignoreResize).density }

// ─── Float receivers (game-loop friendly, no boxing) ───────────────────────

/** Float receiver variants — zero boxing in hot loops. */
fun Float.sdpG(context: Context?): Float = this * resolveMetrics(false).scale
fun Float.sdpaG(context: Context?): Float = this * resolveMetrics(false).defaultScaledAspectRatioMultiplier
fun Float.sdpiG(context: Context?): Float {
    val m = resolveMetrics(true)
    return if (isConstrained(m)) this else this * m.scale
}
fun Float.hdpG(context: Context?): Float = this * resolveMetrics(false).screenHeightFactor
fun Float.wdpG(context: Context?): Float = this * resolveMetrics(false).screenWidthFactor
fun Float.sdpGPx(context: Context?): Float { val m = resolveMetrics(false); return this * m.scale * m.density }
