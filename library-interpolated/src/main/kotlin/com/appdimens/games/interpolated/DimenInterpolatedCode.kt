package com.appdimens.games.interpolated

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Interpolated strategy — code-side extensions (`isdp`/`ihdp`/`iwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Interpolated — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Interpolated: scaled dp by smallest width (midpoint base↔linear). */
fun Float.isdp(context: Context?): Float =
    GameMath.calculateInterpolatedDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.isdpa(context: Context?): Float =
    GameMath.calculateInterpolatedDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.isdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateInterpolatedDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.isdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateInterpolatedDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.isdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateInterpolatedDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.ihdp(context: Context?): Float =
    GameMath.calculateInterpolatedDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.ihdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateInterpolatedDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.iwdp(context: Context?): Float =
    GameMath.calculateInterpolatedDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.iwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateInterpolatedDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.isdp(context: Context?): Float = toFloat().isdp(context)
fun Int.isdpPx(context: Context?): Float = toFloat().isdpPx(context)
