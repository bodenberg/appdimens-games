package com.appdimens.games

import com.appdimens.games.code.DimenSdp
import com.appdimens.games.code.hdp
import com.appdimens.games.code.scaledDp
import com.appdimens.games.code.sdpa
import com.appdimens.games.code.sdp
import com.appdimens.games.code.sdpi
import com.appdimens.games.code.sdpMode
import com.appdimens.games.code.sdpQualifier
import com.appdimens.games.code.sdpRotate
import com.appdimens.games.code.wdp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Orientation
import com.appdimens.games.common.UiModeType
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [EN] Code-side API (context is optional/unused on JVM): extensions, builder priority,
 * facilitators, Java facade and invariant semantics.
 * [PT] API code (contexto opcional/não usado na JVM): extensões, prioridade do builder,
 * facilitadores, fachada Java e semântica invariante.
 */
class DimenCodeApiTest {

    @After fun tearDown() = GameScreen.resetForTests()

    private fun publishFullscreen(w: Int, h: Int, densityDpi: Int = 160): GameMetrics =
        GameMetrics.of(w, h, densityDpi = densityDpi).also { GameScreen.update(it) }

    // ─── Extensions (px out) ────────────────────────────────────────────────

    @Test fun sdp_returns_px_of_sw_scaled_value() {
        val m = publishFullscreen(360, 800, densityDpi = 440) // density 2.75
        // 16dp × (360/300) × 2.75 = 52.8px
        assertEquals(16f * m.scale * m.density, 16.sdp(null), 1e-3f)
    }

    @Test fun sdpa_applies_default_ar_multiplier() {
        val m = publishFullscreen(360, 640, densityDpi = 160)
        assertEquals(16f * m.defaultScaledAspectRatioMultiplier * m.density, 16.sdpa(null), 1e-4f)
    }

    @Test fun hdp_uses_height_axis() {
        val m = publishFullscreen(360, 800, densityDpi = 160)
        assertEquals(32f * (800f / 300f) * m.density, 32.hdp(null), 1e-3f)
    }

    @Test fun wdp_uses_width_axis() {
        val m = publishFullscreen(800, 360, densityDpi = 160)
        assertEquals(32f * (800f / 300f) * m.density, 32.wdp(null), 1e-3f)
    }

    @Test fun sdp_invariant_follows_frozen_fullscreen_anchor() {
        publishFullscreen(360, 800)
        val anchor = GameScreen.invariantMetrics()
        // enter split-screen (non fullscreen)
        GameScreen.update(GameMetrics.of(300, 500, isFullscreen = false))
        assertEquals(48f * anchor.scale * anchor.density, 48.sdpi(null), 1e-3f)
        // live path follows the resized window instead
        assertEquals(48f * GameScreen.metrics().scale * GameScreen.metrics().density, 48.sdp(null), 1e-3f)
    }

    @Test fun sdp_invariant_returns_base_px_when_never_fullscreen() {
        GameScreen.update(GameMetrics.of(200, 400, isFullscreen = false))
        // constrained and no frozen anchor → base value × density of live window
        val m = GameScreen.metrics()
        assertEquals(48f * m.density, 48.sdpi(null), 1e-3f)
    }

    // ─── Builder priorities ────────────────────────────────────────────────

    @Test fun builder_qualifier_override_wins_over_scaling() {
        publishFullscreen(600, 960)
        val px = 100.scaledDp()
            .qualifier(DpQualifier.SMALL_WIDTH, 600, 24)
            .sdp(null)
        // override base 24 → 24 × sw-scale(2.0) × density(1.0)
        assertEquals(48f, px, 1e-4f)
    }

    @Test fun builder_orientation_override_applies_in_landscape() {
        publishFullscreen(960, 600)
        val px = 10.scaledDp().orientation(Orientation.LANDSCAPE, 40).sdp(null)
        assertEquals(80f, px, 1e-4f) // 40 × 2.0
        publishFullscreen(600, 960)
        val portraitPx = 10.scaledDp().orientation(Orientation.LANDSCAPE, 40).sdp(null)
        assertEquals(20f, portraitPx, 1e-4f) // falls back to receiver 10 × 2.0
    }

    @Test fun builder_mode_qualifier_has_top_priority() {
        publishFullscreen(600, 960)
        val px = 100.scaledDp()
            .screen(UiModeType.NORMAL, DpQualifier.SMALL_WIDTH, 600, 8)   // priority 1
            .qualifier(DpQualifier.SMALL_WIDTH, 600, 24)                  // priority 3
            .sdp(null)
        assertEquals(8f * 2f, px, 1e-4f)
    }

    // ─── Facilitators ──────────────────────────────────────────────────────

    @Test fun sdp_rotate_picks_orientation_value() {
        publishFullscreen(960, 600)
        assertEquals(44f * 2f, 30f.sdpRotate(null, 44f, Orientation.LANDSCAPE), 1e-4f)
        publishFullscreen(600, 960)
        assertEquals(30f * 2f, 30f.sdpRotate(null, 44f, Orientation.LANDSCAPE), 1e-4f)
    }

    @Test fun sdp_mode_picks_tv_override() {
        publishFullscreen(600, 960)
        assertEquals(30f * 2f, 30f.sdpMode(null, 24f, UiModeType.TELEVISION), 1e-4f)
        GameScreen.update(
            GameMetrics.of(960, 540, smallestWidthDp = 540, uiMode = UiModeType.TELEVISION)
        )
        assertEquals(24f * 1.8f, 30f.sdpMode(null, 24f, UiModeType.TELEVISION), 1e-3f)
    }

    @Test fun sdp_qualifier_threshold_selects_override() {
        publishFullscreen(600, 960)
        assertEquals(120f * 2f, 60f.sdpQualifier(null, 120f, DpQualifier.SMALL_WIDTH, 600), 1e-4f)
    }

    // ─── Java facade ───────────────────────────────────────────────────────

    @Test fun facade_matches_extensions() {
        val m = publishFullscreen(360, 800, densityDpi = 320)
        assertEquals(48.sdp(null), DimenSdp.sdp(null, 48), 0f)
        assertEquals(48.sdpi(null), DimenSdp.sdpi(null, 48), 0f)
        assertEquals(
            DimenSdp.getDimensionInPx(null, DpQualifier.SMALL_WIDTH, 16),
            16.toFloat() * m.scale * m.density,
            1e-3f
        )
        DimenSdp.warmupCache() // must not throw on JVM
    }
}
