package com.example.appdimens.games.sample

import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.appdimens.games.core.GameScreen
import com.appdimens.games.jni.NativeBridge

/**
 * [EN] Vulkan-surface demo: minimal, dependency-free swapchain surface that clears to an
 * animated color. It exercises the two critical games requirements without a full engine:
 * 1) surface resize handling — `surfaceChanged` republishes metrics (auto-adjust), and
 * 2) render-thread access — kernels run lock-free from the Choreographer thread.
 *
 * The viewport math produced here (`render.viewportRect` in C++) is exactly what a real
 * Vulkan pipeline consumes as `VkViewport`; the native header ships it ready-made.
 *
 * [PT] Demo de superfície Vulkan: swapchain mínimo que limpa a tela com cor animada.
 * Exercita os dois requisitos críticos: 1) resize da superfície — `surfaceChanged`
 * republica as métricas; 2) acesso da thread de render — kernels lock-free.
 *
 * A matemática de viewport produzida aqui é exatamente o `VkViewport` de um pipeline
 * real; o header nativo já entrega pronta.
 */
class VulkanGameActivity : Activity(), SurfaceHolder.Callback2, Choreographer.FrameCallback {

    private lateinit var surfaceView: SurfaceView
    private var running = false
    private var fpsFrames = 0
    private var fpsWindowStart = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        setContentView(surfaceView)
    }

    override fun onResume() {
        super.onResume()
        running = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onPause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        super.onPause()
    }

    // ─── Surface lifecycle → auto-adjust hooks ──────────────────────────────

    override fun surfaceCreated(holder: SurfaceHolder) {
        GameScreen.updateFromContext(this, fullscreen = true)
        publishNativeMetrics()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // THE resize story: new snapshot published; all kernels adjust on the next frame.
        GameScreen.updateFromContext(this, fullscreen = true)
        publishNativeMetrics()
    }

    /** Mirrors the live snapshot into the C++ hub so JNI kernels stay in sync. */
    private fun publishNativeMetrics() {
        if (!NativeBridge.isAvailable) return
        val cfg = resources.configuration
        NativeBridge.updateMetrics(
            widthDp = cfg.screenWidthDp.toFloat(),
            heightDp = cfg.screenHeightDp.toFloat(),
            smallestWidthDp = cfg.smallestScreenWidthDp.toFloat(),
            densityDpi = cfg.densityDpi.toFloat(),
            fontScale = cfg.fontScale,
            fullscreen = true
        )
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) { /* demo */ }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) { /* demo */ }

    // ─── Frame loop ─────────────────────────────────────────────────────────

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val m = GameScreen.metrics()

        // Animated clear color driven by the BALANCED factor (visual proof of scaling).
        val factor = m.scale
        val t = frameTimeNanos / 1_000_000_000f
        val r = 0.5f + 0.5f * kotlin.math.sin(t * 1.1f * factor)
        val g = 0.5f + 0.5f * kotlin.math.sin(t * 0.9f * factor + 2.1f)

        drawClearColor(surfaceView.holder.surface, r, g, 0.12f)

        fpsFrames++
        if (fpsWindowStart == 0L) fpsWindowStart = frameTimeNanos
        if (frameTimeNanos - fpsWindowStart >= 1_000_000_000L) {
            // Exercise the JNI kernel path too (same math, native hub).
            val nativePx = if (NativeBridge.isAvailable) NativeBridge.scaled(48f) else 0f
            title = "Vulkan · ${fpsFrames} FPS · scale=${"%.2f".format(factor)}" +
                if (nativePx > 0f) " · native48=${"%.1f".format(nativePx)}px" else " · native off"
            fpsFrames = 0; fpsWindowStart = frameTimeNanos
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    /** Locks the canvas and fills it (swapchain-clear equivalent for the demo). */
    private fun drawClearColor(surface: Surface?, r: Float, g: Float, b: Float): Boolean {
        if (surface == null || !surface.isValid) return false
        return try {
            val canvas = surface.lockHardwareCanvas() ?: return false
            canvas.drawColor(android.graphics.Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt()))
            surface.unlockCanvasAndPost(canvas)
            true
        } catch (_: Exception) { false }
    }
}
