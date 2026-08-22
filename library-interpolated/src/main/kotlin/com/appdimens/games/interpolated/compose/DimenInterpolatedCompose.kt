package com.appdimens.games.interpolated.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.LocalDimenMetrics
import com.appdimens.games.math.GameMath

/**
 * [EN] Interpolated strategy — Compose extensions. Reactive by design: any window resize
 * recomputes via [LocalDimenMetrics]. Suffixes: `a` (aspect ratio), `i` (resize-invariant).
 *
 * [PT] Estratégia Interpolated — extensões Compose. Reativas por definição: qualquer
 * redimensionamento recomputa via [LocalDimenMetrics].
 */

@Composable
private fun compute(b: Float, inv: Boolean, ar: Boolean, q: DpQualifier = DpQualifier.SMALL_WIDTH): Float {
    val m = LocalDimenMetrics.current
    if (inv && !m.isFullscreen) return b
    val v = GameMath.calculateInterpolatedDp(b, m, applyAspectRatio = ar, qualifier = q)
    return v
}

/** Interpolated scaled dp. */
@get:Composable
val Float.isdp: Dp get() = compute(this, false, false).dp

@get:Composable
val Float.isdpa: Dp get() = compute(this, false, true).dp

@get:Composable
val Float.isdpi: Dp get() = compute(this, true, false).dp

@get:Composable
val Float.isdppx: Float get() = compute(this, false, false) * LocalDimenMetrics.current.density

@get:Composable
val Float.ihdp: Dp get() = compute(this, false, false, DpQualifier.HEIGHT).dp

@get:Composable
val Float.iwdp: Dp get() = compute(this, false, false, DpQualifier.WIDTH).dp

@get:Composable
val Int.isdp: Dp get() = toFloat().isdp
