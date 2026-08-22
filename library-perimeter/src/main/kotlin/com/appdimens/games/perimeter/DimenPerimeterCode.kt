package com.appdimens.games.perimeter

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Perimeter strategy — code-side extensions (`prsdp`/`prhdp`/`prwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Perimeter — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Perimeter: scaled dp by smallest width (b·(min+max)/833). */
fun Float.prsdp(context: Context?): Float =
    GameMath.calculatePerimeterDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.prsdpa(context: Context?): Float =
    GameMath.calculatePerimeterDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.prsdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculatePerimeterDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.prsdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculatePerimeterDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.prsdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePerimeterDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.prhdp(context: Context?): Float =
    GameMath.calculatePerimeterDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.prhdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePerimeterDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.prwdp(context: Context?): Float =
    GameMath.calculatePerimeterDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.prwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePerimeterDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.prsdp(context: Context?): Float = toFloat().prsdp(context)
fun Int.prsdpPx(context: Context?): Float = toFloat().prsdpPx(context)
