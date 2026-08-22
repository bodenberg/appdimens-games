package com.appdimens.games.world

import com.appdimens.games.core.GameMetrics
import kotlin.math.max
import kotlin.math.min

/**
 * [EN] Game-exclusive world layer: vectors, rectangles, viewport modes and
 * world↔screen mapping. All operations are allocation-free (return inline values).
 *
 * [PT] Camada de mundo exclusiva de jogos: vetores, retângulos, modos de viewport e
 * mapeamento mundo↔tela. Todas as operações são sem alocação.
 */

/** [EN] 2D vector. [PT] Vetor 2D. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    operator fun div(s: Float) = Vec2(x / s, y / s)
    infix fun dot(o: Vec2) = x * o.x + y * o.y
    fun length() = kotlin.math.sqrt(x * x + y * y)
    companion object { val ZERO = Vec2(0f, 0f); val ONE = Vec2(1f, 1f) }
}

/** [EN] 3D vector. [PT] Vetor 3D. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun length() = kotlin.math.sqrt(x * x + y * y + z * z)
    companion object { val ZERO = Vec3(0f, 0f, 0f) }
}

/** [EN] Axis-aligned rectangle. [PT] Retângulo alinhado aos eixos. */
data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f
    infix fun contains(p: Vec2): Boolean = p.x in left..right && p.y in top..bottom
}

/**
 * [EN] Viewport scaling mode for game surfaces.
 * [PT] Modo de escalonamento de viewport para superfícies de jogo.
 */
enum class ViewportMode {
    /** Fit entire design inside the window (letterbox). */
    FIT_ALL,
    /** Fill window width, crop/extend height. */
    FIT_WIDTH,
    /** Fill window height, crop/extend width. */
    FIT_HEIGHT,
    /** Stretch to window (non-uniform). */
    STRETCH,
    /** Cover the window, crop overflow. */
    CROP
}

/**
 * [EN] Computed viewport transform for a given design size and window metrics.
 * [PT] Transformação de viewport calculada para um tamanho de design e janela.
 *
 * @property scaleX uniform horizontal scale (px per design unit)
 * @property scaleY uniform vertical scale
 * @property offsetX letterbox/pillarbox offset in px
 * @property offsetY letterbox/pillarbox offset in px
 */
data class ViewportTransform(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    /** Maps a design-space point to screen px. */
    fun apply(v: Vec2): Vec2 = Vec2(v.x * scaleX + offsetX, v.y * scaleY + offsetY)

    /** Inverse map: screen px → design space. */
    fun inverse(p: Vec2): Vec2 = Vec2((p.x - offsetX) / scaleX, (p.y - offsetY) / scaleY)

    companion object {
        /**
         * [EN] Computes the transform. Auto-updates because it derives from the live
         * [GameMetrics]; recompute on `GameScreen` resize listeners or per frame.
         * [PT] Calcula a transformação. Atualiza automaticamente pois deriva do
         * [GameMetrics] vivo; recalcule nos listeners de resize ou por frame.
         */
        @JvmStatic
        @JvmOverloads
        fun of(
            designWidthDp: Float,
            designHeightDp: Float,
            metrics: GameMetrics,
            mode: ViewportMode = ViewportMode.FIT_ALL,
        ): ViewportTransform {
            val wDp = metrics.screenWidthDp.coerceAtLeast(1).toFloat()
            val hDp = metrics.screenHeightDp.coerceAtLeast(1).toFloat()
            return when (mode) {
                ViewportMode.FIT_ALL -> {
                    val s = min(wDp / designWidthDp, hDp / designHeightDp)
                    ViewportTransform(s, s, (wDp - designWidthDp * s) * 0.5f, (hDp - designHeightDp * s) * 0.5f)
                }
                ViewportMode.CROP -> {
                    val s = max(wDp / designWidthDp, hDp / designHeightDp)
                    ViewportTransform(s, s, (wDp - designWidthDp * s) * 0.5f, (hDp - designHeightDp * s) * 0.5f)
                }
                ViewportMode.FIT_WIDTH -> {
                    val s = wDp / designWidthDp
                    ViewportTransform(s, s, 0f, (hDp - designHeightDp * s) * 0.5f)
                }
                ViewportMode.FIT_HEIGHT -> {
                    val s = hDp / designHeightDp
                    ViewportTransform(s, s, (wDp - designWidthDp * s) * 0.5f, 0f)
                }
                ViewportMode.STRETCH ->
                    ViewportTransform(wDp / designWidthDp, hDp / designHeightDp, 0f, 0f)
            }
        }
    }
}

/**
 * [EN] World-scale helpers for 2D/3D games: converts design/world units to screen px
 * with automatic adjustment on window resize (all factors derive from live metrics).
 *
 * [PT] Auxiliares de escala de mundo para jogos 2D/3D: converte unidades de
 * mundo/design para px de tela com ajuste automático no redimensionamento
 * (todos os fatores derivam das métricas vivas).
 */
object WorldScale {

    /** FIT factor in px per world unit (letterbox-consistent). */
    @JvmStatic fun fitPx(metrics: GameMetrics, designW: Float, designH: Float): Float =
        minOf(metrics.minDimensionDp / minOf(designW, designH),
              metrics.maxDimensionDp / maxOf(designW, designH)) * metrics.density

    /** FILL factor in px per world unit (cover). */
    @JvmStatic fun fillPx(metrics: GameMetrics, designW: Float, designH: Float): Float =
        maxOf(metrics.minDimensionDp / designW, metrics.maxDimensionDp / designH) * metrics.density

    /** Scales a point from world/design space into px using the FIT factor. */
    @JvmStatic fun toScreen(v: Vec2, metrics: GameMetrics, designW: Float, designH: Float): Vec2 {
        val s = fitPx(metrics, designW, designH) / metrics.density
        return v * s
    }

    /** Scales a whole rectangle. */
    @JvmStatic fun scaleRect(r: RectF, factor: Float): RectF =
        RectF(r.left * factor, r.top * factor, r.right * factor, r.bottom * factor)
}
