package com.appdimens.games.resize

import android.content.Context
import com.appdimens.games.core.GameScreen
import kotlin.math.min

/**
 * [EN] Container-aware auto-fit extensions for game HUD (dynamic score bars,
 * variable text, adaptive panels). Binary-search fitting — O(log n).
 *
 * [PT] Extensões de auto-fit por contêiner para HUD de jogo (barras de placar
 * dinâmicas, texto variável, painéis adaptativos). Busca binária — O(log n).
 *
 * ```kotlin
 * val side = 64f.autoFitSquarePx(context, containerW, containerH, minDp = 24f, maxDp = 96f)
 * ```
 */
object DimenGameResize {

    /** Largest square side in px that fits the inner box. */
    @JvmStatic
    @JvmOverloads
    fun fittingSquareSidePx(
        context: Context?,
        boxWidthPx: Float,
        boxHeightPx: Float,
        paddingPx: Float = 0f,
        minDp: Float = 8f,
        maxDp: Float = 128f,
        stepDp: Float = 2f,
    ): Float {
        val density = GameScreen.metrics().density
        val (iw, ih) = ResizeMath.innerMaxDimensionsPx(boxWidthPx, boxHeightPx, paddingPx, paddingPx, paddingPx, paddingPx)
        val steps = ResizeMath.buildResizeStepsPx(minDp * density, maxDp * density, stepDp * density)
        return ResizeMath.findLargestFittingResizePx(steps) { it <= min(iw, ih) }
    }

    /** Largest width in px that fits. */
    @JvmStatic
    @JvmOverloads
    fun fittingWidthPx(
        context: Context?,
        boxWidthPx: Float,
        paddingHorizontalPx: Float = 0f,
        minDp: Float = 8f,
        maxDp: Float = 256f,
        stepDp: Float = 2f,
    ): Float {
        val density = GameScreen.metrics().density
        val iw = maxOf(1f, boxWidthPx - paddingHorizontalPx * 2)
        val steps = ResizeMath.buildResizeStepsPx(minDp * density, maxDp * density, stepDp * density)
        return ResizeMath.findLargestFittingResizePx(steps) { it <= iw }
    }

    /** Largest height in px that fits. */
    @JvmStatic
    @JvmOverloads
    fun fittingHeightPx(
        context: Context?,
        boxHeightPx: Float,
        paddingVerticalPx: Float = 0f,
        minDp: Float = 8f,
        maxDp: Float = 256f,
        stepDp: Float = 2f,
    ): Float {
        val density = GameScreen.metrics().density
        val ih = maxOf(1f, boxHeightPx - paddingVerticalPx * 2)
        val steps = ResizeMath.buildResizeStepsPx(minDp * density, maxDp * density, stepDp * density)
        return ResizeMath.findLargestFittingResizePx(steps) { it <= ih }
    }

    /** Percent-of-box based range resolution (family parity). */
    @JvmStatic
    fun percentRangePx(boxPx: Float, minPercent: Float, maxPercent: Float, stepPercent: Float): FloatArray {
        val f = ResizeMath.percentOfBoxToFactor(maxPercent) - ResizeMath.percentOfBoxToFactor(minPercent)
        return ResizeMath.buildResizeStepsPx(
            boxPx * ResizeMath.percentOfBoxToFactor(minPercent),
            boxPx * ResizeMath.percentOfBoxToFactor(maxPercent),
            boxPx * f * stepPercent / 100f
        )
    }
}
