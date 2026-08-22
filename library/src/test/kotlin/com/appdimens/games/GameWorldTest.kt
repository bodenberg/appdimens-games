package com.appdimens.games

import com.appdimens.games.core.GameMetrics
import com.appdimens.games.units.PhysicalUnits
import com.appdimens.games.world.RectF
import com.appdimens.games.world.Vec2
import com.appdimens.games.world.ViewportMode
import com.appdimens.games.world.ViewportTransform
import com.appdimens.games.world.WorldScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [EN] Game world layer: vectors, rects, viewport letterbox/crop/stretch, world↔screen.
 * [PT] Camada de mundo: vetores, retângulos, viewport letterbox/crop/stretch,
 * mundo↔tela.
 */
class GameWorldTest {

    private val m = GameMetrics.of(600, 960, densityDpi = 320) // density 2.0

    // ─── Vec2 / RectF ───────────────────────────────────────────────────────

    @Test fun vec2_operators() {
        val v = Vec2(1f, 2f) + Vec2(3f, 4f)
        assertEquals(Vec2(4f, 6f), v)
        assertEquals(Vec2(-2f, -2f), v - Vec2(6f, 8f))
        assertEquals(Vec2(8f, 12f), v * 2f)
        assertEquals(Vec2(2f, 3f), v / 2f)
        assertEquals(52f, (v dot v), 1e-5f)
        assertEquals(kotlin.math.sqrt(52f), v.length(), 1e-4f)
    }

    @Test fun rect_contains_and_helpers() {
        val r = RectF(0f, 0f, 100f, 50f)
        assertEquals(100f, r.width, 0f)
        assertEquals(50f, r.height, 0f)
        assertEquals(50f, r.centerX, 0f)
        assertEquals(25f, r.centerY, 0f)
        assertTrue(r contains Vec2(50f, 25f))
        assertFalse(r contains Vec2(101f, 25f))
    }

    // ─── ViewportTransform ──────────────────────────────────────────────────

    @Test fun fit_all_letterboxes_and_centers() {
        // design 800×400 in a 600×960 window → s = min(0.75, 2.4) = 0.75
        val t = ViewportTransform.of(800f, 400f, m, ViewportMode.FIT_ALL)
        assertEquals(0.75f, t.scaleX, 1e-5f)
        assertEquals(0.75f, t.scaleY, 1e-5f)
        assertEquals((600f - 800f * 0.75f) / 2f, t.offsetX, 1e-4f)
        assertEquals((960f - 400f * 0.75f) / 2f, t.offsetY, 1e-4f)
    }

    @Test fun crop_covers_window() {
        val t = ViewportTransform.of(800f, 400f, m, ViewportMode.CROP)
        assertEquals(2.4f, t.scaleX, 1e-4f)
        assertEquals(2.4f, t.scaleY, 1e-4f)
    }

    @Test fun stretch_is_non_uniform_without_offsets() {
        val t = ViewportTransform.of(800f, 400f, m, ViewportMode.STRETCH)
        assertEquals(0.75f, t.scaleX, 1e-5f)
        assertEquals(2.4f, t.scaleY, 1e-5f)
        assertEquals(0f, t.offsetX, 0f)
        assertEquals(0f, t.offsetY, 0f)
    }

    @Test fun fit_width_and_height_anchor_one_axis() {
        val tw = ViewportTransform.of(800f, 400f, m, ViewportMode.FIT_WIDTH)
        assertEquals(0.75f, tw.scaleX, 1e-5f)
        assertEquals(0f, tw.offsetX, 0f)

        val th = ViewportTransform.of(800f, 400f, m, ViewportMode.FIT_HEIGHT)
        assertEquals(2.4f, th.scaleY, 1e-4f)
        assertEquals(0f, th.offsetY, 0f)
    }

    @Test fun apply_inverse_roundtrip() {
        val t = ViewportTransform.of(800f, 400f, m, ViewportMode.FIT_ALL)
        val p = Vec2(123.4f, 55.5f)
        val screen = t.apply(p)
        val back = t.inverse(screen)
        assertEquals(p.x, back.x, 1e-3f)
        assertEquals(p.y, back.y, 1e-3f)
    }

    // ─── WorldScale ─────────────────────────────────────────────────────────

    @Test fun world_scale_fit_px_is_letterbox_consistent() {
        // window 600×960, design 300×150 → s = min(600/300, 960/150) = 2 → px = 2 × density(2)
        assertEquals(4f, WorldScale.fitPx(m, 300f, 150f), 1e-4f)
        // matches ViewportTransform FIT_ALL exactly
        val t = ViewportTransform.of(300f, 150f, m, ViewportMode.FIT_ALL)
        assertEquals(t.scaleX * m.density, WorldScale.fitPx(m, 300f, 150f), 1e-5f)
    }

    @Test fun world_scale_fill_px_covers() {
        // max(600/300, 960/150) = 6.4 → ×density 2 = 12.8
        assertEquals(12.8f, WorldScale.fillPx(m, 300f, 150f), 1e-4f)
    }

    @Test fun to_screen_scales_point_in_design_units() {
        val p = WorldScale.toScreen(Vec2(10f, 20f), m, 300f, 150f)
        assertEquals(20f, p.x, 1e-4f)
        assertEquals(40f, p.y, 1e-4f)
    }

    @Test fun scale_rect_multiplies_all_edges() {
        val out = WorldScale.scaleRect(RectF(1f, 2f, 3f, 4f), 2f)
        assertEquals(RectF(2f, 4f, 6f, 8f), out)
    }
}
