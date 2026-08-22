package com.appdimens.games.resize

import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [EN] Container-aware auto-fit extension tests (uses the live GameScreen hub).
 * [PT] Testes das extensões de auto-fit por contêiner (usa o hub GameScreen vivo).
 */
class DimenGameResizeTest {

    @After fun tearDown() = GameScreen.update(GameMetrics.DEFAULT)

    @Test fun fitting_square_never_exceeds_inner_box() {
        GameScreen.update(GameMetrics.of(360, 800, densityDpi = 440)) // density 2.75
        val side = DimenGameResize.fittingSquareSidePx(
            null, boxWidthPx = 500f, boxHeightPx = 300f,
            paddingPx = 10f, minDp = 8f, maxDp = 400f, stepDp = 2f
        )
        assertTrue(side > 0f)
        // inner box: 480×280 px → square side must not exceed 280px
        assertTrue(side <= 280f + 0.01f)
        // and should be a multiple of step (2dp × 2.75 = 5.5px) close to the max
        assertEquals(280f, side, 5.5f)
    }

    @Test fun fitting_width_respects_horizontal_padding() {
        GameScreen.update(GameMetrics.of(360, 800, densityDpi = 160))
        val w = DimenGameResize.fittingWidthPx(
            null, boxWidthPx = 400f, paddingHorizontalPx = 50f,
            minDp = 4f, maxDp = 1000f, stepDp = 1f
        )
        assertEquals(300f, w, 1f)
    }

    @Test fun fitting_height_respects_vertical_padding() {
        GameScreen.update(GameMetrics.of(360, 800, densityDpi = 160))
        val h = DimenGameResize.fittingHeightPx(
            null, boxHeightPx = 200f, paddingVerticalPx = 25f,
            minDp = 2f, maxDp = 1000f, stepDp = 1f
        )
        assertEquals(150f, h, 1f)
    }

    @Test fun percent_range_stays_within_box_bounds() {
        val range = DimenGameResize.percentRangePx(400f, minPercent = 10f, maxPercent = 90f, stepPercent = 10f)
        assertTrue(range.first() >= 40f - 0.01f)
        assertTrue(range.last() <= 360f + 0.01f)
        assertTrue(range.size >= 2)
    }
}
