package com.appdimens.games.power

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Power strategy — code-side extensions (`pwsdp`/`pwhdp`/`pwwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Power — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Power: scaled dp by smallest width (b·(sw/300)^0.75). */
fun Float.pwsdp(context: Context?): Float =
    GameMath.calculatePowerDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.pwsdpa(context: Context?): Float =
    GameMath.calculatePowerDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.pwsdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculatePowerDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.pwsdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculatePowerDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.pwsdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePowerDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.pwhdp(context: Context?): Float =
    GameMath.calculatePowerDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.pwhdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePowerDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.pwwdp(context: Context?): Float =
    GameMath.calculatePowerDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.pwwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculatePowerDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.pwsdp(context: Context?): Float = toFloat().pwsdp(context)
fun Int.pwsdpPx(context: Context?): Float = toFloat().pwsdpPx(context)
