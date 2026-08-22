package com.appdimens.games.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen

/**
 * [EN] CompositionLocal holding the current [GameMetrics]. Recomputed automatically
 * by [AppDimensGamesProvider] whenever the Compose configuration (window size,
 * density, orientation, font scale, uiMode) changes — this is the auto-resize
 * mechanism for Compose games.
 *
 * [PT] CompositionLocal que guarda o [GameMetrics] atual. Recalculado automaticamente
 * pelo [AppDimensGamesProvider] sempre que a configuração do Compose (tamanho da
 * janela, densidade, orientação, escala de fonte, uiMode) mudar — este é o mecanismo
 * de ajuste automático para jogos em Compose.
 */
val LocalGameMetrics = androidx.compose.runtime.compositionLocalOf { GameMetrics.DEFAULT }

/**
 * [EN] Provides the live game metrics to the composition tree.
 * [PT] Fornece as métricas vivas do jogo à árvore de composição.
 */
@Composable
fun AppDimensGamesProvider(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val metrics = remember(configuration, density.density) {
        GameMetrics(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            smallestScreenWidthDp = configuration.smallestScreenWidthDp,
            densityDpi = (density.density * 160f).toInt(),
            fontScaleBits = configuration.fontScale.toRawBits(),
            isFullscreen = !GameScreen.isMultiWindowLikely(configuration)
        )
    }
    androidx.compose.runtime.SideEffect { GameScreen.update(metrics) }
    CompositionLocalProvider(LocalGameMetrics provides metrics, content = content)
}

/** Current snapshot inside composition. / Snapshot atual dentro da composição. */
@Composable
@ReadOnlyComposable
fun currentGameMetrics(): GameMetrics = LocalGameMetrics.current

// ─── SCALED family (sdp/hdp/wdp + a/i/ia + Px) ─────────────────────────────

@Composable
private fun Number.dpScaled(f: (Float, GameMetrics) -> Float): Float {
    val m = LocalGameMetrics.current
    return f(toFloat(), m)
}

private fun Float.scaledOrBase(m: GameMetrics): Float =
    if (!m.isFullscreen) this else this * m.scale

private fun Float.scaledArOrBase(m: GameMetrics): Float =
    if (!m.isFullscreen) this else this * m.defaultScaledAspectRatioMultiplier

/** Scaled dp (smallest width). / Dp escalado (menor largura). */
@get:Composable
val Number.sdp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m -> b * m.scale })

/** Aspect-ratio aware. */
@get:Composable
val Number.sdpa: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m -> b * m.defaultScaledAspectRatioMultiplier })

/** Resize-invariant (`i`): frozen fullscreen reference under resized windows. */
@get:Composable
val Number.sdpi: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m -> b.scaledOrBase(m) })

/** Invariant + aspect ratio (`ia`). */
@get:Composable
val Number.sdpia: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m -> b.scaledArOrBase(m) })

/** Pixels. */
@get:Composable
val Number.sdpPx: Float
    get() = dpScaled { b, m -> b * m.scale * m.density }

@get:Composable
val Number.sdpiPx: Float
    get() = dpScaled { b, m -> b.scaledOrBase(m) * m.density }

// hdp (height)
@get:Composable
val Number.hdp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m -> b * m.screenHeightFactor })

@get:Composable
val Number.hdpi: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m -> if (!m.isFullscreen) b else b * m.screenHeightFactor })

@get:Composable
val Number.hdpPx: Float
    get() = dpScaled { b, m -> b * m.screenHeightFactor * m.density }

// wdp (width)
@get:Composable
val Number.wdp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m -> b * m.screenWidthFactor })

@get:Composable
val Number.wdpi: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m -> if (!m.isFullscreen) b else b * m.screenWidthFactor })

@get:Composable
val Number.wdpPx: Float
    get() = dpScaled { b, m -> b * m.screenWidthFactor * m.density }

// ─── Strategy shortcuts in composition (game-first API) ────────────────────

/** BALANCED strategy inside composition. */
@get:Composable
val Number.bdp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m ->
        com.appdimens.games.math.GameMath.calculateAutoDp(b, m)
    })

/** FIT strategy (letterbox world scale). */
@get:Composable
val Number.ftdp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m ->
        com.appdimens.games.math.GameMath.calculateFitDp(b, m)
    })

/** FILL strategy (cover). */
@get:Composable
val Number.fltdp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m ->
        com.appdimens.games.math.GameMath.calculateFillDp(b, m)
    })

/** DIAGONAL strategy. */
@get:Composable
val Number.dgtdp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(dpScaled { b, m ->
        com.appdimens.games.math.GameMath.calculateDiagonalDp(b, m)
    })
