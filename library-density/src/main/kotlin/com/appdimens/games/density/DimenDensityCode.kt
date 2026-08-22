package com.appdimens.games.density

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Density strategy — code-side extensions (`dsdp`/`dhdp`/`dwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Density — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Density: scaled dp by smallest width (b·dpi/160). */
fun Float.dsdp(context: Context?): Float =
    GameMath.calculateDensityDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.dsdpa(context: Context?): Float =
    GameMath.calculateDensityDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.dsdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateDensityDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.dsdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateDensityDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.dsdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateDensityDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.dhdp(context: Context?): Float =
    GameMath.calculateDensityDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.dhdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateDensityDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.dwdp(context: Context?): Float =
    GameMath.calculateDensityDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.dwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateDensityDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.dsdp(context: Context?): Float = toFloat().dsdp(context)
fun Int.dsdpPx(context: Context?): Float = toFloat().dsdpPx(context)
