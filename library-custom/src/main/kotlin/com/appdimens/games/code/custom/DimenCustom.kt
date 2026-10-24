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
package com.appdimens.games.code.custom

import android.content.Context
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.custom.ScalingStrategies
import com.appdimens.games.custom.ScalingStrategy
import com.appdimens.games.custom.resolve

// EN Engine / Kotlin code API. Like the core `code` package, short names return **px** and read
// the live [GameScreen] snapshot, so values follow every window resize. `i` variants anchor to the
// frozen fullscreen reference (base value under true multi-window). `context` is accepted for
// family parity and is not required.
// PT API para engines / código Kotlin. Como no pacote `code` do core, os nomes curtos retornam
// **px** e leem o snapshot vivo do [GameScreen]. Variantes `i` ancoram na referência fullscreen.

/** EN Scaled with [strategy] for [metrics] → **dp**. PT Escalado com [strategy] para [metrics] → **dp**. */
fun Number.cdpDp(metrics: GameMetrics, strategy: ScalingStrategy = ScalingStrategies.default): Float =
    strategy.resolve(metrics).scale(toFloat())

/** EN Scaled with [strategy] → **dp**, live snapshot. PT Escalado → **dp**, snapshot vivo. */
@JvmOverloads
fun Number.cdpDp(context: Context? = null, strategy: ScalingStrategy = ScalingStrategies.default): Float =
    cdpDp(GameScreen.metrics(), strategy)

/** EN `16.cdpDp(Brand)`. */
fun Number.cdpDp(strategy: ScalingStrategy): Float = cdpDp(GameScreen.metrics(), strategy)

/** EN Scaled with [strategy] → **px**, live snapshot. `16.cdp(context, Brand)`. PT → **px**. */
@JvmOverloads
fun Number.cdp(context: Context? = null, strategy: ScalingStrategy = ScalingStrategies.default): Float {
    val m = GameScreen.metrics()
    return strategy.resolve(m).scale(toFloat()) * m.density
}

/** EN `16.cdp(Brand)` → **px**. */
fun Number.cdp(strategy: ScalingStrategy): Float = cdp(null, strategy)

/**
 * EN Resize-invariant (`i`) → **px**: frozen fullscreen reference; base value under multi-window.
 * PT Invariante ao redimensionamento (`i`) → **px**.
 */
@JvmOverloads
fun Number.cdpi(context: Context? = null, strategy: ScalingStrategy = ScalingStrategies.default): Float {
    val m = GameScreen.invariantMetrics()
    val dp = if (!m.isFullscreen) toFloat() else strategy.resolve(m).scale(toFloat())
    return dp * m.density
}

/** EN `16.cdpi(Brand)`. */
fun Number.cdpi(strategy: ScalingStrategy): Float = cdpi(null, strategy)

/**
 * EN Text size → **px**, applying the strategy font-scale policy (`follow` / `ignore` / `clamp`).
 * PT Tamanho de texto → **px**, aplicando a política de font scale da estratégia.
 */
@JvmOverloads
fun Number.csp(context: Context? = null, strategy: ScalingStrategy = ScalingStrategies.default): Float {
    val m = GameScreen.metrics()
    return strategy.resolve(m).scaleSpDp(toFloat()) * m.density
}

/** EN `14.csp(Brand)` → **px**. */
fun Number.csp(strategy: ScalingStrategy): Float = csp(null, strategy)

/** EN Text size → **dp** (font-scale policy applied). PT Texto → **dp**. */
fun Number.cspDp(strategy: ScalingStrategy = ScalingStrategies.default): Float =
    strategy.resolve(GameScreen.metrics()).scaleSpDp(toFloat())

// EN Inline formulas — the formula **is** the value. Shared per text (see `scalingFormula`).
// PT Fórmulas inline — a fórmula **é** o valor. Compartilhadas por texto.

/** EN `16.cdpDp(metrics, "base * pow(sw / 360, 0.8)")` → **dp**. */
fun Number.cdpDp(metrics: GameMetrics, formula: String): Float = cdpDp(metrics, ScalingStrategies.formula(formula))

/** EN `16.cdpDp("base * pow(sw / 360, 0.8)")` → **dp**, live snapshot. */
fun Number.cdpDp(formula: String): Float = cdpDp(GameScreen.metrics(), ScalingStrategies.formula(formula))

/** EN `16.cdp("base * pow(sw / 360, 0.8)")` → **px**, live snapshot. */
fun Number.cdp(formula: String): Float = cdp(null, ScalingStrategies.formula(formula))

/** EN `16.cdpi("…")` → **px**, resize-invariant. */
fun Number.cdpi(formula: String): Float = cdpi(null, ScalingStrategies.formula(formula))

/** EN `14.csp("base * min(sw / 360, 1.3)")` → **px** for text (font-scale policy applied). */
fun Number.csp(formula: String): Float = csp(null, ScalingStrategies.formula(formula))

/** EN `14.cspDp("…")` → **dp** for text. */
fun Number.cspDp(formula: String): Float = cspDp(ScalingStrategies.formula(formula))

// EN Strategy-first facilitators. PT Facilitadores a partir da estratégia.

/** EN `Hud.px(16)` → px. */
fun ScalingStrategy.px(value: Number): Float = value.cdp(null, this)

/** EN `Hud.dp(16)` → dp. */
fun ScalingStrategy.dp(value: Number): Float = value.cdpDp(GameScreen.metrics(), this)

/** EN `Hud.spPx(14)` → px for text. */
fun ScalingStrategy.spPx(value: Number): Float = value.csp(null, this)

/** EN Current factor (live snapshot). PT Fator atual (snapshot vivo). */
fun ScalingStrategy.factor(): Float = resolve(GameScreen.metrics()).factor

/** EN Debug breakdown for the live snapshot. PT Explicação para o snapshot vivo. */
fun ScalingStrategy.explain(): String = resolve(GameScreen.metrics()).explain(id)

/**
 * EN Reads a strategies JSON from `assets/` and registers it (see `ScalingStrategies.loadJson`).
 * PT Lê um JSON de estratégias de `assets/` e registra.
 */
fun ScalingStrategies.loadAsset(context: Context, path: String): Map<String, ScalingStrategy> =
    loadJson(context.assets.open(path).bufferedReader().use { it.readText() })

/**
 * EN Java / engine-friendly facade. `DimenCustom.cdp(16f, hud)`.
 * PT Fachada amigável para Java / engines.
 */
object DimenCustom {
    @JvmStatic @JvmOverloads
    fun cdp(value: Float, strategy: ScalingStrategy = ScalingStrategies.default): Float = value.cdp(null, strategy)

    @JvmStatic @JvmOverloads
    fun cdpDp(value: Float, strategy: ScalingStrategy = ScalingStrategies.default): Float = value.cdpDp(GameScreen.metrics(), strategy)

    @JvmStatic @JvmOverloads
    fun cdpi(value: Float, strategy: ScalingStrategy = ScalingStrategies.default): Float = value.cdpi(null, strategy)

    @JvmStatic @JvmOverloads
    fun csp(value: Float, strategy: ScalingStrategy = ScalingStrategies.default): Float = value.csp(null, strategy)

    /** EN `DimenCustom.cdp(16f, DimenCustom.formula("base * (sw / 360)"))`. Shared per text. */
    @JvmStatic
    fun formula(formula: String): ScalingStrategy = ScalingStrategies.formula(formula)

    /** EN `DimenCustom.cdp(16f, "base * pow(sw / 360, 0.8)")` → px. */
    @JvmStatic
    fun cdp(value: Float, formula: String): Float = value.cdp(formula)

    @JvmStatic
    fun cdpDp(value: Float, formula: String): Float = value.cdpDp(formula)

    @JvmStatic
    fun cdpi(value: Float, formula: String): Float = value.cdpi(formula)

    @JvmStatic
    fun csp(value: Float, formula: String): Float = value.csp(formula)

    @JvmStatic
    fun factor(strategy: ScalingStrategy): Float = strategy.factor()

    @JvmStatic
    fun strategy(id: String): ScalingStrategy = ScalingStrategies.getOrThrow(id)

    @JvmStatic
    fun loadJson(json: String): Map<String, ScalingStrategy> = ScalingStrategies.loadJson(json)

    @JvmStatic
    fun loadAsset(context: Context, path: String): Map<String, ScalingStrategy> = ScalingStrategies.loadAsset(context, path)
}
