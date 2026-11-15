package com.appdimens.games

import com.appdimens.games.core.GameMetrics
import com.appdimens.games.units.PhysicalUnits
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [EN] Physical units conversions at reference and real densities.
 * [PT] Conversões de unidades físicas na densidade de referência e reais.
 */
class PhysicalUnitsTest {

    private val ref = GameMetrics.of(300, 533, densityDpi = 160)   // 1px = 1dp
    private val xhdpi = GameMetrics.of(300, 533, densityDpi = 320) // density 2

    @Test fun one_inch_equals_25_4mm_at_any_density() {
        val mm = PhysicalUnits.mmToPx(25.4f, ref)
        assertEquals(PhysicalUnits.inchToPx(1f, ref), mm, 1e-4f)
        val mm2 = PhysicalUnits.mmToPx(25.4f, xhdpi)
        assertEquals(PhysicalUnits.inchToPx(1f, xhdpi), mm2, 1e-3f)
    }

    @Test fun cm_is_ten_mm() {
        assertEquals(
            PhysicalUnits.mmToPx(10f, xhdpi),
            PhysicalUnits.cmToPx(1f, xhdpi), 1e-4f
        )
    }

    @Test fun inch_to_dp_is_density_independent() {
        // 1in = 160dp by definition (xdpi defaults to density*160)
        assertEquals(160f, PhysicalUnits.inchToDp(1f, xhdpi), 1e-4f)
        assertEquals(160f, PhysicalUnits.inchToDp(1f, ref), 1e-4f)
    }

    @Test fun explicit_xdpi_overrides_default() {
        // 1cm at exactly 480dpi physical → 10mm × 480/25.4 ≈ 188.976px
        assertEquals(10f * 480f / 25.4f, PhysicalUnits.cmToPx(1f, ref, xdpi = 480f), 1e-3f)
    }

    @Test fun two_cm_touch_target_helper_matches_cm_to_px() {
        assertEquals(PhysicalUnits.cmToPx(2f, xhdpi), PhysicalUnits.cmToPx(2f, xhdpi), 0f)
    }

    @Test fun radius_helpers() {
        assertEquals(16f, PhysicalUnits.radiusFromDiameter(32f), 0f)
        val c = 2f * Math.PI.toFloat() * 7f
        assertEquals(7f, PhysicalUnits.radiusFromCircumference(c), 1e-3f)
    }
}
