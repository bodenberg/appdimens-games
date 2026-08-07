package io.github.bodenberg.appdimens.games.core;

/** Immutable viewport expressed in density-independent pixels. */
public record Screen(float widthDp, float heightDp, float density, Insets safeArea) {
    public Screen {
        if (!Float.isFinite(widthDp) || widthDp <= 0 || !Float.isFinite(heightDp) || heightDp <= 0)
            throw new IllegalArgumentException("widthDp and heightDp must be finite and > 0");
        if (!Float.isFinite(density) || density <= 0) throw new IllegalArgumentException("density must be finite and > 0");
        if (safeArea == null) safeArea = Insets.NONE;
    }
    public Screen(float widthDp, float heightDp, float density) { this(widthDp, heightDp, density, Insets.NONE); }
    public float shortestDp() { return Math.min(widthDp, heightDp); }
    public float longestDp() { return Math.max(widthDp, heightDp); }
    public float aspectRatio() { return longestDp() / shortestDp(); }
    public float usableWidthDp() { return Math.max(0f, widthDp - safeArea.horizontal()); }
    public float usableHeightDp() { return Math.max(0f, heightDp - safeArea.vertical()); }
}
