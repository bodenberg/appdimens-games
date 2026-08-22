package com.appdimens.games.auto.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.LocalGameMetrics
import com.appdimens.games.math.GameMath

/**
 * [EN] Auto strategy — Compose extensions. Reactive by design: any window resize
 * recomposes via [LocalGameMetrics]. Suffixes: `a` (aspect ratio), `i` (resize-invariant).
 *
 * [PT] Estratégia Auto — extensões Compose. Reativas por definição: qualquer
 * redimensionamento recomputa via [LocalGameMetrics].
 */

@Composable
private fun compute(b: Float, inv: Boolean, ar: Boolean, q: DpQualifier = DpQualifier.SMALL_WIDTH): Float {
    val m = LocalGameMetrics.current
    if (inv && !m.isFullscreen) return b
    val v = GameMath.calculateAutoDp(b, m, applyAspectRatio = ar, qualifier = q)
    return v
}

/** Auto scaled dp. */
@get:Composable
val Float.asdp: Dp get() = compute(this, false, false).dp

@get:Composable
val Float.asdpa: Dp get() = compute(this, false, true).dp

@get:Composable
val Float.asdpi: Dp get() = compute(this, true, false).dp

@get:Composable
val Float.asdppx: Float get() = compute(this, false, false) * LocalGameMetrics.current.density

@get:Composable
val Float.ahdp: Dp get() = compute(this, false, false, DpQualifier.HEIGHT).dp

@get:Composable
val Float.awdp: Dp get() = compute(this, false, false, DpQualifier.WIDTH).dp

@get:Composable
val Int.asdp: Dp get() = toFloat().asdp
