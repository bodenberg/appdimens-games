package com.appdimens.games.compat

import android.content.Context
import androidx.annotation.Keep
import com.appdimens.games.code.sdpa

/**
 * [EN] DEPRECATED 2.0.1 migration shim. Maps the old gateway names to the unified
 * 3.x family API. New code must use the standard surface (`DimenSdp`, extensions,
 * `scaledDp()` builder, satellites).
 *
 * [PT] Shim de migração da 2.0.1 (DEPRECATED). Mapeia os nomes antigos do gateway
 * para a API unificada 3.x da família. Código novo deve usar a superfície padrão.
 */
@Deprecated("Migrated to the 3.x family API — see DOCUMENTATION/MODULES.md")
@Keep
object GamesCompat {

    @Deprecated("Use GameScreen.updateFromContext(context)", ReplaceWith("GameScreen.updateFromContext(context)"))
    fun initialize(context: Context) {
        com.appdimens.games.core.GameScreen.updateFromContext(context)
    }

    @Deprecated("Use DimenSdp.sdp / scaledDp().sdp (DEFAULT strategy ≈ sdp family)")
    fun calculateButtonSize(baseDp: Float, context: Context? = null): Float =
        baseDp.toInt().sdpa(context)

    @Deprecated("Use library-auto `asdp` extension: baseDp.asdp(context)")
    fun calculatePlayerSize(baseDp: Float, context: Context? = null): Float =
        baseDp.toAuto(context)

    @Deprecated("Use library-auto `asdp` extension")
    fun calculateEnemySize(baseDp: Float, context: Context? = null): Float =
        baseDp.toAuto(context)

    @Deprecated("Use `psdp` extension (percent module): baseDp.psdp(context)")
    fun calculateWorldSize(baseDp: Float, context: Context? = null): Float =
        baseDp.toPercent(context)

    @Deprecated("Use library-fill `flsdp` extension")
    fun calculateBackgroundSize(baseDp: Float, context: Context? = null): Float =
        baseDp.toFillCompat(context)

    @Deprecated("Use library-fit `ftsdp` extension")
    fun calculateViewportSize(baseDp: Float, context: Context? = null): Float =
        baseDp.toFitCompat(context)

    @Deprecated("Use library-diagonal `dgsdp` extension")
    fun calculateTouchTarget(baseDp: Float, context: Context? = null): Float =
        baseDp.toDiagCompat(context)
}

// Internal bridges keep compat decoupled from satellite modules:
internal fun Float.toAuto(context: Context?): Float {
    val m = com.appdimens.games.core.GameScreen.metrics()
    return com.appdimens.games.math.GameMath.toPx(com.appdimens.games.math.GameMath.calculateAutoDp(this, m), m)
}
private fun Float.toPercent(context: Context?): Float {
    val m = com.appdimens.games.core.GameScreen.metrics()
    return com.appdimens.games.math.GameMath.toPx(com.appdimens.games.math.GameMath.calculatePercentDp(this, m), m)
}
private fun Float.toFillCompat(context: Context?): Float {
    val m = com.appdimens.games.core.GameScreen.metrics()
    return com.appdimens.games.math.GameMath.toPx(com.appdimens.games.math.GameMath.calculateFillDp(this, m), m)
}
private fun Float.toFitCompat(context: Context?): Float {
    val m = com.appdimens.games.core.GameScreen.metrics()
    return com.appdimens.games.math.GameMath.toPx(com.appdimens.games.math.GameMath.calculateFitDp(this, m), m)
}
private fun Float.toDiagCompat(context: Context?): Float {
    val m = com.appdimens.games.core.GameScreen.metrics()
    return com.appdimens.games.math.GameMath.toPx(com.appdimens.games.math.GameMath.calculateDiagonalDp(this, m), m)
}
