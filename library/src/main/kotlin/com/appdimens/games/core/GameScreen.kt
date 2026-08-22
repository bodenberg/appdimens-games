package com.appdimens.games.core

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.UiContext
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Inverter
import com.appdimens.games.common.Orientation
import com.appdimens.games.common.UiModeType

/**
 * [EN] Live screen state hub for game engines. Publishes an immutable [GameMetrics]
 * snapshot atomically on every window resize / configuration change, so every
 * subsequent calculation auto-adjusts. Variant APIs with the `i` suffix bypass the
 * live window and anchor to the last FULLSCREEN snapshot (frozen) — or return the
 * base value under true multi-window — keeping family semantics.
 *
 * [PT] Hub de estado ao vivo da tela para engines de jogos. Publica um snapshot
 * imutável [GameMetrics] atomicamente a cada redimensionamento de janela/mudança de
 * configuração, para que todos os cálculos seguintes se ajustem automaticamente.
 * As variantes com sufixo `i` ignoram a janela ao vivo e ancoram no último snapshot
 * FULLSCREEN (congelado) — ou devolvem o valor base em multi-janela real — mantendo
 * a semântica da família.
 *
 * Wire-up (non-Compose engines):
 * ```kotlin
 * // Activity / SurfaceView:
 * override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
 *     GameScreen.updateFromContext(this, fullscreen = true)
 * }
 * override fun onConfigurationChanged(c: Configuration) {
 *     super.onConfigurationChanged(c)
 *     GameScreen.updateFromContext(this)
 * }
 * ```
 */
object GameScreen {

    @Volatile
    private var current: GameMetrics = GameMetrics.DEFAULT

    /** Frozen FULLSCREEN metrics used by `i` variants. / Métricas FULLSCREEN congeladas p/ sufixo `i`. */
    @Volatile
    private var frozenFullscreen: GameMetrics = GameMetrics.DEFAULT

    private val listeners = ArrayList<() -> Unit>(4)

    /** Current live snapshot. / Snapshot atual. */
    @JvmStatic
    fun metrics(): GameMetrics = current

    /** Metrics used by invariant (`i`) paths. / Métricas usadas pelos caminhos invariantes (`i`). */
    @JvmStatic
    fun invariantMetrics(): GameMetrics {
        val live = current
        if (live.isFullscreen) return live
        return frozenFullscreen
    }

    /**
     * [EN] Publishes a new snapshot. Listeners and cache partitions react automatically.
     * [PT] Publica um novo snapshot. Listeners e partições de cache reagem automaticamente.
     */
    @JvmStatic
    fun update(metrics: GameMetrics) {
        current = metrics
        if (metrics.isFullscreen && metrics.minDimensionDp > 0f) frozenFullscreen = metrics
        GameCache.onMetricsChanged(metrics)
        synchronized(listeners) { listeners.forEach { it() } }
    }

    /**
     * [EN] Rebuilds the snapshot from Android resources (dp units). Fullscreen is
     * detected by comparing the window configuration against the real display size
     * (split-screen/freeform windows are smaller than the display).
     * [PT] Reconstrói o snapshot a partir dos resources Android (unidades dp).
     * Fullscreen é detectado comparando a configuração da janela com o tamanho real
     * do display (janelas split-screen/freeform são menores que o display).
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun updateFromContext(@UiContext context: Context, fullscreen: Boolean? = null): GameMetrics {
        val cfg = context.resources.configuration
        val dm = context.resources.displayMetrics

        val real = android.util.DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(real)

        val windowW = dm.widthPixels.toFloat()
        val windowH = dm.heightPixels.toFloat()
        val coversDisplay = real.widthPixels <= 0 || real.heightPixels <= 0 ||
            (windowW >= real.widthPixels - 1f && windowH >= real.heightPixels - 1f)

        val fs = fullscreen ?: (coversDisplay && !isMultiWindowLikely(cfg))
        val m = GameMetrics(
            screenWidthDp = cfg.screenWidthDp,
            screenHeightDp = cfg.screenHeightDp,
            smallestScreenWidthDp = cfg.smallestScreenWidthDp,
            densityDpi = cfg.densityDpi,
            fontScaleBits = cfg.fontScale.toRawBits(),
            uiMode = uiModeFrom(cfg),
            isFullscreen = fs
        )
        update(m)
        return m
    }

    /** Registers a resize listener. Returns an unregister handle. / Registra listener de resize. */
    @JvmStatic
    fun addListener(listener: () -> Unit): () -> Unit {
        synchronized(listeners) { listeners.add(listener) }
        return { synchronized(listeners) { listeners.remove(listener) } }
    }

    /** Resets to reference window (tests only). / Reseta para a janela de referência (testes). */
    @JvmStatic
    internal fun resetForTests() {
        current = GameMetrics.DEFAULT
        frozenFullscreen = GameMetrics.DEFAULT
        GameCache.clearAll()
    }

    // ─── Plumbing ──────────────────────────────────────────────────────────

    /** Resolves the effective qualifier after inverter application. */
    fun effectiveQualifier(qualifier: DpQualifier, inverter: Inverter, landscape: Boolean, portrait: Boolean): DpQualifier {
        var q = qualifier
        when (inverter) {
            Inverter.PH_TO_LW -> if (landscape && qualifier == DpQualifier.HEIGHT) q = DpQualifier.WIDTH
            Inverter.PW_TO_LH -> if (landscape && qualifier == DpQualifier.WIDTH) q = DpQualifier.HEIGHT
            Inverter.LH_TO_PW -> if (portrait && qualifier == DpQualifier.HEIGHT) q = DpQualifier.WIDTH
            Inverter.LW_TO_PH -> if (portrait && qualifier == DpQualifier.WIDTH) q = DpQualifier.HEIGHT
            Inverter.SW_TO_LH -> if (landscape && qualifier == DpQualifier.SMALL_WIDTH) q = DpQualifier.HEIGHT
            Inverter.SW_TO_LW -> if (landscape && qualifier == DpQualifier.SMALL_WIDTH) q = DpQualifier.WIDTH
            Inverter.SW_TO_PH -> if (portrait && qualifier == DpQualifier.SMALL_WIDTH) q = DpQualifier.HEIGHT
            Inverter.SW_TO_PW -> if (portrait && qualifier == DpQualifier.SMALL_WIDTH) q = DpQualifier.WIDTH
            Inverter.DEFAULT -> Unit
        }
        return q
    }

    /**
     * [EN] True when the app runs inside a resized container (split-screen/freeform).
     * Heuristic parity with Dynamic: `(sw − w) ≥ sw·0.1`.
     * [PT] Verdadeiro quando o app roda em contêiner redimensionado.
     */
    fun isMultiWindowLikely(configuration: Configuration): Boolean {
        val sw = configuration.smallestScreenWidthDp.toFloat()
        if (sw <= 0f) return false
        val cw = configuration.screenWidthDp.toFloat()
        return (sw - cw) >= (sw * 0.1f)
    }

    private fun uiModeFrom(cfg: Configuration): UiModeType =
        UiModeType.fromConfigValue(cfg.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK)
}
