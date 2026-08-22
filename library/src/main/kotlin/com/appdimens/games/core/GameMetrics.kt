package com.appdimens.games.core

import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.GameDeviceType
import com.appdimens.games.common.UiModeType
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * [EN] Immutable per-window snapshot with every scaling factor pre-computed once.
 * All hot-path kernels reduce to `base * factor` — one float multiply, zero allocation.
 * Mirrors `DimenMetrics` from appdimens-dynamic (bit-exact factors) and adds game-only
 * fields (`aspectRatioRaw`, `isFullscreen`, `safeArea`).
 *
 * [PT] Snapshot imutável por janela com todos os fatores pré-computados uma única vez.
 * Todos os kernels do hot path reduzem a `base * fator` — uma multiplicação float, zero alocação.
 * Espelha o `DimenMetrics` do appdimens-dynamic (fatores bit-exatos) e adiciona campos
 * exclusivos de jogos (`aspectRatioRaw`, `isFullscreen`, `safeArea`).
 *
 * @property screenWidthDp   current window width in dp
 * @property screenHeightDp  current window height in dp
 * @property smallestScreenWidthDp rotation-invariant smallest width in dp
 * @property densityDpi      raw density in dpi (160 = mdpi)
 * @property fontScaleBits   system font scale packed via [Float.toRawBits]
 * @property uiMode          current UI mode type
 * @property isFullscreen    true when the game surface covers the whole display
 */
class GameMetrics(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val smallestScreenWidthDp: Int,
    val densityDpi: Int,
    internal val fontScaleBits: Int,
    val uiMode: UiModeType = UiModeType.NORMAL,
    val isFullscreen: Boolean = true,
) {
    /** Safe-area insets in dp (notches/cutouts). Zero by default. / Área segura em dp. */
    val safeAreaDp: FloatArray = FloatArray(4)

    // ─── Derived primitives ────────────────────────────────────────────────

    /** System font scale (1f fallback). / Escala de fonte do sistema. */
    val fontScale: Float =
        Float.fromBits(fontScaleBits).takeIf { it.isFinite() && it > 0f } ?: 1f

    /** min(w,h) ≥ 0. */
    val minDimensionDp: Float =
        minOf(screenWidthDp, screenHeightDp).coerceAtLeast(0).toFloat()

    /** max(w,h) ≥ 0. */
    val maxDimensionDp: Float =
        maxOf(screenWidthDp, screenHeightDp).coerceAtLeast(0).toFloat()

    /** Effective smallest width (fallback chain → base 300). */
    val smallestWidthDpF: Float =
        smallestScreenWidthDp.takeIf { it > 0 }?.toFloat()
            ?: minDimensionDp.takeIf { it > 0f }
            ?: GameScreenConstants.BASE_WIDTH_DP

    /** ρ = dpi/160. */
    val density: Float =
        (densityDpi.toFloat() / 160f).takeIf { it.isFinite() && it > 0f } ?: 1f

    /** sw/300 — SCALED default factor. */
    val scale: Float = smallestWidthDpF * GameScreenConstants.INV_BASE_RATIO

    /** w/300 — WIDTH-qualifier factor. */
    internal val screenWidthFactor: Float = screenWidthDp.coerceAtLeast(0) * GameScreenConstants.INV_BASE_RATIO

    /** h/300 — HEIGHT-qualifier factor. */
    internal val screenHeightFactor: Float = screenHeightDp.coerceAtLeast(0) * GameScreenConstants.INV_BASE_RATIO

    /** Raw aspect ratio max/min (> 0). */
    val aspectRatioRaw: Float =
        if (minDimensionDp > 0f) maxDimensionDp / minDimensionDp else GameScreenConstants.REFERENCE_ASPECT_RATIO

    /** (max/min)/1.78 normalized. */
    val normalizedAspectRatio: Float =
        (aspectRatioRaw / GameScreenConstants.REFERENCE_ASPECT_RATIO)
            .takeIf { it.isFinite() && it > 0f } ?: 1f

    /** ln(normalizedAR) computed once per snapshot. */
    val logNormalizedAspectRatio: Float = ln(normalizedAspectRatio.toDouble()).toFloat()

    /** 1 + K_default·ln(ARn) — AR multiplier for satellite strategies. */
    val defaultAspectRatioMultiplier: Float =
        1f + GameScreenConstants.SENSITIVITY_DEFAULT * logNormalizedAspectRatio

    /** AR-aware SCALED multiplier (default path, sdpa fast lane). */
    val defaultScaledAspectRatioMultiplier: Float =
        1f + (smallestWidthDpF - GameScreenConstants.BASE_WIDTH_DP) *
            (GameScreenConstants.ADJUSTMENT_SCALE + GameScreenConstants.SENSITIVITY_DEFAULT * logNormalizedAspectRatio)

    // ─── Satellite factors (lazy: computed at most once per snapshot) ─────

    /** (sw/300)^0.75 */
    val powerScale: Float by lazy {
        Math.pow((smallestWidthDpF / GameScreenConstants.BASE_WIDTH_DP).toDouble(), GameScreenConstants.POWER_EXPONENT_DEFAULT.toDouble()).toFloat()
    }

    /** 1 + (sw/300 − 1) × 0.5 */
    val interpolatedScale: Float by lazy {
        1f + (smallestWidthDpF * GameScreenConstants.INV_BASE_RATIO - 1f) * 0.5f
    }

    /** √(min² + max²)/611.6305 */
    val diagonalScale: Float by lazy {
        sqrt(minDimensionDp * minDimensionDp + maxDimensionDp * maxDimensionDp) /
            GameScreenConstants.BASE_DIAGONAL_DP
    }

    /** (min + max)/833 */
    val perimeterScale: Float by lazy {
        (minDimensionDp + maxDimensionDp) / GameScreenConstants.BASE_PERIMETER_DP
    }

    /** Piecewise logarithmic factor (memoized from the canonical when-chain). */
    val logarithmicScale: Float by lazy {
        when {
            smallestWidthDpF > GameScreenConstants.BASE_WIDTH_DP ->
                1f + GameScreenConstants.SENSITIVITY_LOG * ln(smallestWidthDpF * GameScreenConstants.INV_BASE_RATIO)
            smallestWidthDpF > 0f ->
                1f - GameScreenConstants.SENSITIVITY_LOG * ln(GameScreenConstants.BASE_WIDTH_DP / smallestWidthDpF)
            else -> 1f
        }
    }

    /** AUTO factor: linear ≤ 480, log above (memoized). */
    val autoScale: Float by lazy {
        val d = smallestWidthDpF
        if (d <= GameScreenConstants.AUTO_TRANSITION_DP) {
            d * GameScreenConstants.INV_BASE_RATIO
        } else {
            (GameScreenConstants.AUTO_TRANSITION_DP * GameScreenConstants.INV_BASE_RATIO) +
                GameScreenConstants.SENSITIVITY_LOG * ln(
                    1f + (d - GameScreenConstants.AUTO_TRANSITION_DP) * GameScreenConstants.INV_BASE_RATIO
                )
        }
    }

    /** FIT factor: min(min/300, max/533). */
    val fitScale: Float by lazy {
        minOf(
            minDimensionDp / GameScreenConstants.BASE_WIDTH_DP,
            maxDimensionDp / GameScreenConstants.BASE_HEIGHT_DP
        )
    }

    /** FILL factor: max(min/300, max/533). */
    val fillScale: Float by lazy {
        maxOf(
            minDimensionDp / GameScreenConstants.BASE_WIDTH_DP,
            maxDimensionDp / GameScreenConstants.BASE_HEIGHT_DP
        )
    }

    /** Inferred device class. */
    val deviceType: GameDeviceType by lazy { GameDeviceType.from(smallestWidthDpF, uiMode) }

    // ─── Multipliers API ───────────────────────────────────────────────────

    /**
     * [EN] SCALED multiplier honoring AR flags.
     * [PT] Multiplicador SCALED respeitando as flags de AR.
     */
    fun scaledMultiplier(applyAspectRatio: Boolean, customSensitivityK: Float?): Float {
        if (!applyAspectRatio) return scale
        if (customSensitivityK == null) return defaultScaledAspectRatioMultiplier
        require(customSensitivityK.isFinite()) { "customSensitivityK must be finite" }
        val result = 1f + (smallestWidthDpF - GameScreenConstants.BASE_WIDTH_DP) *
            (GameScreenConstants.ADJUSTMENT_SCALE + customSensitivityK * logNormalizedAspectRatio)
        require(result.isFinite()) { "customSensitivityK produces a non-finite multiplier" }
        return result
    }

    /**
     * [EN] Standalone aspect-ratio multiplier.
     * [PT] Multiplicador de proporção isolado.
     */
    fun aspectRatioMultiplier(customSensitivityK: Float?): Float {
        if (customSensitivityK == null) return defaultAspectRatioMultiplier
        require(customSensitivityK.isFinite()) { "customSensitivityK must be finite" }
        val result = 1f + customSensitivityK * logNormalizedAspectRatio
        require(result.isFinite()) { "customSensitivityK produces a non-finite multiplier" }
        return result
    }

    /** Axis dimension in dp after qualifier/inverter resolution. */
    fun axisDp(qualifier: DpQualifier): Float = when (qualifier) {
        DpQualifier.SMALL_WIDTH -> smallestWidthDpF
        DpQualifier.WIDTH -> screenWidthDp.coerceAtLeast(0).toFloat()
        DpQualifier.HEIGHT -> screenHeightDp.coerceAtLeast(0).toFloat()
    }

    override fun equals(other: Any?): Boolean = other is GameMetrics &&
        screenWidthDp == other.screenWidthDp &&
        screenHeightDp == other.screenHeightDp &&
        smallestScreenWidthDp == other.smallestScreenWidthDp &&
        densityDpi == other.densityDpi &&
        fontScaleBits == other.fontScaleBits &&
        uiMode == other.uiMode &&
        isFullscreen == other.isFullscreen

    override fun hashCode(): Int {
        var r = screenWidthDp
        r = 31 * r + screenHeightDp
        r = 31 * r + smallestScreenWidthDp
        r = 31 * r + densityDpi
        r = 31 * r + fontScaleBits
        r = 31 * r + uiMode.hashCode()
        r = 31 * r + if (isFullscreen) 1 else 0
        return r
    }

    companion object {
        /** Synthetic reference window (300×533 @160dpi) used by `i` fallbacks and tests. */
        @JvmField
        val DEFAULT: GameMetrics = GameMetrics(
            screenWidthDp = 300, screenHeightDp = 533, smallestScreenWidthDp = 300,
            densityDpi = 160, fontScaleBits = 1f.toRawBits(),
            uiMode = UiModeType.NORMAL, isFullscreen = true
        )

        @JvmStatic
        fun of(
            widthDp: Int, heightDp: Int, smallestWidthDp: Int = 0,
            densityDpi: Int = 160, fontScale: Float = 1f,
            uiMode: UiModeType = UiModeType.NORMAL, isFullscreen: Boolean = true
        ): GameMetrics = GameMetrics(
            widthDp, heightDp, smallestWidthDp, densityDpi,
            fontScale.toRawBits(), uiMode, isFullscreen
        )
    }
}
