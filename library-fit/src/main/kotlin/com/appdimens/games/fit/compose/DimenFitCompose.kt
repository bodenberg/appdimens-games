package com.appdimens.games.fit.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.LocalDimenMetrics
import com.appdimens.games.math.GameMath

/**
 * [EN] Fit strategy — Compose extensions. Reactive by design: any window resize
 * recomputes via [LocalDimenMetrics]. Suffixes: `a` (aspect ratio), `i` (resize-invariant).
 *
 * [PT] Estratégia Fit — extensões Compose. Reativas por definição: qualquer
 * redimensionamento recomputa via [LocalDimenMetrics].
 */

@Composable
private fun compute(b: Float, inv: Boolean, ar: Boolean, q: DpQualifier = DpQualifier.SMALL_WIDTH): Float {
    val m = LocalDimenMetrics.current
    if (inv && !m.isFullscreen) return b
    val v = GameMath.calculateFitDp(b, m, applyAspectRatio = ar, qualifier = q)
    return v
}

/** Fit scaled dp. */
@get:Composable
val Float.ftsdp: Dp get() = compute(this, false, false).dp

@get:Composable
val Float.ftsdpa: Dp get() = compute(this, false, true).dp

@get:Composable
val Float.ftsdpi: Dp get() = compute(this, true, false).dp

@get:Composable
val Float.ftsdppx: Float get() = compute(this, false, false) * LocalDimenMetrics.current.density

@get:Composable
val Float.fthdp: Dp get() = compute(this, false, false, DpQualifier.HEIGHT).dp

@get:Composable
val Float.ftwdp: Dp get() = compute(this, false, false, DpQualifier.WIDTH).dp

@get:Composable
val Int.ftsdp: Dp get() = toFloat().ftsdp
