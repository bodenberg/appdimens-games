package io.github.bodenberg.appdimens.games.android;

import io.github.bodenberg.appdimens.games.core.Screen;
import io.github.bodenberg.appdimens.games.core.Strategy;

/** Explicit JNI facade. Failure to load never leaves partially initialized state. */
public final class NativeEngine {
    public static final int ABI_VERSION = 0x00030106;
    private static final boolean AVAILABLE;
    static {
        boolean loaded;
        try { System.loadLibrary("appdimens_games"); loaded = nativeAbiVersion() == ABI_VERSION; }
        catch (LinkageError | SecurityException error) { loaded = false; }
        AVAILABLE = loaded;
    }
    private NativeEngine() {}
    public static boolean isAvailable() { return AVAILABLE; }
    /** Mutates values in place in one JNI transition. */
    public static void scaleInPlace(float[] values, Strategy strategy, Screen screen) {
        if (!AVAILABLE) throw new IllegalStateException("AppDimens Games native ABI is unavailable or incompatible");
        int status = nativeScaleBatch(values, strategy.id, screen.widthDp(), screen.heightDp(), screen.density());
        if (status != 0) throw new IllegalArgumentException("Native calculation failed with status " + status);
    }
    private static native int nativeAbiVersion();
    private static native int nativeScaleBatch(float[] values, int strategy, float width, float height, float density);
}
