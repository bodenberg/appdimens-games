package io.github.bodenberg.appdimens.games.core;

/** Allocation-free scaling engine. All scalar methods are thread-safe and stateless. */
public final class Calculator {
    private Calculator() {}

    public static float scale(float value, Strategy strategy, Screen screen) {
        return scale(value, strategy, screen, ScaleConfig.DEFAULT);
    }

    public static float scale(float value, Strategy strategy, Screen screen, ScaleConfig config) {
        if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException("value must be finite and >= 0");
        if (strategy == null || screen == null || config == null) throw new NullPointerException();
        final float w = screen.shortestDp();
        final float h = screen.longestDp();
        final float wr = w / config.designWidthDp();
        final float hr = h / config.designHeightDp();
        final float ar = aspectAdjustment(screen.aspectRatio(), config.designHeightDp() / config.designWidthDp());
        final float result = switch (strategy) {
            case NONE -> value;
            case DEFAULT -> value * (1f + (w - config.designWidthDp()) / 300f * .10f) * ar;
            case PERCENTAGE -> value * wr;
            case BALANCED -> value * (w <= config.transitionDp() ? wr :
                    (config.transitionDp() / config.designWidthDp()) *
                    (1f + config.sensitivity() * (float)Math.log(w / config.transitionDp()))) * ar;
            case LOGARITHMIC -> value * (1f + config.sensitivity() * (float)Math.log(w / config.designWidthDp())) * ar;
            case POWER -> value * (float)Math.pow(wr, config.exponent()) * ar;
            case FLUID, AUTOSIZE -> config.maxValue() == Float.MAX_VALUE ? value : fluid(w, config);
            case INTERPOLATED -> value * (1f + (wr - 1f) * .5f) * ar;
            case DIAGONAL -> value * hypot(screen.widthDp(), screen.heightDp()) /
                    hypot(config.designWidthDp(), config.designHeightDp());
            case PERIMETER -> value * (screen.widthDp() + screen.heightDp()) /
                    (config.designWidthDp() + config.designHeightDp());
            case FIT -> value * Math.min(wr, hr);
            case FILL -> value * Math.max(wr, hr);
        };
        return clamp(Math.max(0f, result), config.minValue(), config.maxValue());
    }

    /** Bulk hot path. Supports identical input/output arrays and performs no heap allocation. */
    public static void scale(float[] input, int inputOffset, float[] output, int outputOffset, int count,
                             Strategy strategy, Screen screen, ScaleConfig config) {
        if (input == null || output == null) throw new NullPointerException();
        if (count < 0 || inputOffset < 0 || outputOffset < 0 || inputOffset + count > input.length || outputOffset + count > output.length)
            throw new IndexOutOfBoundsException();
        if (input == output && outputOffset > inputOffset && outputOffset < inputOffset + count) {
            for (int i = count - 1; i >= 0; --i) output[outputOffset + i] = scale(input[inputOffset + i], strategy, screen, config);
        } else {
            for (int i = 0; i < count; ++i) output[outputOffset + i] = scale(input[inputOffset + i], strategy, screen, config);
        }
    }

    private static float fluid(float width, ScaleConfig c) {
        float t = clamp((width - c.minViewportDp()) / (c.maxViewportDp() - c.minViewportDp()), 0f, 1f);
        return c.minValue() + (c.maxValue() - c.minValue()) * t;
    }
    private static float aspectAdjustment(float actual, float design) {
        return clamp(1f + (actual / design - 1f) * .08f, .85f, 1.15f);
    }
    private static float hypot(float x, float y) { return (float)Math.sqrt(x * x + y * y); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
