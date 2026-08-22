package com.appdimens.games.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdimens.games.common.UiModeType
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen

/**
 * [EN] CompositionLocals — family parity with `AppDimensProvider` / `LocalDimenMetrics`
 * from appdimens-dynamic/kmp.
 *
 * [PT] CompositionLocals — paridade com `AppDimensProvider` / `LocalDimenMetrics`.
 */
val LocalDimenMetrics = compositionLocalOf { GameMetrics.DEFAULT }
val LocalUiModeType = compositionLocalOf { UiModeType.NORMAL }

/**
 * [EN] Provides live game metrics to the composition tree. Any window change
 * (resize, rotation, split-screen, font scale, density) recomposes automatically —
 * this is the auto-adjust mechanism for Compose games.
 *
 * [PT] Fornece as métricas vivas à árvore de composição. Qualquer mudança de janela
 * (resize, rotação, split-screen, escala de fonte, densidade) recomputa automaticamente.
 */
@Composable
fun AppDimensProvider(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val metrics = remember(configuration, density.density) {
        GameMetrics(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            smallestScreenWidthDp = configuration.smallestScreenWidthDp,
            densityDpi = (density.density * 160f).toInt(),
            fontScaleBits = configuration.fontScale.toRawBits(),
            uiMode = UiModeType.fromConfigValue(
                configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK
            ),
            isFullscreen = !GameScreen.isMultiWindowLikely(configuration)
        )
    }
    SideEffect { GameScreen.update(metrics) }
    CompositionLocalProvider(
        LocalDimenMetrics provides metrics,
        LocalUiModeType provides metrics.uiMode,
        content = content
    )
}

/** Current snapshot inside composition. / Snapshot atual na composição. */
@Composable
@ReadOnlyComposable
fun currentDimenMetrics(): GameMetrics = LocalDimenMetrics.current

// ─── Kernel plumbing ───────────────────────────────────────────────────────

@Composable
private fun Number.computeScaled(inv: Boolean, ar: Boolean): Float {
    val m = LocalDimenMetrics.current
    if (inv && !m.isFullscreen) return toFloat()
    return if (ar) toFloat() * m.defaultScaledAspectRatioMultiplier else toFloat() * m.scale
}

@Composable
private fun Number.computeAxis(inv: Boolean, height: Boolean): Float {
    val m = LocalDimenMetrics.current
    if (inv && !m.isFullscreen) return toFloat()
    return if (height) toFloat() * m.screenHeightFactor else toFloat() * m.screenWidthFactor
}

// ─── sdp family ────────────────────────────────────────────────────────────

/** Scaled dp by smallest width. / Dp escalado pela menor largura. */
@get:Composable
val Number.sdp: Dp get() = computeScaled(false, false).dp

/** Aspect-ratio refined (`a`). */
@get:Composable
val Number.sdpa: Dp get() = computeScaled(false, true).dp

/** Resize-invariant (`i`). */
@get:Composable
val Number.sdpi: Dp get() = computeScaled(true, false).dp

/** Invariant + AR (`ia`). */
@get:Composable
val Number.sdpia: Dp get() = computeScaled(true, true).dp

/** Pixels. */
@get:Composable
val Number.sdpPx: Float get() = computeScaled(false, false) * LocalDimenMetrics.current.density

@get:Composable
val Number.sdpaPx: Float get() = computeScaled(false, true) * LocalDimenMetrics.current.density

@get:Composable
val Number.sdpiPx: Float get() = computeScaled(true, false) * LocalDimenMetrics.current.density

// ─── hdp family ────────────────────────────────────────────────────────────

@get:Composable
val Number.hdp: Dp get() = computeAxis(false, true).dp

@get:Composable
val Number.hdpa: Dp
    get() = (computeAxis(false, true) * LocalDimenMetrics.current.defaultAspectRatioMultiplier).dp

@get:Composable
val Number.hdpi: Dp get() = computeAxis(true, true).dp

@get:Composable
val Number.hdpPx: Float get() = computeAxis(false, true) * LocalDimenMetrics.current.density

// ─── wdp family ────────────────────────────────────────────────────────────

@get:Composable
val Number.wdp: Dp get() = computeAxis(false, false).dp

@get:Composable
val Number.wdpa: Dp
    get() = (computeAxis(false, false) * LocalDimenMetrics.current.defaultAspectRatioMultiplier).dp

@get:Composable
val Number.wdpi: Dp get() = computeAxis(true, false).dp

@get:Composable
val Number.wdpPx: Float get() = computeAxis(false, false) * LocalDimenMetrics.current.density

// ─── ssp family (text; sp via Dp×fontScale handled by Compose sp) ──────────

private fun Float.spValue(m: GameMetrics): Float =
    this * m.scale // sp value in dp-scale terms; Compose applies fontScale on render

@get:Composable
val Number.ssp: androidx.compose.ui.unit.TextUnit
    get() {
        val m = LocalDimenMetrics.current
        return androidx.compose.ui.unit.TextUnit(toFloat() * m.scale, androidx.compose.ui.unit.TextUnitType.Sp)
    }

@get:Composable
val Number.sspi: androidx.compose.ui.unit.TextUnit
    get() {
        val m = LocalDimenMetrics.current
        val v = if (!m.isFullscreen) toFloat() else toFloat() * m.scale
        return androidx.compose.ui.unit.TextUnit(v, androidx.compose.ui.unit.TextUnitType.Sp)
    }

/** Fixed text (`sem`) — ignores system font scale at render time is not possible in sp;
 *  provide px instead for pixel-exact fixed text. */
@get:Composable
val Number.semPx: Float
    get() {
        val m = LocalDimenMetrics.current
        return toFloat() * m.scale * m.density
    }
