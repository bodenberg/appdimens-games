package com.appdimens.games.custom


import com.appdimens.games.common.UiModeType
import com.appdimens.games.core.GameMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMetricsBridgeTest {

    private val screens = listOf(
        GameMetrics.of(360, 780, 360, 420),
        GameMetrics.of(891, 411, 411, 420),
        GameMetrics.of(1280, 800, 800, 320),
        GameMetrics.of(240, 320, 240, 240),
    )

    @Test
    fun builtIns_matchGameMetricsFactors() {
        for (m in screens) {
            assertEquals(m.scale, BuiltInStrategies.SCALED.resolve(m).factor, 1e-6f)
            assertEquals(m.powerScale, BuiltInStrategies.POWER.resolve(m).factor, 1e-6f)
            assertEquals(m.logarithmicScale, BuiltInStrategies.LOGARITHMIC.resolve(m).factor, 1e-5f)
            assertEquals(m.interpolatedScale, BuiltInStrategies.INTERPOLATED.resolve(m).factor, 1e-6f)
            assertEquals(m.diagonalScale, BuiltInStrategies.DIAGONAL.resolve(m).factor, 1e-5f)
            assertEquals(m.perimeterScale, BuiltInStrategies.PERIMETER.resolve(m).factor, 1e-6f)
        }
    }

    @Test
    fun aspectRatio_matchesGameMultiplier() {
        val m = screens[1]
        val s = scalingStrategy("arParity", register = false) { curve = power(0.75f); aspectRatio() }
        assertEquals(m.powerScale * m.defaultAspectRatioMultiplier, s.resolve(m).factor, 1e-6f)
    }

    @Test
    fun snapshot_mapsGameUiModeOrientationAndFullscreen() {
        val tv = GameMetrics.of(960, 540, 540, 320, uiMode = UiModeType.TELEVISION).toScreenSnapshot()
        assertEquals(DeviceClass.TV, tv.device)
        assertTrue(tv.isLandscape)
        assertEquals(DeviceClass.WATCH, GameMetrics.of(192, 192, 192, uiMode = UiModeType.WATCH).toScreenSnapshot().device)
        assertEquals(DeviceClass.DESKTOP, GameMetrics.of(1280, 800, 800, uiMode = UiModeType.DESK).toScreenSnapshot().device)
        assertEquals(DeviceClass.TABLET, screens[2].toScreenSnapshot().device)
        assertEquals(DeviceClass.TABLET, GameMetrics.of(800, 1280, 800, uiMode = UiModeType.FOLD_OPEN).toScreenSnapshot().device)
        assertTrue(GameMetrics.of(400, 800, 800, isFullscreen = false).toScreenSnapshot().isMultiWindow)
        assertEquals(listOf("tv"), CustomPresets.TV_SAFE.resolve(GameMetrics.of(960, 540, 540, uiMode = UiModeType.TELEVISION)).appliedRules)
    }

    @Test
    fun resolve_isMemoizedPerSnapshot() {
        val s = scalingStrategy("memo", register = false) { curve = power(0.8f) }
        val m = screens[0]
        assertSame(s.resolve(m), s.resolve(m))
        assertSame(s.resolve(m), s.resolve(GameMetrics.of(360, 780, 360, 420)))
    }

    @Test
    fun ignoreMultiWindow_returnsBaseForResizedSurface() {
        val s = scalingStrategy("mw", register = false) { ignoreMultiWindow = true }
        assertEquals(16f, s.resolve(GameMetrics.of(400, 800, 800, isFullscreen = false)).scale(16f), 0f)
        assertEquals(16f * 800f / 300f, s.resolve(GameMetrics.of(800, 1280, 800)).scale(16f), 1e-4f)
    }
}
