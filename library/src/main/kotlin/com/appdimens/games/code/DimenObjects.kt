package com.appdimens.games.code

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Inverter

/**
 * [EN] Java-friendly static facade — family parity with `DimenSdp` (dynamic/kmp).
 * [PT] Fachada estática amigável a Java — paridade com `DimenSdp` (dynamic/kmp).
 *
 * ```java
 * float hud = DimenSdp.sdp(ctx, 48);
 * float inv = DimenSdp.sdpi(ctx, 48);          // `i` — resize invariant
 * float px  = DimenSdp.getDimensionInPx(ctx, DpQualifier.SMALL_WIDTH, 16,
 *                                        Inverter.DEFAULT, false, false, null);
 * ```
 */
object DimenSdp {

    @JvmStatic fun sdp(context: Context?, value: Int): Float = value.sdp(context)
    @JvmStatic fun sdpa(context: Context?, value: Int): Float = value.sdpa(context)
    @JvmStatic fun sdpi(context: Context?, value: Int): Float = value.sdpi(context)
    @JvmStatic fun sdpia(context: Context?, value: Int): Float = value.sdpia(context)

    @JvmStatic fun hdp(context: Context?, value: Int): Float = value.hdp(context)
    @JvmStatic fun hdpi(context: Context?, value: Int): Float = value.hdpi(context)
    @JvmStatic fun wdp(context: Context?, value: Int): Float = value.wdp(context)
    @JvmStatic fun wdpi(context: Context?, value: Int): Float = value.wdpi(context)

    /** Full-control px resolution. / Resolução de controle total em px. */
    @JvmStatic
    @JvmOverloads
    fun getDimensionInPx(
        context: Context?,
        qualifier: DpQualifier,
        value: Int,
        inverter: Inverter = Inverter.DEFAULT,
        ignoreMultiWindows: Boolean = false,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
    ): Float = value.toFloat().toDynamicScaledPx(
        context, qualifier, inverter, ignoreMultiWindows, applyAspectRatio, customSensitivityK
    )

    /** Full-control dp resolution. */
    @JvmStatic
    @JvmOverloads
    fun getDimensionInDp(
        context: Context?,
        qualifier: DpQualifier,
        value: Int,
        inverter: Inverter = Inverter.DEFAULT,
        ignoreMultiWindows: Boolean = false,
        applyAspectRatio: Boolean = false,
        customSensitivityK: Float? = null,
    ): Float = value.toFloat().toDynamicScaledDp(
        context, qualifier, inverter, ignoreMultiWindows, applyAspectRatio, customSensitivityK
    )

    /** Builder entry for Java. / Entrada do builder para Java. */
    @JvmStatic fun scaled(value: Int): DimenScaled = DimenScaled.of(value)

    /** Warms the lazy factors of the current snapshot. / Aquece os fatores lazy do snapshot. */
    @JvmStatic fun warmupCache() {
        val m = GameScreenMetricsProvider.current()
        m.powerScale; m.interpolatedScale; m.diagonalScale; m.perimeterScale
        m.logarithmicScale; m.autoScale; m.fitScale; m.fillScale
    }
}

internal object GameScreenMetricsProvider {
    fun current() = com.appdimens.games.core.GameScreen.metrics()
}
