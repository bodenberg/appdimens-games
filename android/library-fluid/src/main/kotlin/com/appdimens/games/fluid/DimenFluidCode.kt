package com.appdimens.games.fluid

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Fluid strategy — code-side extensions (`fsdp`/`fhdp`/`fwdp` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia Fluid — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** Fluid: scaled dp by smallest width (lerp band 320–768). */
fun Float.fsdp(context: Context?): Float =
    GameMath.calculateFluidDp(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.fsdpa(context: Context?): Float =
    GameMath.calculateFluidDp(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.fsdpi(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateFluidDp(this, m)
}

/** Invariant + AR (`ia`). */
fun Float.fsdpia(context: Context?): Float {
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.calculateFluidDp(this, m, applyAspectRatio = true)
}

/** Pixels. */
fun Float.fsdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFluidDp(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.fhdp(context: Context?): Float =
    GameMath.calculateFluidDp(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.fhdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFluidDp(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.fwdp(context: Context?): Float =
    GameMath.calculateFluidDp(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.fwdpPx(context: Context?): Float =
    GameMath.toPx(GameMath.calculateFluidDp(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.fsdp(context: Context?): Float = toFloat().fsdp(context)
fun Int.fsdpPx(context: Context?): Float = toFloat().fsdpPx(context)
