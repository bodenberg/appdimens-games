package com.example.appdimens.games.sample

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appdimens.games.core.GameScreen
import com.appdimens.games.resize.DimenGameResize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test that validates the library works correctly with ANY
 * AndroidX Compose BOM version.
 *
 * This test specifically catches the "module not found" / class resolution
 * crash that occurred when the consumer used a different Compose version
 * than the one the library was compiled against.
 *
 * Run on an emulator/device with:
 *   ./gradlew :sample:connectedAndroidTest
 *
 * To validate cross-BOM compatibility, change the Compose BOM version
 * in the sample's build.gradle.kts (e.g., to 2026.08.00 or an older version)
 * and re-run. The test must pass with ANY Compose 1.x BOM version.
 */
@RunWith(AndroidJUnit4::class)
class ComposeBomCompatibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        GameScreen.updateFromContext(context)
    }

    @Test
    fun libraryResize_functionsDoNotCrash_withAnyComposeBom() {
        // These functions reference Compose types internally (e.g., Dp).
        // If the Compose BOM version is incompatible, class resolution fails
        // at call time with NoClassDefFoundError / NoSuchMethodError.
        val boxW = 1080f
        val boxH = 1920f

        val square = DimenGameResize.fittingSquareSidePx(
            context = context,
            boxWidthPx = boxW,
            boxHeightPx = boxH,
            paddingPx = 0f,
            minDp = 24f,
            maxDp = 96f,
            stepDp = 2f
        )
        assertTrue("fittingSquareSidePx must return positive value", square > 0f)

        val width = DimenGameResize.fittingWidthPx(
            context = context,
            boxWidthPx = boxW,
            paddingHorizontalPx = 0f,
            minDp = 8f,
            maxDp = 256f,
            stepDp = 2f
        )
        assertTrue("fittingWidthPx must return positive value", width > 0f)

        val height = DimenGameResize.fittingHeightPx(
            context = context,
            boxHeightPx = boxH,
            paddingVerticalPx = 0f,
            minDp = 8f,
            maxDp = 256f,
            stepDp = 2f
        )
        assertTrue("fittingHeightPx must return positive value", height > 0f)

        val range = DimenGameResize.percentRangePx(
            boxPx = boxW,
            minPercent = 10f,
            maxPercent = 90f,
            stepPercent = 10f
        )
        assertEquals("percentRangePx must return 9 steps", 9, range.size)
        assertTrue("First step must be > 0", range[0] > 0f)
    }

    @Test
    fun gameScreen_metricsAreValid_afterResize() {
        val m = GameScreen.metrics()
        assertTrue("density must be > 0", m.density > 0f)
        assertTrue("screenWidthDp must be > 0", m.screenWidthDp > 0)
        assertTrue("screenHeightDp must be > 0", m.screenHeightDp > 0)
        assertTrue("smallestScreenWidthDp must be > 0", m.smallestScreenWidthDp > 0)
        assertFalse("isFullscreen should not be false by default on emulator", m.isFullscreen)
    }

    @Test
    fun composeExtensions_areResolvable_atRuntime() {
        // This test validates that Compose extension properties (sdp, asdp, etc.)
        // can be resolved at runtime regardless of the Compose BOM version used.
        // A crash here indicates binary incompatibility.
        composeTestRule.setContent {
            val padding = 12.sdp
            val size = 48.asdp
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.padding(padding).then(
                    androidx.compose.ui.Modifier.size(size)
                )
            ) {
                androidx.compose.material3.Text("OK")
            }
        }

        composeTestRule.onNodeWithText("OK").assertExists()
    }
}
