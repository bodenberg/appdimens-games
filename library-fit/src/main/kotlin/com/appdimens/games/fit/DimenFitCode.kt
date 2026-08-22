package com.appdimens.games.fit

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Fit strategy — code-side extensions (`ftsdp`/`fthdp`/`ftwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Fit — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Fit: scaled dp by smallest width (letterbox min-ratio). */
fun Float.ftsdp(context: Context?): Float =
    GameMath.calculateFitDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.ftsdpa(context: Context?): Float =
    GameMath.calculateFitDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.ftsdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateFitDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.ftsdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateFitDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.ftsdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFitDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.fthdp(context: Context?): Float =
    GameMath.calculateFitDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.fthdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFitDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.ftwdp(context: Context?): Float =
    GameMath.calculateFitDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.ftwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFitDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.ftsdp(context: Context?): Float = toFloat().ftsdp(context)
fun Int.ftsdpPx(context: Context?): Float = toFloat().ftsdpPx(context)
