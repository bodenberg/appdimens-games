package com.appdimens.games.power.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.LocalGameMetrics
import com.appdimens.games.math.GameMath

/**
 * [EN] Power strategy — Compose extensions. Reactive by design: any window resize
 * recomposes via [LocalGameMetrics]. Suffixes: `a` (aspect ratio), `i` (resize-invariant).
 *
 * [PT] Estratégia Power — extensões Compose. Reativas por definição: qualquer
 * redimensionamento recomputa via [LocalGameMetrics].
 */

@Composable
private fun compute(b: Float, inv: Boolean, ar: Boolean, q: DpQualifier = DpQualifier.SMALL_WIDTH): Float {
    val m = LocalGameMetrics.current
    if (inv && !m.isFullscreen) return b
    val v = GameMath.calculatePowerDp(b, m, applyAspectRatio = ar, qualifier = q)
    return v
}

/** Power scaled dp. */
@get:Composable
val Float.pwsdp: Dp get() = compute(this, false, false).dp

@get:Composable
val Float.pwsdpa: Dp get() = compute(this, false, true).dp

@get:Composable
val Float.pwsdpi: Dp get() = compute(this, true, false).dp

@get:Composable
val Float.pwsdppx: Float get() = compute(this, false, false) * LocalGameMetrics.current.density

@get:Composable
val Float.pwhdp: Dp get() = compute(this, false, false, DpQualifier.HEIGHT).dp

@get:Composable
val Float.pwwdp: Dp get() = compute(this, false, false, DpQualifier.WIDTH).dp

@get:Composable
val Int.pwsdp: Dp get() = toFloat().pwsdp
