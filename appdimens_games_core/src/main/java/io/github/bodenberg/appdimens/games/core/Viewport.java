package io.github.bodenberg.appdimens.games.core;

/** FIT/FILL viewport and coordinate conversion without renderer dependencies. */
public final class Viewport {
    public enum Mode { FIT, FILL, STRETCH }
    public record Transform(float scaleX, float scaleY, float offsetX, float offsetY,
                            float viewportWidth, float viewportHeight) {}
    private Viewport() {}
    public static Transform calculate(float designWidth, float designHeight, Screen screen, Mode mode) {
        if (!(designWidth > 0) || !(designHeight > 0)) throw new IllegalArgumentException("Design size must be > 0");
        float width = screen.usableWidthDp(), height = screen.usableHeightDp();
        float sx = width / designWidth, sy = height / designHeight;
        if (mode != Mode.STRETCH) sx = sy = mode == Mode.FIT ? Math.min(sx, sy) : Math.max(sx, sy);
        float vw = designWidth * sx, vh = designHeight * sy;
        return new Transform(sx, sy, screen.safeArea().left() + (width - vw) * .5f,
                screen.safeArea().top() + (height - vh) * .5f, vw, vh);
    }
}
