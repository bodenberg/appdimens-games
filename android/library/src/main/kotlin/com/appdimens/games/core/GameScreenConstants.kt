package com.appdimens.games.core

/**
 * [EN] Canonical design-scale constants shared by the whole AppDimens family
 * (dynamic 3.x / KMP 1.x / games 3.0). Do not change — parity is bit-exact.
 *
 * [PT] Constantes canônicas de escala compartilhadas por toda a família AppDimens
 * (dynamic 3.x / KMP 1.x / games 3.0). Não alterar — a paridade é bit-exata.
 */
object GameScreenConstants {
    /** Reference base width in dp (W₀). */
    const val BASE_WIDTH_DP = 300f

    /** Reference base height in dp (H₀). */
    const val BASE_HEIGHT_DP = 533f

    /** √(300² + 533²) — reference diagonal. */
    const val BASE_DIAGONAL_DP = 611.6305f

    /** 300 + 533 — reference perimeter. */
    const val BASE_PERIMETER_DP = 833f

    /** Reference aspect ratio (16:9). */
    const val REFERENCE_ASPECT_RATIO = 1.78f

    /** 1/1.78 pre-inverted. */
    const val INV_REFERENCE_ASPECT_RATIO = 0.5617978f

    /** 1/300 pre-inverted (INV_BASE_RATIO). */
    const val INV_BASE_RATIO = 0.0033333334f

    /** 0.10/30 — linear increment factor of the AR-aware scaled path. */
    const val ADJUSTMENT_SCALE = 0.10f / 30f

    /** 0.08/30 — default aspect-ratio sensitivity (K). */
    const val SENSITIVITY_DEFAULT = 0.08f / 30f

    /** FLUID band lower bound. */
    const val FLUID_MIN_WIDTH_DP = 320f

    /** FLUID band upper bound. */
    const val FLUID_MAX_WIDTH_DP = 768f

    /** AUTO transition point between linear and log segments. */
    const val AUTO_TRANSITION_DP = 480f

    /** AUTO/LOGARITHMIC default sensitivity. */
    const val SENSITIVITY_LOG = 0.4f

    /** POWER default exponent (Stevens). */
    const val POWER_EXPONENT_DEFAULT = 0.75f
}
