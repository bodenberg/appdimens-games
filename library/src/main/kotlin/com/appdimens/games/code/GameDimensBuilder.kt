package com.appdimens.games.code

import android.content.Context
import com.appdimens.games.common.GameElementType
import com.appdimens.games.common.GameScalingStrategy
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Fluent builder — migration-compatible with the 2.0.1 `smart()` DSL.
 * [PT] Builder fluente — compatível com a DSL `smart()` da 2.0.1.
 *
 * ```kotlin
 * val player = 64f.smart().forElement(GameElementType.PLAYER).dp
 * val text   = 16f.smart().withStrategy(GameScalingStrategy.FLUID)
 *                     .withFluid(12f, 24f).dp
 * ```
 */
class GameDimensBuilder @JvmOverloads constructor(
    private val baseValue: Float,
    private var strategy: GameScalingStrategy? = null,
    private var elementType: GameElementType? = null,
) {
    private var fluidMin = Float.NaN
    private var fluidMax = Float.NaN
    private var fluidMinW = 320f
    private var fluidMaxW = 768f
    private var powerExponent = 0.75f
    private var applyAspectRatio = false
    private var ignoreResize = false
    private var constraintsMin = Float.NaN
    private var constraintsMax = Float.NaN
    private var metricsOverride: GameMetrics? = null

    /** Auto-infers the strategy from the element type. / Infere a estratégia pelo tipo de elemento. */
    fun forElement(type: GameElementType) = apply { elementType = type }

    fun withStrategy(strategy: GameScalingStrategy) = apply { this.strategy = strategy }
    fun withFluid(minValue: Float, maxValue: Float, minWidth: Float = 320f, maxWidth: Float = 768f) = apply {
        strategy = GameScalingStrategy.FLUID
        fluidMin = minValue; fluidMax = maxValue; fluidMinW = minWidth; fluidMaxW = maxWidth
    }
    fun withPower(exponent: Float) = apply { strategy = GameScalingStrategy.POWER; powerExponent = exponent }
    fun aspectRatio(enabled: Boolean) = apply { applyAspectRatio = enabled }
    fun invariant() = apply { ignoreResize = true }
    fun range(minValue: Float, maxValue: Float) = apply { constraintsMin = minValue; constraintsMax = maxValue }
    fun metrics(metrics: GameMetrics) = apply { metricsOverride = metrics }

    /** Terminal: scaled value in dp. / Terminal: valor escalado em dp. */
    val dp: Float get() = build()

    /** Terminal: scaled value in px. */
    val px: Float get() = build() * (metricsOverride ?: GameScreen.metrics()).density

    private fun build(): Float {
        val m = metricsOverride ?: if (ignoreResize) GameScreen.invariantMetrics() else GameScreen.metrics()
        val effective = strategy ?: elementType?.recommended ?: GameScalingStrategy.BALANCED
        var out = when (effective) {
            GameScalingStrategy.DEFAULT -> baseValue * m.defaultScaledAspectRatioMultiplier
            GameScalingStrategy.PERCENTAGE -> GameMath.calculatePercentDp(baseValue, m)
            GameScalingStrategy.BALANCED -> GameMath.calculateAutoDp(baseValue, m)
            GameScalingStrategy.LOGARITHMIC -> GameMath.calculateLogarithmicDp(baseValue, m, applyAspectRatio = applyAspectRatio)
            GameScalingStrategy.POWER -> GameMath.calculatePowerDp(baseValue, m, powerExponent, applyAspectRatio)
            GameScalingStrategy.FLUID ->
                GameMath.calculateFluidDp(
                    baseValue, m,
                    if (fluidMin.isNaN()) baseValue * 0.8f else fluidMin,
                    if (fluidMax.isNaN()) baseValue * 1.2f else fluidMax,
                    fluidMinW, fluidMaxW, applyAspectRatio
                )
            GameScalingStrategy.INTERPOLATED -> GameMath.calculateInterpolatedDp(baseValue, m, applyAspectRatio = applyAspectRatio)
            GameScalingStrategy.DIAGONAL -> GameMath.calculateDiagonalDp(baseValue, m, applyAspectRatio)
            GameScalingStrategy.PERIMETER -> GameMath.calculatePerimeterDp(baseValue, m, applyAspectRatio)
            GameScalingStrategy.FIT -> GameMath.calculateFitDp(baseValue, m, applyAspectRatio)
            GameScalingStrategy.FILL -> GameMath.calculateFillDp(baseValue, m, applyAspectRatio)
            GameScalingStrategy.AUTOSIZE, GameScalingStrategy.NONE -> baseValue
        }
        if (!constraintsMin.isNaN()) out = maxOf(out, constraintsMin)
        if (!constraintsMax.isNaN()) out = minOf(out, constraintsMax)
        return out
    }
}

/** [EN] Entry of the fluent DSL. [PT] Entrada da DSL fluente. */
fun Float.smart(): GameDimensBuilder = GameDimensBuilder(this)

/** Int overload. */
fun Int.smart(): GameDimensBuilder = GameDimensBuilder(toFloat())

/**
 * [EN] Java-friendly static facade over the builder.
 * [PT] Fachada estática amigável a Java sobre o builder.
 *
 * ```java
 * float player = AppDimensGamesJava.playerSize(64f);
 * float button = AppDimensGamesJava.hud(48f, context);
 * ```
 */
object AppDimensGamesJava {
    @JvmStatic @JvmOverloads fun hud(sizeDp: Float, context: Context? = null): Float =
        GameDimensBuilder(sizeDp, GameScalingStrategy.DEFAULT).dp
    @JvmStatic @JvmOverloads fun playerSize(sizeDp: Float, context: Context? = null): Float =
        GameDimensBuilder(sizeDp, GameScalingStrategy.BALANCED).dp
    @JvmStatic @JvmOverloads fun enemySize(sizeDp: Float, context: Context? = null): Float =
        GameDimensBuilder(sizeDp, GameScalingStrategy.BALANCED).dp
    @JvmStatic @JvmOverloads fun worldSize(sizeDp: Float, context: Context? = null): Float =
        GameDimensBuilder(sizeDp, GameScalingStrategy.PERCENTAGE).dp
    @JvmStatic @JvmOverloads fun background(sizeDp: Float, context: Context? = null): Float =
        GameDimensBuilder(sizeDp, GameScalingStrategy.FILL).dp
    @JvmStatic @JvmOverloads fun viewportContent(sizeDp: Float, context: Context? = null): Float =
        GameDimensBuilder(sizeDp, GameScalingStrategy.FIT).dp
    @JvmStatic @JvmOverloads fun touchTarget(sizeDp: Float, context: Context? = null): Float =
        GameDimensBuilder(sizeDp, GameScalingStrategy.DIAGONAL).dp
}
