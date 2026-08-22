package com.appdimens.games.units

import com.appdimens.games.core.GameMetrics
import kotlin.math.PI

/**
 * [EN] Physical units → dp/px conversions (family parity with `library-units`).
 * [PT] Conversões de unidades físicas → dp/px (paridade com `library-units`).
 */
object PhysicalUnits {

    /** mm→dp using the window x-density. / mm→dp. */
    @JvmStatic
    fun mmToDp(mm: Float, metrics: GameMetrics, xdpi: Float = 160f * metrics.density): Float =
        mm * xdpi / 25.4f / metrics.density

    /** cm→dp. */
    @JvmStatic
    fun cmToDp(cm: Float, metrics: GameMetrics, xdpi: Float = 160f * metrics.density): Float =
        cm * 10f * xdpi / 25.4f / metrics.density

    /** inch→dp. */
    @JvmStatic
    fun inchToDp(inch: Float, metrics: GameMetrics, xdpi: Float = 160f * metrics.density): Float =
        inch * xdpi / metrics.density

    /** mm→px. */
    @JvmStatic
    fun mmToPx(mm: Float, metrics: GameMetrics, xdpi: Float = 160f * metrics.density): Float =
        mm * xdpi / 25.4f

    /** cm→px (2 cm touch target helper). */
    @JvmStatic
    fun cmToPx(cm: Float, metrics: GameMetrics, xdpi: Float = 160f * metrics.density): Float =
        cm * 10f * xdpi / 25.4f

    /** inch→px. */
    @JvmStatic
    fun inchToPx(inch: Float, metrics: GameMetrics, xdpi: Float = 160f * metrics.density): Float =
        inch * xdpi

    /** Radius from a physical diameter in px. */
    @JvmStatic fun radiusFromDiameter(diameterPx: Float): Float = diameterPx / 2f

    /** Radius from a circumference length in px. */
    @JvmStatic fun radiusFromCircumference(circumferencePx: Float): Float = circumferencePx / (2f * PI.toFloat())
}
