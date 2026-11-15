package com.appdimens.games.code

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Inverter
import com.appdimens.games.common.Orientation
import com.appdimens.games.common.UiModeType
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Fluent builder — family parity with Dynamic/KMP `scaledDp()`.
 * [PT] Builder fluente — paridade com `scaledDp()` do Dynamic/KMP.
 *
 * ```kotlin
 * 16.scaledDp()
 *    .aspectRatio(true)
 *    .screen(UiModeType.TELEVISION, 32)
 *    .qualifier(DpQualifier.SMALL_WIDTH, 600, 24)
 *    .sdp(context)        // terminal → px (family convention)
 * ```
 */
class DimenScaled private constructor(private val baseValue: Float) {

    private var applyAspectRatio = false
    private var customSensitivityK: Float? = null
    private var ignoreMultiWindows = false

    // Priority entries (family order): 1 = mode+qualifier, 2 = mode, 3 = qualifier, 4 = orientation.
    private var modeQualifier: Triple<UiModeType, DpQualifier, Int>? = null
    private var modeEntry: Pair<UiModeType, Float>? = null
    private var qualifierEntry: Triple<DpQualifier, Int, Float>? = null
    private var orientationEntry: Pair<Orientation, Float>? = null

    fun aspectRatio(enabled: Boolean = true, sensitivityK: Float? = null) = apply {
        applyAspectRatio = enabled; customSensitivityK = sensitivityK
    }

    fun ignoreMultiWindows(ignore: Boolean = true) = apply { ignoreMultiWindows = ignore }

    /** Priority 1. / Prioridade 1. */
    fun screen(uiModeType: UiModeType, qualifierType: DpQualifier, qualifierThreshold: Int, value: Number) =
        apply { modeQualifier = Triple(uiModeType, qualifierType, qualifierThreshold); modeEntry = uiModeType to value.toFloat() }

    /** Priority 2. / Prioridade 2. */
    fun screen(uiModeType: UiModeType, value: Number) = apply { modeEntry = uiModeType to value.toFloat() }

    /** Priority 3. / Prioridade 3. */
    fun qualifier(qualifierType: DpQualifier, qualifierThreshold: Int, value: Number) =
        apply { qualifierEntry = Triple(qualifierType, qualifierThreshold, value.toFloat()) }

    /** Priority 4. / Prioridade 4. */
    fun orientation(target: Orientation, value: Number) = apply { orientationEntry = target to value.toFloat() }

    /** Terminal: pixels (family code-side convention). */
    fun sdp(context: Context?): Float {
        val m = resolveMetrics(ignoreMultiWindows)
        val base = resolveOverride(m) ?: baseValue
        if (ignoreMultiWindows && isConstrained(m)) return base * m.density
        return GameMath.toPx(
            GameMath.calculateScaledDp(base, m, DpQualifier.SMALL_WIDTH, Inverter.DEFAULT, applyAspectRatio, customSensitivityK),
            m
        )
    }

    /** Terminal: density-independent dp. */
    fun dp(context: Context?): Float {
        val m = resolveMetrics(ignoreMultiWindows)
        val base = resolveOverride(m) ?: baseValue
        if (ignoreMultiWindows && isConstrained(m)) return base
        return GameMath.calculateScaledDp(base, m, DpQualifier.SMALL_WIDTH, Inverter.DEFAULT, applyAspectRatio, customSensitivityK)
    }

    private fun resolveOverride(m: GameMetrics): Float? {
        modeQualifier?.let { (mode, q, t) ->
            if (m.uiMode == mode && m.axisDp(q) >= t) return modeEntry?.second
        }
        modeEntry?.let { (mode, v) -> if (m.uiMode == mode) return v }
        qualifierEntry?.let { (q, t, v) -> if (m.axisDp(q) >= t) return v }
        orientationEntry?.let { (o, v) ->
            val current = when {
                m.screenWidthDp > m.screenHeightDp -> Orientation.LANDSCAPE
                m.screenHeightDp > m.screenWidthDp -> Orientation.PORTRAIT
                else -> Orientation.DEFAULT
            }
            if (o == current) return v
        }
        return null
    }

    companion object {
        /** [EN] Builder entry — family `scaledDp()`. [PT] Entrada do builder. */
        @JvmStatic
        fun of(baseValue: Number): DimenScaled = DimenScaled(baseValue.toFloat())
    }
}

/** [EN] Family builder entry over Number. [PT] Entrada do builder sobre Number. */
fun Number.scaledDp(): DimenScaled = DimenScaled.of(this)
