package com.appdimens.games.core;

import com.appdimens.games.common.DpQualifier;

/** Stateless, allocation-free dimension engine designed for frame-loop use. */
public final class GameDimens {
    public static final float DESIGN_WIDTH_DP = 300f;
    public static final float DESIGN_HEIGHT_DP = 533f;
    private GameDimens() {}

    public static float calculate(float value, GameStrategy strategy, DpQualifier qualifier, GameScreen screen) {
        if (!Float.isFinite(value) || value < 0f || strategy == null || qualifier == null || screen == null)
            throw new IllegalArgumentException();
        final float actual = switch (qualifier) {
            case WIDTH -> screen.widthDp(); case HEIGHT -> screen.heightDp(); case SMALL_WIDTH -> screen.smallWidthDp();
        };
        final float design = qualifier == DpQualifier.HEIGHT ? DESIGN_HEIGHT_DP : DESIGN_WIDTH_DP;
        final float ratio = actual / design;
        final float diagonal = hypot(screen.widthDp(), screen.heightDp()) / hypot(DESIGN_WIDTH_DP, DESIGN_HEIGHT_DP);
        final float factor = switch (strategy) {
            case SCALED, PERCENT, RESIZE -> ratio;
            case AUTO -> clamp(ratio, .75f, 1.75f);
            case DENSITY -> screen.density();
            case DIAGONAL -> diagonal;
            case FIT -> Math.min(screen.widthDp() / DESIGN_WIDTH_DP, screen.heightDp() / DESIGN_HEIGHT_DP);
            case FILL -> Math.max(screen.widthDp() / DESIGN_WIDTH_DP, screen.heightDp() / DESIGN_HEIGHT_DP);
            case FLUID -> 1f + (ratio - 1f) * .65f;
            case INTERPOLATED -> 1f + (ratio - 1f) * .5f;
            case LOGARITHMIC -> Math.max(.25f, 1f + .45f * (float)Math.log(ratio));
            case PERIMETER -> (screen.widthDp() + screen.heightDp()) / (DESIGN_WIDTH_DP + DESIGN_HEIGHT_DP);
            case POWER -> (float)Math.pow(ratio, .85f);
        };
        return value * factor;
    }

    public static void calculate(float[] values, int offset, int count, GameStrategy strategy,
                                 DpQualifier qualifier, GameScreen screen) {
        if (values == null || offset < 0 || count < 0 || offset + count > values.length) throw new IllegalArgumentException();
        for (int i = offset, end = offset + count; i < end; i++) values[i] = calculate(values[i], strategy, qualifier, screen);
    }
    private static float hypot(float x, float y) { return (float)Math.sqrt(x*x + y*y); }
    private static float clamp(float x, float min, float max) { return Math.max(min, Math.min(max, x)); }
}
