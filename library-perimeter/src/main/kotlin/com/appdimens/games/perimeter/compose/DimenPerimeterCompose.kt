package com.appdimens.games.perimeter.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.LocalDimenMetrics
import com.appdimens.games.math.GameMath

/**
 * [EN] Perimeter strategy — Compose extensions. Reactive by design: any window resize
 * recomputes via [LocalDimenMetrics]. Suffixes: `a` (aspect ratio), `i` (resize-invariant).
 *
 * [PT] Estratégia Perimeter — extensões Compose. Reativas por definição: qualquer
 * redimensionamento recomputa via [LocalDimenMetrics].
 */

@Composable
private fun compute(b: Float, inv: Boolean, ar: Boolean, q: DpQualifier = DpQualifier.SMALL_WIDTH): Float {
    val m = LocalDimenMetrics.current
    if (inv && !m.isFullscreen) return b
    val v = GameMath.calculatePerimeterDp(b, m, applyAspectRatio = ar, qualifier = q)
    return v
}

/** Perimeter scaled dp. */
@get:Composable
val Float.prsdp: Dp get() = compute(this, false, false).dp

@get:Composable
val Float.prsdpa: Dp get() = compute(this, false, true).dp

@get:Composable
val Float.prsdpi: Dp get() = compute(this, true, false).dp

@get:Composable
val Float.prsdppx: Float get() = compute(this, false, false) * LocalDimenMetrics.current.density

@get:Composable
val Float.prhdp: Dp get() = compute(this, false, false, DpQualifier.HEIGHT).dp

@get:Composable
val Float.prwdp: Dp get() = compute(this, false, false, DpQualifier.WIDTH).dp

@get:Composable
val Int.prsdp: Dp get() = toFloat().prsdp
