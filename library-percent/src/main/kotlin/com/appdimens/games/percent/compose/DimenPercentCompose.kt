package com.appdimens.games.percent.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.LocalGameMetrics
import com.appdimens.games.math.GameMath

/**
 * [EN] Percent strategy — Compose extensions. Reactive by design: any window resize
 * recomposes via [LocalGameMetrics]. Suffixes: `a` (aspect ratio), `i` (resize-invariant).
 *
 * [PT] Estratégia Percent — extensões Compose. Reativas por definição: qualquer
 * redimensionamento recomputa via [LocalGameMetrics].
 */

@Composable
private fun compute(b: Float, inv: Boolean, ar: Boolean, q: DpQualifier = DpQualifier.SMALL_WIDTH): Float {
    val m = LocalGameMetrics.current
    if (inv && !m.isFullscreen) return b
    val v = GameMath.calculatePercentDp(b, m, applyAspectRatio = ar, qualifier = q)
    return v
}

/** Percent scaled dp. */
@get:Composable
val Float.psdp: Dp get() = compute(this, false, false).dp

@get:Composable
val Float.psdpa: Dp get() = compute(this, false, true).dp

@get:Composable
val Float.psdpi: Dp get() = compute(this, true, false).dp

@get:Composable
val Float.psdppx: Float get() = compute(this, false, false) * LocalGameMetrics.current.density

@get:Composable
val Float.phdp: Dp get() = compute(this, false, false, DpQualifier.HEIGHT).dp

@get:Composable
val Float.pwdp: Dp get() = compute(this, false, false, DpQualifier.WIDTH).dp

@get:Composable
val Int.psdp: Dp get() = toFloat().psdp
