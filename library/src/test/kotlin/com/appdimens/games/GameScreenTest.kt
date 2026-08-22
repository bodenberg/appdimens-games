package com.appdimens.games

import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Inverter
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [EN] Live hub semantics: publish/freeze, inverter resolution, multi-window heuristic.
 * [PT] Semântica do hub vivo: publicação/congelamento, resolução de inversores,
 * heurística de multi-janela.
 */
class GameScreenTest {

    @After fun tearDown() = GameScreen.resetForTests()

    private fun land() = GameMetrics.of(800, 360, smallestWidthDp = 360)
    private fun port() = GameMetrics.of(360, 800, smallestWidthDp = 360)

    // ─── Inverters ──────────────────────────────────────────────────────────

    @Test fun default_inverter_never_changes_qualifier() {
        GameScreen.update(port())
        for (q in DpQualifier.entries) {
            assertEquals(q, GameScreen.effectiveQualifier(q, Inverter.DEFAULT, true, true))
        }
    }

    @Test fun ph_to_lw_swaps_height_to_width_only_in_landscape() {
        assertEquals(
            DpQualifier.WIDTH,
            GameScreen.effectiveQualifier(DpQualifier.HEIGHT, Inverter.PH_TO_LW, landscape = true, portrait = false)
        )
        assertEquals(
            DpQualifier.HEIGHT,
            GameScreen.effectiveQualifier(DpQualifier.HEIGHT, Inverter.PH_TO_LW, landscape = false, portrait = true)
        )
    }

    @Test fun pw_to_lh_swaps_width_to_height_only_in_landscape() {
        assertEquals(
            DpQualifier.HEIGHT,
            GameScreen.effectiveQualifier(DpQualifier.WIDTH, Inverter.PW_TO_LH, landscape = true, portrait = false)
        )
        assertEquals(
            DpQualifier.WIDTH,
            GameScreen.effectiveQualifier(DpQualifier.WIDTH, Inverter.PW_TO_LH, landscape = false, portrait = true)
        )
    }

    @Test fun lh_to_pw_and_lw_to_ph_act_in_portrait() {
        assertEquals(
            DpQualifier.WIDTH,
            GameScreen.effectiveQualifier(DpQualifier.HEIGHT, Inverter.LH_TO_PW, landscape = false, portrait = true)
        )
        assertEquals(
            DpQualifier.HEIGHT,
            GameScreen.effectiveQualifier(DpQualifier.WIDTH, Inverter.LW_TO_PH, landscape = false, portrait = true)
        )
        // no-op in landscape
        assertEquals(
            DpQualifier.HEIGHT,
            GameScreen.effectiveQualifier(DpQualifier.HEIGHT, Inverter.LH_TO_PW, landscape = true, portrait = false)
        )
    }

    @Test fun sw_inverters_resolve_per_orientation() {
        // Landscape
        assertEquals(DpQualifier.HEIGHT, GameScreen.effectiveQualifier(DpQualifier.SMALL_WIDTH, Inverter.SW_TO_LH, true, false))
        assertEquals(DpQualifier.WIDTH, GameScreen.effectiveQualifier(DpQualifier.SMALL_WIDTH, Inverter.SW_TO_LW, true, false))
        // Portrait
        assertEquals(DpQualifier.HEIGHT, GameScreen.effectiveQualifier(DpQualifier.SMALL_WIDTH, Inverter.SW_TO_PH, false, true))
        assertEquals(DpQualifier.WIDTH, GameScreen.effectiveQualifier(DpQualifier.SMALL_WIDTH, Inverter.SW_TO_PW, false, true))
        // No-op when orientation doesn't match
        assertEquals(DpQualifier.SMALL_WIDTH, GameScreen.effectiveQualifier(DpQualifier.SMALL_WIDTH, Inverter.SW_TO_LH, false, true))
    }

    // ─── Publish / freeze (`i`) ─────────────────────────────────────────────

    @Test fun update_publishes_live_snapshot_and_freezes_fullscreen() {
        GameScreen.update(port())
        val live1 = GameScreen.metrics()
        assertTrue(live1.isFullscreen)
        assertEquals(live1, GameScreen.invariantMetrics())

        GameScreen.update(GameMetrics.of(600, 500, isFullscreen = false))
        assertEquals(600, GameScreen.metrics().screenWidthDp)
        // invariant path still anchored to the last fullscreen snapshot
        assertEquals(live1, GameScreen.invariantMetrics())
    }

    @Test fun non_fullscreen_snapshot_is_not_frozen_as_anchor() {
        GameScreen.update(GameMetrics.of(200, 400, isFullscreen = false))
        // never had a fullscreen anchor → DEFAULT reference
        assertEquals(GameMetrics.DEFAULT, GameScreen.invariantMetrics())
    }

    @Test fun listeners_fire_on_update_and_can_unregister() {
        var count = 0
        val handle = GameScreen.addListener { count++ }
        GameScreen.update(port())
        assertEquals(1, count)
        handle()
        GameScreen.update(land())
        assertEquals(1, count)
    }

    @Test fun metrics_object_identity_changes_per_publish() {
        GameScreen.update(port())
        val first = GameScreen.metrics()
        GameScreen.update(port())
        assertFalse(first === GameScreen.metrics())
    }
}
