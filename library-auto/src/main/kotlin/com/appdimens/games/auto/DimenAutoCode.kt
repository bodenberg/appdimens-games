package com.appdimens.games.auto

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Auto strategy — code-side extensions (`asdp`/`ahdp`/`awdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Auto — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Auto: scaled dp by smallest width (linear ≤480 then log). */
fun Float.asdp(context: Context?): Float =
    GameMath.calculateAutoDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.asdpa(context: Context?): Float =
    GameMath.calculateAutoDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.asdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateAutoDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.asdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateAutoDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.asdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateAutoDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.ahdp(context: Context?): Float =
    GameMath.calculateAutoDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.ahdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateAutoDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.awdp(context: Context?): Float =
    GameMath.calculateAutoDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.awdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateAutoDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.asdp(context: Context?): Float = toFloat().asdp(context)
fun Int.asdpPx(context: Context?): Float = toFloat().asdpPx(context)
