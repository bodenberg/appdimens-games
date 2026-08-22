package com.appdimens.games.resize

import kotlin.math.max
import kotlin.math.min

/**
 * [EN] Container-aware auto-fit math (family parity with Dynamic's ResizeMath):
 * step table generation plus binary search for the largest fitting candidate — O(log n).
 *
 * [PT] Matemática de auto-fit por contêiner (paridade com o ResizeMath do Dynamic):
 * geração de tabela de passos e busca binária pelo maior candidato que cabe — O(log n).
 */
object ResizeMath {

    private const val MAX_RESIZE_STEPS = 4096

    /** Builds an ascending step table `[min..max]` with `step` spacing (epsilon-safe). */
    @JvmStatic
    fun buildResizeStepsPx(minPx: Float, maxPx: Float, stepPx: Float): FloatArray {
        val lo = min(minPx, maxPx)
        val hi = max(minPx, maxPx)
        if (stepPx <= 0f) return floatArrayOf(lo)
        val capacity = (((hi - lo) / stepPx).toInt() + 2).coerceIn(1, MAX_RESIZE_STEPS)
        val buf = FloatArray(capacity)
        var x = lo
        val epsilon = stepPx * 1e-4f
        var count = 0
        while (x <= hi + epsilon && count < capacity) {
            buf[count] = min(x, hi); x += stepPx; count++
        }
        if (count == 0) { buf[0] = lo; count = 1 }
        if (buf[count - 1] < hi - epsilon && count < capacity) { buf[count] = hi; count++ }
        return if (count == capacity) buf else buf.copyOf(count)
    }

    /** Largest step whose predicate returns true (binary search). */
    @JvmStatic
    fun findLargestFittingResizePx(sortedStepsPx: FloatArray, fits: (Float) -> Boolean): Float {
        if (sortedStepsPx.isEmpty()) return 0f
        if (sortedStepsPx.size == 1) return if (fits(sortedStepsPx[0])) sortedStepsPx[0] else 0f
        var left = 0
        var right = sortedStepsPx.size - 1
        var best = 0f
        while (left <= right) {
            val mid = (left + right) ushr 1
            val v = sortedStepsPx[mid]
            if (fits(v)) { best = v; left = mid + 1 } else { right = mid - 1 }
        }
        return best
    }

    /** Inner box after padding. / Caixa interna após padding. */
    @JvmStatic
    fun innerMaxDimensionsPx(boxW: Float, boxH: Float, padL: Float, padT: Float, padR: Float, padB: Float): Pair<Float, Float> =
        max(1f, boxW - padL - padR) to max(1f, boxH - padT - padB)

    /** Percent of box → 0..1 factor (clamped). */
    @JvmStatic
    fun percentOfBoxToFactor(percent: Float): Float =
        (percent / 100f).coerceIn(0f, 1f)
}
