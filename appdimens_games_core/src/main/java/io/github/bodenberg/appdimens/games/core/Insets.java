package io.github.bodenberg.appdimens.games.core;

/** Safe-area insets in dp (cutouts, system bars, fold features). */
public record Insets(float left, float top, float right, float bottom) {
    public static final Insets NONE = new Insets(0, 0, 0, 0);
    public Insets {
        if (!finiteNonNegative(left) || !finiteNonNegative(top) || !finiteNonNegative(right) || !finiteNonNegative(bottom))
            throw new IllegalArgumentException("Insets must be finite and >= 0");
    }
    private static boolean finiteNonNegative(float value) { return Float.isFinite(value) && value >= 0; }
    public float horizontal() { return left + right; }
    public float vertical() { return top + bottom; }
}
