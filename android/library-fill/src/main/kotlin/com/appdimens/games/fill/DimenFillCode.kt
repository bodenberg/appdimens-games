package com.appdimens.games.fill

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Fill strategy — code-side extensions (`flsdp`/`flhdp`/`flwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Fill — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Fill: scaled dp by smallest width (cover max-ratio). */
fun Float.flsdp(context: Context?): Float =
    GameMath.calculateFillDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.flsdpa(context: Context?): Float =
    GameMath.calculateFillDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.flsdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateFillDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.flsdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateFillDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.flsdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFillDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.flhdp(context: Context?): Float =
    GameMath.calculateFillDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.flhdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFillDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.flwdp(context: Context?): Float =
    GameMath.calculateFillDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.flwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFillDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.flsdp(context: Context?): Float = toFloat().flsdp(context)
fun Int.flsdpPx(context: Context?): Float = toFloat().flsdpPx(context)
