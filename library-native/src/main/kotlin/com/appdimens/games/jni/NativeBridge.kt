package com.appdimens.games.jni

/**
 * [EN] JNI bridge to the native (C++/NDK) engine. Same kernels, same snapshot
 * semantics as the Kotlin core — usable from GL/Vulkan render threads with
 * zero JVM allocations.
 *
 * [PT] Ponte JNI para o motor nativo (C++/NDK). Mesmos kernels e mesma semântica de
 * snapshot do núcleo Kotlin — utilizável em threads de render GL/Vulkan sem
 * alocações na JVM.
 *
 * Call [updateMetrics] from `onSurfaceChanged` / swapchain recreate; then any thread
 * can call the kernels lock-free.
 */
object NativeBridge {

    @JvmStatic
    external fun updateMetrics(
        widthDp: Float, heightDp: Float, smallestWidthDp: Float,
        densityDpi: Float, fontScale: Float, fullscreen: Boolean
    )

    @JvmStatic external fun scaled(base: Float): Float
    @JvmStatic external fun scaledAr(base: Float): Float

    /** `i` variant — anchored to frozen FULLSCREEN metrics. / Variante `i`. */
    @JvmStatic external fun scaledInvariant(base: Float): Float

    @JvmStatic external fun power(base: Float, ar: Boolean): Float
    @JvmStatic external fun fluid(base: Float, ar: Boolean): Float
    @JvmStatic external fun auto(base: Float, ar: Boolean): Float
    @JvmStatic external fun logarithmic(base: Float, ar: Boolean): Float
    @JvmStatic external fun diagonal(base: Float, ar: Boolean): Float
    @JvmStatic external fun fit(base: Float, ar: Boolean): Float
    @JvmStatic external fun fill(base: Float, ar: Boolean): Float
    @JvmStatic external fun toPx(dp: Float): Float

    val isAvailable: Boolean = try {
        System.loadLibrary("appdimens_games"); true
    } catch (_: UnsatisfiedLinkError) { false }
}
