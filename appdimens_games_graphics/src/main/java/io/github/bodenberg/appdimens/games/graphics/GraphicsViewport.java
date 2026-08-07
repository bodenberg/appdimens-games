package io.github.bodenberg.appdimens.games.graphics;

import io.github.bodenberg.appdimens.games.core.Viewport;

/** Renderer-neutral conversion for Vulkan VkViewport and OpenGL glViewport. */
public record GraphicsViewport(float x, float y, float width, float height, float minDepth, float maxDepth) {
    public static GraphicsViewport openGl(Viewport.Transform value, float density) {
        return new GraphicsViewport(value.offsetX()*density, value.offsetY()*density,
                value.viewportWidth()*density, value.viewportHeight()*density, 0, 1);
    }
    /** Vulkan positive-height convention. Engines using inverted Y may negate height themselves. */
    public static GraphicsViewport vulkan(Viewport.Transform value, float density) { return openGl(value, density); }
    public int pixelX() { return Math.round(x); }
    public int pixelY() { return Math.round(y); }
    public int pixelWidth() { return Math.round(width); }
    public int pixelHeight() { return Math.round(height); }
}
