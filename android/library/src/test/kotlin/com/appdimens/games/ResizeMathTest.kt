package com.appdimens.games

import com.appdimens.games.resize.ResizeMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [EN] Auto-fit math: step tables, binary search, inner box, percent factors.
 * [PT] Matemática de auto-fit: tabelas de passos, busca binária, caixa interna,
 * fatores percentuais.
 */
class ResizeMathTest {

    @Test fun step_table_ascending_and_epsilon_safe() {
        val steps = ResizeMath.buildResizeStepsPx(8f, 128f, 2f)
        assertTrue(steps.size >= 2)
        assertEquals(8f, steps.first(), 0f)
        assertEquals(128f, steps.last(), 1e-3f)
        for (i in 1 until steps.size) {
            assertTrue("step $i not ascending", steps[i] > steps[i - 1])
            assertEquals(2f, steps[i] - steps[i - 1], 0.01f)
        }
    }

    @Test fun degenerate_step_table_returns_single_entry() {
        val s = ResizeMath.buildResizeStepsPx(10f, 10f, 2f)
        assertEquals(1, s.size)
        assertEquals(10f, s[0], 0f)
        // invalid step → [lo]
        val s2 = ResizeMath.buildResizeStepsPx(4f, 40f, 0f)
        assertEquals(1, s2.size)
        assertEquals(4f, s2[0], 0f)
    }

    @Test fun binary_search_finds_largest_fitting() {
        val steps = ResizeMath.buildResizeStepsPx(0f, 100f, 10f)
        assertEquals(100f, ResizeMath.findLargestFittingResizePx(steps) { it <= 105f }, 0f)
        assertEquals(80f, ResizeMath.findLargestFittingResizePx(steps) { it <= 85f }, 0f)
        assertEquals(0f, ResizeMath.findLargestFittingResizePx(steps) { false }, 0f)
    }

    @Test fun binary_search_handles_single_and_empty_tables() {
        assertEquals(5f, ResizeMath.findLargestFittingResizePx(floatArrayOf(5f)) { true }, 0f)
        assertEquals(0f, ResizeMath.findLargestFittingResizePx(floatArrayOf(5f)) { false }, 0f)
        assertEquals(0f, ResizeMath.findLargestFittingResizePx(FloatArray(0)) { true }, 0f)
    }

    @Test fun inner_box_applies_padding_with_min_guard() {
        val (w, h) = ResizeMath.innerMaxDimensionsPx(100f, 200f, 10f, 20f, 30f, 40f)
        assertEquals(60f, w, 0f)
        assertEquals(140f, h, 0f)
        val (w2, _) = ResizeMath.innerMaxDimensionsPx(10f, 200f, 50f, 0f, 50f, 0f)
        assertEquals(1f, w2, 0f) // never below 1px
    }

    @Test fun percent_factor_clamps_to_unit_interval() {
        assertEquals(0.5f, ResizeMath.percentOfBoxToFactor(50f), 0f)
        assertEquals(0f, ResizeMath.percentOfBoxToFactor(-5f), 0f)
        assertEquals(1f, ResizeMath.percentOfBoxToFactor(150f), 0f)
    }
}
