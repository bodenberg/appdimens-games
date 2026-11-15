package com.appdimens.games.percent

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Percent strategy — code-side extensions (`psdp`/`phdp`/`pwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Percent — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Percent: scaled dp by smallest width (b·(d/300)). */
fun Float.psdp(context: Context?): Float =
    GameMath.calculatePercentDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.psdpa(context: Context?): Float =
    GameMath.calculatePercentDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.psdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculatePercentDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.psdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculatePercentDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.psdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePercentDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.phdp(context: Context?): Float =
    GameMath.calculatePercentDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.phdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePercentDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.pwdp(context: Context?): Float =
    GameMath.calculatePercentDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.pwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePercentDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.psdp(context: Context?): Float = toFloat().psdp(context)
fun Int.psdpPx(context: Context?): Float = toFloat().psdpPx(context)
