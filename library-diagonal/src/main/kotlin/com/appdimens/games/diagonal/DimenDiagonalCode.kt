package com.appdimens.games.diagonal

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Diagonal strategy — code-side extensions (`dgsdp`/`dghdp`/`dgwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Diagonal — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Diagonal: scaled dp by smallest width (b·diag/611.63). */
fun Float.dgsdp(context: Context?): Float =
    GameMath.calculateDiagonalDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.dgsdpa(context: Context?): Float =
    GameMath.calculateDiagonalDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.dgsdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateDiagonalDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.dgsdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateDiagonalDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.dgsdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateDiagonalDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.dghdp(context: Context?): Float =
    GameMath.calculateDiagonalDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.dghdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateDiagonalDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.dgwdp(context: Context?): Float =
    GameMath.calculateDiagonalDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.dgwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateDiagonalDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.dgsdp(context: Context?): Float = toFloat().dgsdp(context)
fun Int.dgsdpPx(context: Context?): Float = toFloat().dgsdpPx(context)
