/**
 * Author & Developer: Jean Bodenberg
 * GIT: https://github.com/bodenberg/appdimens-games.git
 *
 * Library: AppDimens — Custom scaling strategies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdimens.games.compose.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdimens.games.compose.LocalDimenMetrics
import com.appdimens.games.custom.ScalingStrategies
import com.appdimens.games.custom.ScalingStrategy
import com.appdimens.games.custom.resolve

// EN Compose API. Reactive by design: values read [LocalDimenMetrics], so any window resize
// recomposes. Each strategy memoizes its resolution per snapshot (one multiply on hit).
// PT API Compose. Reativa: lê [LocalDimenMetrics]; cada estratégia memoiza por snapshot.

/**
 * EN Strategy provided to a subtree by [ProvideScalingStrategy]; `null` means
 * [ScalingStrategies.default].
 * PT Estratégia fornecida a uma subárvore; `null` significa [ScalingStrategies.default].
 */
val LocalScalingStrategy = compositionLocalOf<ScalingStrategy?> { null }

/** EN Every `16.cdp` without an explicit strategy inside [content] uses [strategy]. */
@Composable
fun ProvideScalingStrategy(strategy: ScalingStrategy, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalScalingStrategy provides strategy, content = content)
}

/** EN Strategy in effect at this point of the composition. PT Estratégia em vigor aqui. */
val currentScalingStrategy: ScalingStrategy
    @Composable @ReadOnlyComposable get() = LocalScalingStrategy.current ?: ScalingStrategies.default

/** EN Scales this dp value with [strategy] → [Dp]. `16.cdp(Hud)`. */
@Composable
@ReadOnlyComposable
fun Number.cdp(strategy: ScalingStrategy): Dp = strategy.resolve(LocalDimenMetrics.current).scale(toFloat()).dp

@get:Composable
@get:ReadOnlyComposable
val Number.cdp: Dp get() = cdp(currentScalingStrategy)

/** EN Resize-invariant (`i`): base value when the surface is not fullscreen. */
@Composable
@ReadOnlyComposable
fun Number.cdpi(strategy: ScalingStrategy): Dp {
    val m = LocalDimenMetrics.current
    return (if (!m.isFullscreen) toFloat() else strategy.resolve(m).scale(toFloat())).dp
}

@get:Composable
@get:ReadOnlyComposable
val Number.cdpi: Dp get() = cdpi(currentScalingStrategy)

/** EN Scaled value in **px**. */
@Composable
@ReadOnlyComposable
fun Number.cdpPx(strategy: ScalingStrategy): Float {
    val m = LocalDimenMetrics.current
    return strategy.resolve(m).scale(toFloat()) * m.density
}

@get:Composable
@get:ReadOnlyComposable
val Number.cdpPx: Float get() = cdpPx(currentScalingStrategy)

/**
 * EN Text size → [TextUnit] in sp units (Compose applies the system font scale; the strategy
 * policy `follow` / `ignore` / `clamp` is pre-compensated). `14.csp(Hud)`.
 * PT Tamanho de texto em sp (a política de font scale é pré-compensada).
 */
@Composable
@ReadOnlyComposable
fun Number.csp(strategy: ScalingStrategy): TextUnit = strategy.resolve(LocalDimenMetrics.current).scaleSpUnits(toFloat()).sp

@get:Composable
@get:ReadOnlyComposable
val Number.csp: TextUnit get() = csp(currentScalingStrategy)

/** EN Text size in **px** (policy applied). */
@Composable
@ReadOnlyComposable
fun Number.cspPx(strategy: ScalingStrategy): Float {
    val m = LocalDimenMetrics.current
    return strategy.resolve(m).scaleSpDp(toFloat()) * m.density
}

@get:Composable
@get:ReadOnlyComposable
val Number.cspPx: Float get() = cspPx(currentScalingStrategy)

// EN Inline formulas ───────────────────────────────────────────────────────────
// PT Fórmulas inline ───────────────────────────────────────────────────────────

/**
 * EN Scales with a text formula — the formula **is** the value: `16.cdp("base * pow(sw / 360, 0.8)")`.
 * Shared per text (see [com.appdimens.games.custom.scalingFormula]); write `base * (…)` for the
 * one-multiply fast path.
 * PT Escala com uma fórmula em texto — a fórmula **é** o valor.
 */
@Composable
@ReadOnlyComposable
fun Number.cdp(formula: String): Dp = cdp(ScalingStrategies.formula(formula))

/** EN `16.cdpi("…")` — resize-invariant. */
@Composable
@ReadOnlyComposable
fun Number.cdpi(formula: String): Dp = cdpi(ScalingStrategies.formula(formula))

/** EN `16.cdpPx("base * (sw / 360)")` → px. */
@Composable
@ReadOnlyComposable
fun Number.cdpPx(formula: String): Float = cdpPx(ScalingStrategies.formula(formula))

/** EN `14.csp("base * min(sw / 360, 1.3)")` → [TextUnit] (font-scale policy applied). */
@Composable
@ReadOnlyComposable
fun Number.csp(formula: String): TextUnit = csp(ScalingStrategies.formula(formula))

/** EN `14.cspPx("…")` → px. */
@Composable
@ReadOnlyComposable
fun Number.cspPx(formula: String): Float = cspPx(ScalingStrategies.formula(formula))

/** EN `Hud.dp(16)`. */
@Composable
@ReadOnlyComposable
fun ScalingStrategy.dp(value: Number): Dp = value.cdp(this)

/** EN `Hud.sp(14)`. */
@Composable
@ReadOnlyComposable
fun ScalingStrategy.sp(value: Number): TextUnit = value.csp(this)

/** EN `Hud.px(16)`. */
@Composable
@ReadOnlyComposable
fun ScalingStrategy.px(value: Number): Float = value.cdpPx(this)

/** EN `16.dp.scaledBy(Hud)`. */
@Composable
@ReadOnlyComposable
fun Dp.scaledBy(strategy: ScalingStrategy = currentScalingStrategy): Dp = value.cdp(strategy)

/** EN Factor for the current snapshot (debug overlays). PT Fator para o snapshot atual. */
@Composable
@ReadOnlyComposable
fun ScalingStrategy.currentFactor(): Float = resolve(LocalDimenMetrics.current).factor

/** EN Human-readable breakdown for the current snapshot. */
@Composable
@ReadOnlyComposable
fun ScalingStrategy.explainCurrent(): String = resolve(LocalDimenMetrics.current).explain(id)
