package com.appdimens.games.math

import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Inverter
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.core.GameScreenConstants as C
import kotlin.math.ln

/**
 * [EN] Pure calculation kernels for all strategies. Single source of truth.
 * Every function is allocation-free and safe to call per-frame at 60+ FPS.
 * Math is bit-exact with appdimens-dynamic / appdimens-kmp.
 *
 * [PT] Kernels puros de cálculo de todas as estratégias. Fonte única de verdade.
 * Toda função é sem alocação e segura para chamadas por frame a 60+ FPS.
 * A matemática é bit-exata com appdimens-dynamic / appdimens-kmp.
 */
object GameMath {

    // ─── SCALED (sdp/hdp/wdp) ──────────────────────────────────────────────

    /** [EN] SCALED kernel. [PT] Kernel SCALED. */
    fun calculateScaledDp(
        baseValue: Float,
        metrics: GameMetrics,
        qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
        inverter: Inverter = Inverter.DEFAULT,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
    ): Float {
        val q = resolve(qualifier, inverter, metrics)
        val isDefaultSw = qualifier == DpQualifier.SMALL_WIDTH && inverter == Inverter.DEFAULT
        if (isDefaultSw && customSensitivityK == null) {
            return baseValue * metrics.scaledMultiplier(applyAspectRatio, null)
        }
        val dim = metrics.axisDp(q)
        return scaledWithDim(baseValue, dim, metrics, applyAspectRatio, customSensitivityK)
    }

    private inline fun scaledWithDim(
        baseValue: Float, dim: Float, metrics: GameMetrics,
        applyAspectRatio: Boolean, customSensitivityK: Float?,
    ): Float =
        if (applyAspectRatio) {
            val adjustment = (customSensitivityK ?: C.SENSITIVITY_DEFAULT) * metrics.logNormalizedAspectRatio
            baseValue * (1f + (dim - C.BASE_WIDTH_DP) * (C.ADJUSTMENT_SCALE + adjustment))
        } else {
            baseValue * (dim * C.INV_BASE_RATIO)
        }

    // ─── PERCENT (psdp) ────────────────────────────────────────────────────

    /** [EN] PERCENT kernel (same curve as SCALED). [PT] Kernel PERCENT (mesma curva do SCALED). */
    fun calculatePercentDp(
        baseValue: Float,
        metrics: GameMetrics,
        qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
        inverter: Inverter = Inverter.DEFAULT,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
    ): Float = calculateScaledDp(baseValue, metrics, qualifier, inverter, applyAspectRatio, customSensitivityK)

    /** [EN] Literal screen percentage: `(p/100)·axis`. [PT] Percentual literal da tela. */
    fun percentOfAxisDp(percent: Float, metrics: GameMetrics, qualifier: DpQualifier): Float =
        if (!percent.isFinite()) 0f else (percent / 100f) * metrics.axisDp(qualifier)

    /** [EN] Percentage of an explicit reference. [PT] Percentual de uma referência explícita. */
    fun percentOfReferenceDp(percent: Float, referenceDp: Float): Float =
        if (!percent.isFinite()) 0f else (percent / 100f) * referenceDp

    // ─── POWER (pwsdp) ─────────────────────────────────────────────────────

    /** [EN] POWER kernel `(d/300)^exp`. [PT] Kernel POWER. */
    fun calculatePowerDp(
        baseValue: Float,
        metrics: GameMetrics,
        powerExponent: Float = C.POWER_EXPONENT_DEFAULT,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
        inverter: Inverter = Inverter.DEFAULT,
    ): Float {
        val isDefaultSw = qualifier == DpQualifier.SMALL_WIDTH && inverter == Inverter.DEFAULT &&
            powerExponent == C.POWER_EXPONENT_DEFAULT
        val scale = if (isDefaultSw) {
            metrics.powerScale
        } else {
            val ratio = metrics.axisDp(resolve(qualifier, inverter, metrics)) / C.BASE_WIDTH_DP
            Math.pow(ratio.toDouble(), powerExponent.toDouble()).toFloat()
        }
        var out = baseValue * scale
        if (applyAspectRatio) out *= arMul(metrics, customSensitivityK)
        return out
    }

    // ─── FLUID (fsdp) ──────────────────────────────────────────────────────

    /** [EN] FLUID kernel: lerp between `b·0.8` and `b·1.2` inside the 320–768 band. [PT] Kernel FLUID. */
    fun calculateFluidDp(
        baseValue: Float,
        metrics: GameMetrics,
        minValue: Float = baseValue * 0.8f,
        maxValue: Float = baseValue * 1.2f,
        minWidth: Float = C.FLUID_MIN_WIDTH_DP,
        maxWidth: Float = C.FLUID_MAX_WIDTH_DP,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
        inverter: Inverter = Inverter.DEFAULT,
    ): Float {
        val dim = metrics.axisDp(resolve(qualifier, inverter, metrics))
        val v = when {
            dim <= minWidth -> minValue
            dim >= maxWidth -> maxValue
            else -> minValue + (maxValue - minValue) * (dim - minWidth) / (maxWidth - minWidth)
        }
        var out = v
        if (applyAspectRatio) out *= arMul(metrics, customSensitivityK)
        return out
    }

    // ─── AUTO (asdp) ───────────────────────────────────────────────────────

    /** [EN] AUTO kernel: linear ≤480 dp then logarithmic. [PT] Kernel AUTO. */
    fun calculateAutoDp(
        baseValue: Float,
        metrics: GameMetrics,
        transitionPoint: Float = C.AUTO_TRANSITION_DP,
        sensitivity: Float = C.SENSITIVITY_LOG,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
        inverter: Inverter = Inverter.DEFAULT,
    ): Float {
        val isDefaultSw = qualifier == DpQualifier.SMALL_WIDTH && inverter == Inverter.DEFAULT &&
            transitionPoint == C.AUTO_TRANSITION_DP && sensitivity == C.SENSITIVITY_LOG
        val scale = if (isDefaultSw) {
            metrics.autoScale
        } else {
            val d = metrics.axisDp(resolve(qualifier, inverter, metrics))
            if (d <= transitionPoint) d * C.INV_BASE_RATIO
            else (transitionPoint * C.INV_BASE_RATIO) + sensitivity * ln(1f + (d - transitionPoint) * C.INV_BASE_RATIO)
        }
        var out = baseValue * scale
        if (applyAspectRatio) out *= arMul(metrics, customSensitivityK)
        return out
    }

    // ─── LOGARITHMIC (logsdp) ──────────────────────────────────────────────

    /** [EN] LOGARITHMIC kernel (Weber-Fechner). [PT] Kernel LOGARITHMIC. */
    fun calculateLogarithmicDp(
        baseValue: Float,
        metrics: GameMetrics,
        sensitivity: Float = C.SENSITIVITY_LOG,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
        inverter: Inverter = Inverter.DEFAULT,
    ): Float {
        val isDefaultSw = qualifier == DpQualifier.SMALL_WIDTH && inverter == Inverter.DEFAULT &&
            sensitivity == C.SENSITIVITY_LOG
        val scale = if (isDefaultSw) {
            metrics.logarithmicScale
        } else {
            logScaleOf(metrics.axisDp(resolve(qualifier, inverter, metrics)), sensitivity)
        }
        var out = baseValue * scale
        if (applyAspectRatio) out *= arMul(metrics, customSensitivityK)
        return out
    }

    private inline fun logScaleOf(dim: Float, sensitivity: Float): Float =
        if (dim > C.BASE_WIDTH_DP) 1f + sensitivity * ln(dim * C.INV_BASE_RATIO)
        else if (dim > 0f) 1f - sensitivity * ln(C.BASE_WIDTH_DP / dim)
        else 1f

    // ─── INTERPOLATED (isdp) ───────────────────────────────────────────────

    /** [EN] INTERPOLATED kernel: midpoint between base and linear. [PT] Kernel INTERPOLATED. */
    fun calculateInterpolatedDp(
        baseValue: Float,
        metrics: GameMetrics,
        fraction: Float = 0.5f,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
        inverter: Inverter = Inverter.DEFAULT,
    ): Float {
        val isDefaultSw = qualifier == DpQualifier.SMALL_WIDTH && inverter == Inverter.DEFAULT &&
            fraction == 0.5f
        val out = if (isDefaultSw) {
            baseValue * metrics.interpolatedScale
        } else {
            val linear = baseValue * (metrics.axisDp(resolve(qualifier, inverter, metrics)) * C.INV_BASE_RATIO)
            baseValue + (linear - baseValue) * fraction
        }
        return if (applyAspectRatio) out * arMul(metrics, customSensitivityK) else out
    }

    // ─── DIAGONAL / PERIMETER / FIT / FILL / DENSITY ───────────────────────

    /** [EN] DIAGONAL kernel. [PT] Kernel DIAGONAL. */
    fun calculateDiagonalDp(
        baseValue: Float,
        metrics: GameMetrics,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        @Suppress("UNUSED_PARAMETER") qualifier: com.appdimens.games.common.DpQualifier = com.appdimens.games.common.DpQualifier.SMALL_WIDTH,
        @Suppress("UNUSED_PARAMETER") inverter: com.appdimens.games.common.Inverter = com.appdimens.games.common.Inverter.DEFAULT,
    ): Float {
        val out = baseValue * metrics.diagonalScale
        return if (applyAspectRatio) out * arMul(metrics, customSensitivityK) else out
    }

    /** [EN] PERIMETER kernel. [PT] Kernel PERIMETER. */
    fun calculatePerimeterDp(
        baseValue: Float,
        metrics: GameMetrics,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        @Suppress("UNUSED_PARAMETER") qualifier: com.appdimens.games.common.DpQualifier = com.appdimens.games.common.DpQualifier.SMALL_WIDTH,
        @Suppress("UNUSED_PARAMETER") inverter: com.appdimens.games.common.Inverter = com.appdimens.games.common.Inverter.DEFAULT,
    ): Float {
        val out = baseValue * metrics.perimeterScale
        return if (applyAspectRatio) out * arMul(metrics, customSensitivityK) else out
    }

    /** [EN] FIT kernel (letterbox). [PT] Kernel FIT. */
    fun calculateFitDp(
        baseValue: Float,
        metrics: GameMetrics,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        @Suppress("UNUSED_PARAMETER") qualifier: com.appdimens.games.common.DpQualifier = com.appdimens.games.common.DpQualifier.SMALL_WIDTH,
        @Suppress("UNUSED_PARAMETER") inverter: com.appdimens.games.common.Inverter = com.appdimens.games.common.Inverter.DEFAULT,
    ): Float {
        val out = baseValue * metrics.fitScale
        return if (applyAspectRatio) out * arMul(metrics, customSensitivityK) else out
    }

    /** [EN] FILL kernel (cover). [PT] Kernel FILL. */
    fun calculateFillDp(
        baseValue: Float,
        metrics: GameMetrics,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        @Suppress("UNUSED_PARAMETER") qualifier: com.appdimens.games.common.DpQualifier = com.appdimens.games.common.DpQualifier.SMALL_WIDTH,
        @Suppress("UNUSED_PARAMETER") inverter: com.appdimens.games.common.Inverter = com.appdimens.games.common.Inverter.DEFAULT,
    ): Float {
        val out = baseValue * metrics.fillScale
        return if (applyAspectRatio) out * arMul(metrics, customSensitivityK) else out
    }

    /** [EN] DENSITY kernel (dpi/160). [PT] Kernel DENSITY. */
    fun calculateDensityDp(
        baseValue: Float,
        metrics: GameMetrics,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
        @Suppress("UNUSED_PARAMETER") qualifier: com.appdimens.games.common.DpQualifier = com.appdimens.games.common.DpQualifier.SMALL_WIDTH,
        @Suppress("UNUSED_PARAMETER") inverter: com.appdimens.games.common.Inverter = com.appdimens.games.common.Inverter.DEFAULT,
    ): Float {
        val out = baseValue * metrics.density
        return if (applyAspectRatio) out * arMul(metrics, customSensitivityK) else out
    }

    // ─── Shared helpers ────────────────────────────────────────────────────

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun arMul(metrics: GameMetrics, customK: Float?): Float =
        if (customK == null) metrics.defaultAspectRatioMultiplier
        else 1f + customK * metrics.logNormalizedAspectRatio

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun resolve(q: DpQualifier, inv: Inverter, m: GameMetrics): DpQualifier =
        GameScreen.effectiveQualifier(
            q, inv,
            landscape = m.screenWidthDp > m.screenHeightDp,
            portrait = m.screenHeightDp > m.screenWidthDp
        )

    // ─── px/sp conversion ──────────────────────────────────────────────────

    /** dp→px using the snapshot density (exact family conversion). */
    fun toPx(dp: Float, metrics: GameMetrics): Float = dp * metrics.density

    /** dp→sp honoring system font scale unless [respectFontScale] is false. */
    fun toSp(dp: Float, metrics: GameMetrics, respectFontScale: Boolean = true): Float =
        if (respectFontScale) dp else dp / metrics.fontScale
}
