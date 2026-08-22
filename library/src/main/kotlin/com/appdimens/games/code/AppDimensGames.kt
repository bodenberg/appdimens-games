package com.appdimens.games.code

import android.content.Context
import com.appdimens.games.common.GameElementType
import com.appdimens.games.common.GameScalingStrategy
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.core.GameCache
import com.appdimens.games.math.GameMath

/**
 * [EN] Unified game gateway (migration-compatible with `AppDimensGames` 2.0.1).
 * Strategy-driven entry point over the precomputed-snapshot engine.
 *
 * [PT] Gateway unificado de jogos (compatível com a migração do `AppDimensGames` 2.0.1).
 * Ponto de entrada orientado a estratégia sobre o motor de snapshot pré-computado.
 *
 * ```kotlin
 * GameScreen.updateFromContext(context)           // once per resize/config change
 * val player = AppDimensGames.calculate(64f, GameScalingStrategy.BALANCED)
 * ```
 */
object AppDimensGames {

    /** Legacy-compatible initialization. / Inicialização compatível com o legado. */
    @JvmStatic
    @JvmOverloads
    fun initialize(context: Context, performanceMode: String = "BALANCED") {
        GameScreen.updateFromContext(context)
        warmup()
    }

    /**
     * [EN] Main calculation. Hot path = one multiply when the strategy factor is cached.
     * [PT] Cálculo principal. Hot path = uma multiplicação quando o fator está cacheado.
     */
    @JvmStatic
    fun calculate(
        baseValue: Float,
        strategy: GameScalingStrategy,
        elementType: GameElementType? = null,
        metrics: GameMetrics = GameScreen.metrics(),
    ): Float {
        val effective = elementType?.recommended ?: strategy
        return when (effective) {
            GameScalingStrategy.DEFAULT -> baseValue * metrics.defaultScaledAspectRatioMultiplier
            GameScalingStrategy.PERCENTAGE -> GameMath.calculatePercentDp(baseValue, metrics)
            GameScalingStrategy.BALANCED -> GameMath.calculateAutoDp(baseValue, metrics)
            GameScalingStrategy.LOGARITHMIC -> GameMath.calculateLogarithmicDp(baseValue, metrics)
            GameScalingStrategy.POWER -> GameMath.calculatePowerDp(baseValue, metrics)
            GameScalingStrategy.FLUID ->
                GameMath.calculateFluidDp(baseValue, metrics, baseValue * 0.8f, baseValue * 1.2f)
            GameScalingStrategy.INTERPOLATED -> GameMath.calculateInterpolatedDp(baseValue, metrics)
            GameScalingStrategy.DIAGONAL -> GameMath.calculateDiagonalDp(baseValue, metrics)
            GameScalingStrategy.PERIMETER -> GameMath.calculatePerimeterDp(baseValue, metrics)
            GameScalingStrategy.FIT -> GameMath.calculateFitDp(baseValue, metrics)
            GameScalingStrategy.FILL -> GameMath.calculateFillDp(baseValue, metrics)
            GameScalingStrategy.AUTOSIZE -> baseValue // requires container; use resize module
            GameScalingStrategy.NONE -> baseValue
        }
    }

    // ─── Legacy-named helpers (2.0.1 migration surface) ────────────────────

    /** HUD button size (DEFAULT). / Tamanho de botão de HUD. */
    @JvmStatic fun calculateButtonSize(baseDp: Float): Float = calculate(baseDp, GameScalingStrategy.DEFAULT)

    /** Player size (BALANCED — recommended). / Tamanho do jogador. */
    @JvmStatic fun calculatePlayerSize(baseDp: Float): Float = calculate(baseDp, GameScalingStrategy.BALANCED)

    /** Enemy size (BALANCED). / Tamanho de inimigo. */
    @JvmStatic fun calculateEnemySize(baseDp: Float): Float = calculate(baseDp, GameScalingStrategy.BALANCED)

    /** World/container size (PERCENTAGE). / Tamanho de mundo/contêiner. */
    @JvmStatic fun calculateWorldSize(baseDp: Float): Float = calculate(baseDp, GameScalingStrategy.PERCENTAGE)

    /** Background size (FILL). / Tamanho de fundo. */
    @JvmStatic fun calculateBackgroundSize(baseDp: Float): Float = calculate(baseDp, GameScalingStrategy.FILL)

    /** Viewport content size (FIT). / Tamanho de conteúdo de viewport. */
    @JvmStatic fun calculateViewportSize(baseDp: Float): Float = calculate(baseDp, GameScalingStrategy.FIT)

    /** Touch target size (DIAGONAL). / Tamanho de alvo de toque. */
    @JvmStatic fun calculateTouchTarget(baseDp: Float): Float = calculate(baseDp, GameScalingStrategy.DIAGONAL)

    /**
     * [EN] Pre-warms lazy factors for the current snapshot.
     * [PT] Pré-aquece os fatores lazy do snapshot atual.
     */
    @JvmStatic
    fun warmup() {
        val m = GameScreen.metrics()
        m.powerScale; m.interpolatedScale; m.diagonalScale; m.perimeterScale
        m.logarithmicScale; m.autoScale; m.fitScale; m.fillScale
    }

    /** Cache stats proxy. / Proxy das estatísticas de cache. */
    @JvmStatic fun cacheStats(): GameCache.Stats = GameCache.stats()

    /** Clears all caches. / Limpa todos os caches. */
    @JvmStatic fun clearCaches() = GameCache.clearAll()
}
