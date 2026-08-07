package io.github.bodenberg.appdimens.games.core;

/** Immutable tuning parameters. Defaults match the 300x533 AppDimens design grid. */
public record ScaleConfig(float designWidthDp, float designHeightDp, float sensitivity,
                          float exponent, float transitionDp, float minValue, float maxValue,
                          float minViewportDp, float maxViewportDp) {
    public static final ScaleConfig DEFAULT = new ScaleConfig(300f, 533f, .40f, .75f, 480f,
            0f, Float.MAX_VALUE, 320f, 768f);
    public ScaleConfig {
        if (!(designWidthDp > 0) || !(designHeightDp > 0)) throw new IllegalArgumentException("Design size must be > 0");
        if (sensitivity < 0 || exponent <= 0 || transitionDp <= 0) throw new IllegalArgumentException("Invalid perceptual tuning");
        if (minValue < 0 || maxValue < minValue || maxViewportDp <= minViewportDp) throw new IllegalArgumentException("Invalid bounds");
    }
    public ScaleConfig withBounds(float min, float max) {
        return new ScaleConfig(designWidthDp, designHeightDp, sensitivity, exponent, transitionDp,
                min, max, minViewportDp, maxViewportDp);
    }
}
