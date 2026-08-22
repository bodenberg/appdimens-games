package com.appdimens.games.logarithmic

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Logarithmic strategy — code-side extensions (`logsdp`/`loghdp`/`logwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Logarithmic — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Logarithmic: scaled dp by smallest width (Weber-Fechner). */
fun Float.logsdp(context: Context?): Float =
    GameMath.calculateLogarithmicDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.logsdpa(context: Context?): Float =
    GameMath.calculateLogarithmicDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.logsdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateLogarithmicDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.logsdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateLogarithmicDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.logsdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateLogarithmicDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.loghdp(context: Context?): Float =
    GameMath.calculateLogarithmicDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.loghdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateLogarithmicDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.logwdp(context: Context?): Float =
    GameMath.calculateLogarithmicDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.logwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateLogarithmicDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.logsdp(context: Context?): Float = toFloat().logsdp(context)
fun Int.logsdpPx(context: Context?): Float = toFloat().logsdpPx(context)
