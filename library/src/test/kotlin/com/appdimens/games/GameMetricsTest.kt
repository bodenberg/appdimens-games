package com.appdimens.games

import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreenConstants as C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [EN] Snapshot (GameMetrics) semantics: canonical factors, fallback chains,
 * AR math and identity/equality contract.
 * [PT] Semântica do snapshot (GameMetrics): fatores canônicos, cadeias de fallback,
 * matemática de AR e contrato de identidade/igualdade.
 */
class GameMetricsTest {

    @Test fun scale_factor_is_sw_over_300() {
        val m = GameMetrics.of(360, 800, smallestWidthDp = 360)
        assertEquals(1.2f, m.scale, 1e-6f)
    }

    @Test fun smallest_width_falls_back_to_min_dimension_then_base() {
        assertEquals(360f, GameMetrics.of(360, 800).smallestWidthDpF, 0f)
        // sw=0 → min dimension
        assertEquals(240f, GameMetrics.of(240, 1000, smallestWidthDp = 0).smallestWidthDpF, 0f)
        // both zero → canonical 300
        assertEquals(C.BASE_WIDTH_DP, GameMetrics.of(0, 0).smallestWidthDpF, 0f)
    }

    @Test fun density_is_dpi_over_160_with_safe_fallback() {
        assertEquals(2f, GameMetrics.of(300, 533, densityDpi = 320).density, 0f)
        assertEquals(1f, GameMetrics.of(300, 533, densityDpi = 0).density, 0f)
        assertEquals(1f, GameMetrics.of(300, 533, densityDpi = -160).density, 0f)
    }

    @Test fun font_scale_falls_back_to_1_on_invalid() {
        val bad = GameMetrics(300, 533, 300, 160, 0f.toRawBits())
        assertEquals(1f, bad.fontScale, 0f)
        val neg = GameMetrics(300, 533, 300, 160, (-1.5f).toRawBits())
        assertEquals(1f, neg.fontScale, 0f)
        val ok = GameMetrics(300, 533, 300, 160, 1.3f.toRawBits())
        assertEquals(1.3f, ok.fontScale, 0f)
    }

    @Test fun aspect_ratio_raw_and_normalized() {
        val m = GameMetrics.of(400, 800) // AR 2.0
        assertEquals(2f, m.aspectRatioRaw, 1e-6f)
        assertEquals(2f / C.REFERENCE_ASPECT_RATIO, m.normalizedAspectRatio, 1e-6f)
        assertTrue(m.logNormalizedAspectRatio > 0f)
    }

    @Test fun aspect_ratio_guarded_when_zero_dimension() {
        val m = GameMetrics.of(0, 800)
        assertEquals(C.REFERENCE_ASPECT_RATIO, m.aspectRatioRaw, 0f)
        assertEquals(1f, m.defaultAspectRatioMultiplier, 0f)
    }

    @Test fun satellite_factors_match_canonical_formulas() {
        val m = GameMetrics.of(600, 960, smallestWidthDp = 600)
        // POWER: (600/300)^0.75
        assertEquals(Math.pow(2.0, 0.75).toFloat(), m.powerScale, 1e-4f)
        // INTERPOLATED: 1 + (2−1)/2
        assertEquals(1.5f, m.interpolatedScale, 1e-6f)
        // DIAGONAL: √(600²+960²)/611.6305
        assertEquals(Math.sqrt(600.0 * 600 + 960.0 * 960).toFloat() / C.BASE_DIAGONAL_DP, m.diagonalScale, 1e-4f)
        // PERIMETER: 1560/833
        assertEquals(1560f / C.BASE_PERIMETER_DP, m.perimeterScale, 1e-4f)
        // FIT / FILL: min/max(min/300, max/533)
        val fit = Math.min(600f / 300f, 960f / 533f)
        val fill = Math.max(600f / 300f, 960f / 533f)
        assertEquals(fit, m.fitScale, 1e-4f)
        assertEquals(fill, m.fillScale, 1e-4f)
    }

    @Test fun scaled_multiplier_ar_aware_matches_formula() {
        val m = GameMetrics.of(360, 640, smallestWidthDp = 360)
        val k = 0.01f
        val expectedNoAr = 360f * C.INV_BASE_RATIO
        assertEquals(expectedNoAr, m.scaledMultiplier(false, null), 1e-6f)

        val lnAr = kotlin.math.ln(((640f / 360f) / C.REFERENCE_ASPECT_RATIO).toDouble()).toFloat()
        val expectedCustom = 1f + (360f - C.BASE_WIDTH_DP) *
            (C.ADJUSTMENT_SCALE + k * lnAr)
        assertEquals(expectedCustom, m.scaledMultiplier(true, k), 1e-4f)
        assertEquals(m.defaultScaledAspectRatioMultiplier, m.scaledMultiplier(true, null), 0f)
    }

    @Test fun axis_dp_respects_qualifier() {
        val m = GameMetrics.of(360, 800, smallestWidthDp = 360)
        assertEquals(360f, m.axisDp(com.appdimens.games.common.DpQualifier.SMALL_WIDTH), 0f)
        assertEquals(360f, m.axisDp(com.appdimens.games.common.DpQualifier.WIDTH), 0f)
        assertEquals(800f, m.axisDp(com.appdimens.games.common.DpQualifier.HEIGHT), 0f)
    }

    @Test fun equality_is_value_based_but_identity_partitions_cache() {
        val a = GameMetrics.of(360, 800, densityDpi = 440)
        val b = GameMetrics.of(360, 800, densityDpi = 440)
        assertEquals(a, b)
        assertNotEquals(a, GameMetrics.of(361, 800))
        assertFalse(a == null)
    }

    @Test fun default_reference_window_is_canonical() {
        val d = GameMetrics.DEFAULT
        assertEquals(300, d.screenWidthDp)
        assertEquals(533, d.screenHeightDp)
        assertEquals(160, d.densityDpi)
        assertTrue(d.isFullscreen)
        assertEquals(1f, d.scale, 0f)
    }
}
