// AppDimens Games 3.0 — JNI bridge.
// Exposes the native snapshot engine to Kotlin/Java game loops (GL/Vulkan threads).
#include <jni.h>
#include "appdimens/games/core.h"
#include "appdimens/games/math.h"

using namespace appdimens::games;

static thread_local Metrics* tlsScratch = nullptr;

static inline Metrics* scratch() {
    if (!tlsScratch) tlsScratch = new Metrics();
    return tlsScratch;
}

extern "C" JNIEXPORT void JNICALL
Java_com_appdimens_games_jni_NativeBridge_updateMetrics(
        JNIEnv*, jclass,
        jfloat widthDp, jfloat heightDp, jfloat smallestWidthDp,
        jfloat densityDpi, jfloat fontScale, jboolean fullscreen) {
    *scratch() = Metrics::make(widthDp, heightDp, smallestWidthDp, densityDpi, fontScale, fullscreen);
    updateMetrics(*scratch());
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_scaled(JNIEnv*, jclass, jfloat base) {
    return math::scaledDp(base, metrics());
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_scaledAr(JNIEnv*, jclass, jfloat base) {
    return math::scaledArDp(base, metrics());
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_scaledInvariant(JNIEnv*, jclass, jfloat base) {
    const Metrics& m = invariantMetrics();
    return m.fullscreen ? math::scaledDp(base, m) : base;
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_power(JNIEnv*, jclass, jfloat base, jboolean ar) {
    return math::powerDp(base, metrics(), ar);
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_fluid(JNIEnv*, jclass, jfloat base, jboolean ar) {
    return math::fluidDp(base, metrics(), 0.0f, 0.0f, ar);
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_auto(JNIEnv*, jclass, jfloat base, jboolean ar) {
    return math::autoDp(base, metrics(), ar);
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_logarithmic(JNIEnv*, jclass, jfloat base, jboolean ar) {
    return math::logarithmicDp(base, metrics(), ar);
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_diagonal(JNIEnv*, jclass, jfloat base, jboolean ar) {
    return math::diagonalDp(base, metrics(), ar);
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_fit(JNIEnv*, jclass, jfloat base, jboolean ar) {
    return math::fitDp(base, metrics(), ar);
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_fill(JNIEnv*, jclass, jfloat base, jboolean ar) {
    return math::fillDp(base, metrics(), ar);
}
extern "C" JNIEXPORT jfloat JNICALL
Java_com_appdimens_games_jni_NativeBridge_toPx(JNIEnv*, jclass, jfloat dp) {
    return math::toPx(dp, metrics());
}
